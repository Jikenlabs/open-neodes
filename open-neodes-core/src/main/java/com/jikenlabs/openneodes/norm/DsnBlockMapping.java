package com.jikenlabs.openneodes.norm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Mappage d'un bloc DSN dans une hiérarchie, avec cardinalités.
 *
 * @param blockKey  La clé du bloc (par ex. S21.G00.30).
 * @param minOccurs Nombre minimum d'occurrences (0 pour optionnel).
 * @param maxOccurs Nombre maximum d'occurrences (-1 pour illimité).
 * @param children  Mappage récursif des blocs enfants.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DsnBlockMapping(
        String blockKey,
        int minOccurs,
        int maxOccurs,
        Map<String, DsnBlockMapping> children) {

    /**
     * Constructeur compact.
     */
    public DsnBlockMapping {
        if (children == null) {
            children = Map.of();
        }
    }
}
