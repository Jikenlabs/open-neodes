package com.jikenlabs.openneodes.core;

/**
 * Représentation atomique d'une ligne DSN.
 *
 * @param key   L'identifiant de la rubrique (par ex. S21.G00.30.001)
 * @param value La valeur associée à la rubrique
 */
public record DsnToken(String key, String value) {
    /**
     * Constructeur canonique compact.
     */
    public DsnToken {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }
}
