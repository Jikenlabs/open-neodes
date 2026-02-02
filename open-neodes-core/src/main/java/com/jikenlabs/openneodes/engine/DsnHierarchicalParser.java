package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.core.DsnLineParser;
import com.jikenlabs.openneodes.core.DsnToken;
import com.jikenlabs.openneodes.exception.DsnHierarchyException;
import com.jikenlabs.openneodes.exception.DsnSequenceException;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnDocument;
import com.jikenlabs.openneodes.model.DsnEnvelope;
import com.jikenlabs.openneodes.norm.DsnBlockDefinition;
import com.jikenlabs.openneodes.norm.DsnBlockMapping;
import com.jikenlabs.openneodes.norm.DsnNatureConfiguration;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Parseur avec état pour reconstruire la hiérarchie DSN et valider les
 * transitions basées
 * Transition de Nature de Déclaration et les règles O/I/C.
 * 
 * <p>
 * <strong>NOTE : Cette classe est STATEFUL et n'est pas THREAD-SAFE.</strong>
 * Une instance distincte doit être utilisée pour chaque document DSN traité en
 * parallèle.
 * </p>
 */
public class DsnHierarchicalParser {

    private static final Logger logger = LoggerFactory.getLogger(DsnHierarchicalParser.class);

    private static final int DEFAULT_MAX_DEPTH = 32;
    private static final int DEFAULT_MAX_BLOCKS = 100_000;

    private final DsnLineParser lineParser;
    private final DsnNormRegistry registry;
    private final List<DsnBlockListener> listeners;
    private final boolean detachCompletedBlocks;
    private final DsnValueMapper mapper = new DsnValueMapper();

    private final int maxDepth;
    private final int maxBlocks;
    private int blockCount = 0;

    private DsnNatureConfiguration currentNature = null;
    private final DsnValidationContext validationContext;

    /**
     * Constructeur avec encodage par défaut.
     *
     * @param lineParser Le parseur de ligne.
     * @param registry   Le registre de normes.
     */
    public DsnHierarchicalParser(DsnLineParser lineParser, DsnNormRegistry registry) {
        this(lineParser, registry, Collections.emptyList(), false);
    }

    /**
     * Constructeur avec écouteurs.
     *
     * @param lineParser Le parseur de ligne.
     * @param registry   Le registre de normes.
     * @param listeners  Les écouteurs de blocs.
     */
    public DsnHierarchicalParser(DsnLineParser lineParser, DsnNormRegistry registry, List<DsnBlockListener> listeners) {
        this(lineParser, registry, listeners, false);
    }

    /**
     * Constructeur complet.
     *
     * @param lineParser            Le parseur de ligne.
     * @param registry              Le registre de normes.
     * @param listeners             Les écouteurs de blocs.
     * @param detachCompletedBlocks Si vrai, détache les blocs terminés pour
     *                              économiser la mémoire.
     */
    public DsnHierarchicalParser(DsnLineParser lineParser, DsnNormRegistry registry, List<DsnBlockListener> listeners,
            boolean detachCompletedBlocks) {
        this(lineParser, registry, listeners, detachCompletedBlocks, DEFAULT_MAX_DEPTH, DEFAULT_MAX_BLOCKS);
    }

    /**
     * Constructeur avec limites de sécurité explicites.
     *
     * @param lineParser            Le parseur de ligne.
     * @param registry              Le registre de normes.
     * @param listeners             Les écouteurs de blocs.
     * @param detachCompletedBlocks Si vrai, détache les blocs terminés pour
     *                              économiser la mémoire.
     * @param maxDepth              Profondeur maximale de la hiérarchie pour
     *                              prévenir les stack overflows ou DoS.
     * @param maxBlocks             Nombre maximal de blocs autorisés dans un
     *                              document.
     */
    public DsnHierarchicalParser(DsnLineParser lineParser, DsnNormRegistry registry, List<DsnBlockListener> listeners,
            boolean detachCompletedBlocks, int maxDepth, int maxBlocks) {
        this.lineParser = lineParser;
        this.registry = registry;
        this.listeners = listeners;
        this.detachCompletedBlocks = detachCompletedBlocks;
        this.maxDepth = maxDepth;
        this.maxBlocks = maxBlocks;
        this.validationContext = new DsnValidationContext(registry.getNorm());
    }

    /**
     * Parse un flux de lignes en un document DSN entier.
     *
     * @param lines Les lignes DSN brutes.
     * @return Le document DSN parsé.
     */
    public DsnDocument parse(Iterable<String> lines) {
        DsnDocument document = new DsnDocument();
        // Propage la version de la norme si disponible
        if (registry.getNorm() != null && registry.getNorm().version() != null) {
            document.setNormVersion(registry.getNorm().version());
        }
        processLines(lines, document);

        // On valide la cohérence S90/S10/S20 en créant l'enveloppe à la fin du parse
        createEnvelope(document);

        return document;
    }

    /**
     * Parse un flux de lignes en une enveloppe DSN structurée (S10, S20, S90).
     *
     * @param lines Les lignes DSN brutes.
     * @return L'enveloppe DSN parsée.
     */
    public DsnEnvelope parseEnvelope(Iterable<String> lines) {
        DsnDocument document = new DsnDocument();
        processLines(lines, document);
        return createEnvelope(document);
    }

    private void processLines(Iterable<String> lines, DsnDocument document) {
        Deque<DsnBlockInstance> stack = new ArrayDeque<>();
        Deque<Map<String, DsnBlockMapping>> mappingStack = new ArrayDeque<>();

        int lineNumber = 0;
        this.blockCount = 0;
        this.currentNature = null;
        this.validationContext.switchNature(null);

        // Mapping initial : l'enveloppe DSN
        mappingStack.push(registry.getNorm().dsnEnvelope());

        for (String line : lines) {
            lineNumber++;
            if (line == null || line.isBlank())
                continue;

            DsnToken token = lineParser.parse(line);
            String blockKey = getBlockKey(token.key());

            // Détection du changement de nature (S20.G00.05.001)
            if ("S20.G00.05.001".equals(token.key())) {
                String natureCode = token.value().replaceAll("'", "");
                switchToNature(natureCode, stack, mappingStack);
            }

            try {
                DsnBlockDefinition blockDef = registry.getBlock(blockKey);
                if (blockDef == null) {
                    throw new com.jikenlabs.openneodes.exception.InvalidDsnFormatException(
                            "Unknown block for rubrique: " + token.key());
                }

                reconcileStack(stack, mappingStack, blockDef, token.key(), document);

                validationContext.onRubrique(token.key(), lineNumber);

                RubriqueDefinition rubriqueDef = registry.getDefinition(token.key());

                // Validation de la largeur (length) définie dans la norme
                if (rubriqueDef.length() != null && token.value().length() > rubriqueDef.length()) {
                    throw new com.jikenlabs.openneodes.exception.DsnBusinessException(
                            "Value exceeds maximum length (" + rubriqueDef.length() + ")",
                            lineNumber, token.key(), token.value());
                }

                Object typedValue = mapper.map(token.value(), rubriqueDef, lineNumber, token.key());

                if (!stack.isEmpty()) {
                    stack.peek().addValue(token.key(), typedValue);
                }
            } catch (DsnHierarchyException | com.jikenlabs.openneodes.exception.InvalidDsnFormatException
                    | com.jikenlabs.openneodes.exception.DsnBusinessException e) {
                // Ces erreurs sont fatales
                throw e;
            } catch (Exception e) {
                logger.warn("Ligne {} ignorée : {}", lineNumber, e.getMessage());
                // On continue la lecture malgré l'erreur pour les autres cas
            }
        }

        document.setTotalRubrics(lineNumber);

        while (!stack.isEmpty()) {
            closeBlock(stack.pop());
        }
    }

    private void switchToNature(String natureCode, Deque<DsnBlockInstance> stack,
            Deque<Map<String, DsnBlockMapping>> mappingStack) {
        this.currentNature = registry.getNorm().natures().get(natureCode);
        this.validationContext.switchNature(this.currentNature);

        // Lors du changement de nature, nous devons mettre à jour le mapping pour S20
        // et ses enfants
        // Transition de Enveloppe(S20) vers Nature(structure S20)
        // Cela sera géré dans reconcileStack lors de l'atteinte du mapping S20
    }

    private void reconcileStack(Deque<DsnBlockInstance> stack, Deque<Map<String, DsnBlockMapping>> mappingStack,
            DsnBlockDefinition newBlockDef, String rubriqueKey, DsnDocument document) {

        String targetBlockKey = getBlockKey(rubriqueKey);

        if (stack.isEmpty()) {
            Map<String, DsnBlockMapping> currentLevel = mappingStack.peek();

            // Root level: Only allow official S10/S20/S90 OR non-official technical
            // extensions
            if (currentLevel != null && (currentLevel.containsKey(targetBlockKey)
                    || !registry.getNorm().isOfficialBlock(targetBlockKey))) {
                createNewBlockInstance(stack, mappingStack, targetBlockKey, newBlockDef, document);
                return;
            }
            throw new DsnHierarchyException(
                    "Block " + targetBlockKey + " not allowed at root level");
        }

        DsnBlockInstance current = stack.peek();

        // 1. Same block? Check for RESTART first
        if (current.getKey().equals(targetBlockKey)) {
            if (isFirstRubriqueOfBlock(rubriqueKey, newBlockDef)) {
                closeBlock(stack.pop());
                mappingStack.pop();
                reconcileStack(stack, mappingStack, newBlockDef, rubriqueKey, document);
            }
            return;
        }

        // 2. Does the rubrique belong to the CURRENT block?
        if (current.getDefinition().fields().containsKey(rubriqueKey)) {
            return;
        }

        // 3. Is target a child of current?
        Map<String, DsnBlockMapping> currentChildren = mappingStack.peek();
        if (currentChildren != null && currentChildren.containsKey(targetBlockKey)) {
            createNewBlockInstance(stack, mappingStack, targetBlockKey, newBlockDef, document);
            return;
        }

        // 4. Is target an OFFICIAL block allowed somewhere else higher in the
        // hierarchy?
        if (isDefinedSomewhereInStack(mappingStack, targetBlockKey)) {
            closeBlock(stack.pop());
            mappingStack.pop();
            reconcileStack(stack, mappingStack, newBlockDef, rubriqueKey, document);
            return;
        }

        // 5. Is target a NON-OFFICIAL block (Technical Extension like S10.G00.95)?
        // We allow these to attach to the current block flexibly if they are NOT
        // official.
        if (!registry.getNorm().isOfficialBlock(targetBlockKey)) {
            createNewBlockInstance(stack, mappingStack, targetBlockKey, newBlockDef, document);
            return;
        }

        // 6. Official block but not allowed here and not child of any parent -> Pop and
        // let root handle it or throw
        closeBlock(stack.pop());
        mappingStack.pop();
        reconcileStack(stack, mappingStack, newBlockDef, rubriqueKey, document);
    }

    private boolean isDefinedSomewhereInStack(Deque<Map<String, DsnBlockMapping>> stack, String blockKey) {
        for (Map<String, DsnBlockMapping> mapping : stack) {
            if (mapping != null && mapping.containsKey(blockKey)) {
                return true;
            }
        }
        return false;
    }

    private void createNewBlockInstance(Deque<DsnBlockInstance> stack, Deque<Map<String, DsnBlockMapping>> mappingStack,
            String blockKey, DsnBlockDefinition def, DsnDocument document) {

        if (stack.size() >= maxDepth) {
            throw new DsnHierarchyException("Maximum depth reached (" + maxDepth + ") for block " + blockKey);
        }

        if (++blockCount > maxBlocks) {
            throw new DsnHierarchyException("Maximum number of blocks reached (" + maxBlocks + ")");
        }

        DsnBlockInstance newInstance = new DsnBlockInstance(blockKey, def);
        validationContext.startBlock(blockKey, def);

        Map<String, DsnBlockMapping> currentMappingLevel = mappingStack.peek();
        DsnBlockMapping mapping = currentMappingLevel != null ? currentMappingLevel.get(blockKey) : null;

        if (stack.isEmpty()) {
            document.addRootBlock(newInstance);
        } else {
            stack.peek().addChild(newInstance);
        }

        stack.push(newInstance);

        if (mapping != null) {
            // Crucial : En entrant dans S20, si la nature est connue, on bascule vers le
            // mapping de la nature
            if ("S20.G00.05".equals(blockKey) && currentNature != null) {
                mappingStack.push(currentNature.map());
            } else {
                mappingStack.push(mapping.children());
            }
        } else {
            // Bloc technique ou extension sans hiérarchie définie (ex: S10.G00.95)
            mappingStack.push(java.util.Map.of());
        }
    }

    private boolean isFirstRubriqueOfBlock(String rubriqueKey, DsnBlockDefinition def) {
        return def.fields().keySet().iterator().next().equals(rubriqueKey);
    }

    private void closeBlock(DsnBlockInstance block) {
        validationContext.finishBlock(block.getKey());

        for (DsnBlockListener listener : listeners) {
            listener.onBlockCompleted(block);
        }
        if (detachCompletedBlocks) {
            block.detachChildren();
        }
    }

    private DsnEnvelope createEnvelope(DsnDocument document) {
        DsnBlockInstance header = null;
        List<DsnBlockInstance> declarations = new ArrayList<>();
        DsnBlockInstance footer = null;

        for (DsnBlockInstance root : document.getRootBlocks()) {
            if (root.getKey().startsWith("S10"))
                header = root;
            else if (root.getKey().startsWith("S20"))
                declarations.add(root);
            else if (root.getKey().startsWith("S90"))
                footer = root;
        }

        if (header == null) {
            throw new DsnSequenceException("Missing header block (S10)");
        }
        if (footer == null) {
            throw new DsnSequenceException("Missing footer block (S90)");
        }

        Number declaredCount = (Number) footer.getValue("S90.G00.90.002");
        if (declaredCount == null) {
            declaredCount = (Number) footer.getValue("NombreDSN");
        }

        if (declaredCount != null && declaredCount.longValue() != declarations.size()) {
            throw new DsnSequenceException("Coherence error: S90 declared " + declaredCount.longValue() +
                    " DSN blocks but actual count is " + declarations.size());
        }

        Number declaredRubrics = (Number) footer.getValue("S90.G00.90.001");
        if (declaredRubrics == null) {
            declaredRubrics = (Number) footer.getValue("NombreRubriques");
        }

        if (declaredRubrics != null && declaredRubrics.longValue() != document.getTotalRubrics()) {
            throw new DsnSequenceException("Coherence error: S90 declared " + declaredRubrics.longValue() +
                    " rubrics but actual count is " + document.getTotalRubrics());
        }

        return new DsnEnvelope(header, declarations, footer);
    }

    private String getBlockKey(String rubriqueKey) {
        int lastDot = rubriqueKey.lastIndexOf('.');
        return lastDot == -1 ? rubriqueKey : rubriqueKey.substring(0, lastDot);
    }
}
