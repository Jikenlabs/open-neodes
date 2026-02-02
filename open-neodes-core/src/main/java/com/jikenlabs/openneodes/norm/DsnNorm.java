package com.jikenlabs.openneodes.norm;

import java.util.List;
import java.util.Map;

/**
 * Objet racine pour une version de la Norme DSN.
 *
 * @param version              La version de la norme (par ex. 2025.1.3).
 * @param segments             Toutes les définitions de blocs (rubriques)
 *                             disponibles dans cette norme.
 * @param natures              Mappage et hiérarchies par nature de déclaration.
 * @param dsnEnvelope          La structure de haut niveau (S10, S20, S90).
 * @param globalRubriquesUsage Règles d'utilisation globales pour les rubriques
 *                             (pour l'enveloppe).
 */
public record DsnNorm(
        String version,
        Map<String, DsnBlockDefinition> segments,
        Map<String, DsnNatureConfiguration> natures,
        Map<String, DsnBlockMapping> dsnEnvelope,
        Map<String, UsageRule> globalRubriquesUsage) {

    /**
     * Constructeur canonique compact.
     */
    public DsnNorm {
        segments = (segments == null) ? new java.util.TreeMap<>() : new java.util.TreeMap<>(segments);
        natures = (natures == null) ? new java.util.TreeMap<>() : new java.util.TreeMap<>(natures);
        dsnEnvelope = (dsnEnvelope == null) ? new java.util.TreeMap<>() : new java.util.TreeMap<>(dsnEnvelope);
        globalRubriquesUsage = (globalRubriquesUsage == null) ? new java.util.TreeMap<>()
                : new java.util.TreeMap<>(globalRubriquesUsage);
    }

    /**
     * Retourne une liste plate de toutes les clés de segments et leurs rubriques
     * dans cette norme.
     * 
     * @return Une map où la clé est la clé du segment/bloc et la valeur est une
     *         liste des clés de ses rubriques.
     */
    public Map<String, List<String>> getSegmentsWithRubrics() {
        Map<String, List<String>> result = new java.util.HashMap<>();
        segments.forEach((key, block) -> {
            result.put(key, List.copyOf(block.fields().keySet()));
        });
        return result;
    }

    /**
     * Vérifie si un bloc fait partie de la hiérarchie officielle de la norme
     * (Enveloppe ou Natures).
     * 
     * @param blockKey La clé du bloc à vérifier.
     * @return true si le bloc est officiel.
     */
    public boolean isOfficialBlock(String blockKey) {
        // 1. Check in Envelope
        if (dsnEnvelope.containsKey(blockKey)) {
            return true;
        }
        // 2. Check in all Natures
        for (DsnNatureConfiguration nature : natures.values()) {
            if (isBlockInMapping(blockKey, nature.map())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockInMapping(String blockKey, Map<String, DsnBlockMapping> mapping) {
        if (mapping == null)
            return false;
        if (mapping.containsKey(blockKey)) {
            return true;
        }
        for (DsnBlockMapping childMapping : mapping.values()) {
            if (isBlockInMapping(blockKey, childMapping.children())) {
                return true;
            }
        }
        return false;
    }
}
