package com.jikenlabs.openneodes.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.DsnBlockDefinition;
import com.jikenlabs.openneodes.norm.DsnNormRegistry;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportateur pour convertir DsnBlockInstance en JSON en utilisant les libellés
 * de la norme.
 */
public class DsnJsonExporter {

    private final ObjectMapper objectMapper;
    private final DsnNormRegistry registry;

    /**
     * Constructeur avec registre.
     *
     * @param registry Le registre de norme.
     */
    public DsnJsonExporter(DsnNormRegistry registry) {
        this.registry = registry;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Convertit une instance de bloc DsnBlockInstance en chaîne JSON.
     *
     * @param block Le bloc à exporter.
     * @return La chaîne JSON.
     * @throws IOException Si la sérialisation échoue.
     */
    public String toJson(DsnBlockInstance block) throws IOException {
        Map<String, Object> map = toMap(block);
        return objectMapper.writeValueAsString(map);
    }

    /**
     * Convertit une instance de bloc DsnBlockInstance en Map en utilisant les
     * libellés de la norme.
     *
     * @param instance L'instance de bloc.
     * @return Une Map associant les libellés aux valeurs.
     */
    public Map<String, Object> toMap(DsnBlockInstance instance) {
        DsnBlockDefinition blockDef = registry.getBlock(instance.getKey());
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Exporter les valeurs (Rubriques)
        for (Map.Entry<String, Object> entry : instance.getValues().entrySet()) {
            RubriqueDefinition rubriqueDef = registry.getDefinition(entry.getKey());
            String label = rubriqueDef != null ? rubriqueDef.name() : entry.getKey();

            Object value = entry.getValue();
            if (value instanceof DsnEnumOption enumOption) {
                // Pour les Enums, exporter le libellé comme valeur principale
                // AC : "Les libellés du YAML doivent être utilisés"
                // Nous pourrions aussi exporter comme un objet imbriqué si nécessaire
                result.put(label, enumOption.label());
                // Nous pourrions aussi exporter comme un objet imbriqué si nécessaire
                // result.put(label, Map.of("code", enumOption.code(), "label",
                // enumOption.label()));
            } else {
                result.put(label, value);
            }
        }

        // 2. Exporter les enfants (Blocs)
        for (Map.Entry<String, List<DsnBlockInstance>> entry : instance.getChildren().entrySet()) {
            DsnBlockDefinition childDef = registry.getBlock(entry.getKey());
            String blockLabel = childDef != null ? childDef.name() : entry.getKey();

            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (DsnBlockInstance childInstance : entry.getValue()) {
                childMaps.add(toMap(childInstance));
            }

            // AC 3 : Si répétable OU toujours un tableau pour la cohérence
            // Pour l'instant, s'il n'y a qu'un enfant, on vérifie si c'est un bloc non
            // répétitif connu
            // comme S10 ou S90
            boolean isKnownMultiple = !entry.getKey().startsWith("S10") && !entry.getKey().equals("S90.G00.90");
            if (isKnownMultiple || childMaps.size() > 1) {
                result.put(blockLabel, childMaps);
            } else {
                result.put(blockLabel, childMaps.get(0));
            }
        }

        return result;
    }
}
