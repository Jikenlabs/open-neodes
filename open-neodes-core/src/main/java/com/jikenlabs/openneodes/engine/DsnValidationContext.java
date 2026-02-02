package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.exception.DsnStructureException;
import com.jikenlabs.openneodes.norm.DsnBlockDefinition;
import com.jikenlabs.openneodes.norm.DsnNatureConfiguration;
import com.jikenlabs.openneodes.norm.DsnNorm;
import com.jikenlabs.openneodes.norm.UsageRule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gère l'état de la validation O/I/C pour une déclaration DSN et son enveloppe.
 */
public class DsnValidationContext {

    private final Set<String> pendingMandatoryRubriques = new HashSet<>();
    private final Map<String, UsageRule> globalRubriquesUsage;
    private final Map<String, UsageRule> globalBlocksUsage;

    private DsnNatureConfiguration nature;

    /**
     * Crée un nouveau contexte de validation.
     *
     * @param norm La norme DSN utilisée.
     */
    public DsnValidationContext(DsnNorm norm) {
        this.globalRubriquesUsage = norm.globalRubriquesUsage();
        this.globalBlocksUsage = java.util.Collections.emptyMap(); // Managed by structural mapping now
    }

    /**
     * Bascule le contexte de validation vers une nouvelle nature de déclaration.
     *
     * @param nature La configuration de la nature de déclaration.
     */
    public void switchNature(DsnNatureConfiguration nature) {
        this.nature = nature;
        // Do not clear pending mandatory rubriques from S10 as they might still be
        // pending
    }

    /**
     * Initialise la validation pour un nouveau bloc.
     *
     * @param blockKey   La clé du bloc.
     * @param definition La définition du bloc.
     */
    public void startBlock(String blockKey, DsnBlockDefinition definition) {
        // 1. Check Global Mandatory rubriques
        for (String rubriqueKey : definition.fields().keySet()) {
            if (globalRubriquesUsage.get(rubriqueKey) == UsageRule.O) {
                pendingMandatoryRubriques.add(rubriqueKey);
            }
        }

        // 2. Check Nature-specific Mandatory rubriques
        if (nature != null) {
            for (String rubriqueKey : definition.fields().keySet()) {
                if (nature.rubriquesUsage().get(rubriqueKey) == UsageRule.O) {
                    pendingMandatoryRubriques.add(rubriqueKey);
                }
            }
        }
    }

    /**
     * Valide une rubrique et la marque comme rencontrée.
     *
     * @param rubriqueKey La clé de la rubrique.
     * @param lineNumber  Le numéro de la ligne courante.
     */
    public void onRubrique(String rubriqueKey, int lineNumber) {
        // 1. Immediate check for Global Forbidden (I)
        if (globalRubriquesUsage.get(rubriqueKey) == UsageRule.I) {
            throw new DsnStructureException("Ligne " + lineNumber + " : Rubrique " + rubriqueKey +
                    " interdite globalement");
        }

        // 2. Immediate check for Nature-specific Forbidden (I)
        if (nature != null && nature.rubriquesUsage().get(rubriqueKey) == UsageRule.I) {
            throw new DsnStructureException("Ligne " + lineNumber + " : Rubrique " + rubriqueKey +
                    " interdite pour la Nature " + nature.natureCode());
        }

        pendingMandatoryRubriques.remove(rubriqueKey);
    }

    /**
     * Effectue une validation différée des rubriques obligatoires lorsqu'un bloc
     * est fermé.
     *
     * @param blockKey La clé du bloc.
     */
    public void finishBlock(String blockKey) {
        if (!pendingMandatoryRubriques.isEmpty()) {
            // Filter pending to only keep those belonging to the block being closed
            for (String missing : new HashSet<>(pendingMandatoryRubriques)) {
                if (missing.startsWith(blockKey)) {
                    // Check if it's still mandatory in current context (Nature or Global)
                    boolean isMandatory = globalRubriquesUsage.get(missing) == UsageRule.O
                            || (nature != null && nature.rubriquesUsage().get(missing) == UsageRule.O);

                    if (isMandatory) {
                        throw new DsnStructureException("Rubrique " + missing +
                                " obligatoire manquante dans le bloc " + blockKey +
                                (nature != null ? " pour la Nature " + nature.natureCode() : " (Global)"));
                    }
                    pendingMandatoryRubriques.remove(missing);
                }
            }
        }
    }
}
