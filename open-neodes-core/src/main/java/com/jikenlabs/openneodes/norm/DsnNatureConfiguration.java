package com.jikenlabs.openneodes.norm;

import java.util.Map;

/**
 * Configuration pour une nature de déclaration spécifique (par ex. Mensuelle).
 *
 * @param natureCode     Le code (par ex. 01).
 * @param label          Nom lisible.
 * @param map            Mappage hiérarchique des blocs avec cardinalités.
 * @param rubriquesUsage Règles d'utilisation (O/I/C) pour les rubriques dans
 *                       cette nature.
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record DsnNatureConfiguration(
        String natureCode,
        String label,
        Map<String, DsnBlockMapping> map,
        Map<String, UsageRule> rubriquesUsage) {

    /**
     * Constructeur canonique compact.
     */
    public DsnNatureConfiguration {
        if (map == null)
            map = Map.of();
        if (rubriquesUsage == null)
            rubriquesUsage = Map.of();
    }
}
