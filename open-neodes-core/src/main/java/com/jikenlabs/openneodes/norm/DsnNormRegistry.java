package com.jikenlabs.openneodes.norm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jikenlabs.openneodes.exception.DefinitionNotFoundException;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registre pour les définitions de rubriques DSN, chargées depuis un YAML
 * imbriqué.
 */
public class DsnNormRegistry {

    private static final Map<String, DsnNormRegistry> CACHE = new ConcurrentHashMap<>();

    private final DsnNorm norm;
    private final Map<String, DsnBlockDefinition> blockLookup = new TreeMap<>();

    private DsnNormRegistry(DsnNorm norm) {
        this.norm = norm;
        this.blockLookup.putAll(norm.segments());
        // Injecter ou Fusionner systématiquement les blocs techniques (S10.G00.95,
        // etc.)
        DsnTechnicalDefinitions.getTechnicalBlocks().forEach((key, techDef) -> {
            if (this.blockLookup.containsKey(key)) {
                // Fusionner les rubriques techniques dans le bloc existant
                DsnBlockDefinition existing = this.blockLookup.get(key);
                Map<String, RubriqueDefinition> combinedFields = new TreeMap<>(existing.fields());
                combinedFields.putAll(techDef.fields());
                this.blockLookup.put(key, new DsnBlockDefinition(existing.name(), combinedFields));
            } else {
                // Nouveau bloc technique complet
                this.blockLookup.put(key, techDef);
            }
        });
    }

    /**
     * Charge un registre de norme DSN depuis une ressource YAML dans le classpath.
     * Utilise un cache thread-safe pour éviter les rechargements inutiles.
     *
     * @param resourcePath Chemin vers la ressource YAML.
     * @return Un future qui se complétera avec le DsnNormRegistry initialisé.
     */
    public static CompletableFuture<DsnNormRegistry> loadFromYamlAsync(String resourcePath) {
        if (CACHE.containsKey(resourcePath)) {
            return CompletableFuture.completedFuture(CACHE.get(resourcePath));
        }

        CompletableFuture<DsnNormRegistry> future = new CompletableFuture<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try (InputStream is = DsnNormRegistry.class.getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        future.completeExceptionally(
                                new IllegalArgumentException("Resource not found: " + resourcePath));
                        return;
                    }

                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    DsnNorm norm = mapper.readValue(is, DsnNorm.class);
                    DsnNormRegistry registry = new DsnNormRegistry(norm);
                    CACHE.put(resourcePath, registry);
                    future.complete(registry);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }

        return future;
    }

    /**
     * Récupère les métadonnées de la norme.
     *
     * @return Les métadonnées de la norme.
     */
    public DsnNorm getNorm() {
        return norm;
    }

    /**
     * Obtient la définition pour une clé de rubrique donnée.
     *
     * @param key La clé de la rubrique (par ex. S21.G00.30.001).
     * @return La définition.
     * @throws DefinitionNotFoundException si non trouvé.
     */
    public RubriqueDefinition getDefinition(String key) {
        for (DsnBlockDefinition block : blockLookup.values()) {
            if (block.fields().containsKey(key)) {
                return block.fields().get(key);
            }
        }
        throw new DefinitionNotFoundException(key);
    }

    /**
     * Obtient une définition de bloc par sa clé.
     *
     * @param blockKey La clé du bloc (par ex. S21.G00.30).
     * @return La définition du bloc ou null si non trouvée.
     */
    public DsnBlockDefinition getBlock(String blockKey) {
        return blockLookup.get(blockKey);
    }
}
