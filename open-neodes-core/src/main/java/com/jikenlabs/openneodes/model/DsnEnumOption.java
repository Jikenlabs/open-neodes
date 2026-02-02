package com.jikenlabs.openneodes.model;

/**
 * Représente une option d'énumération résolue de la norme DSN.
 * Contient à la fois le code technique (par ex. "01") et son libellé lisible.
 *
 * @param code  Le code technique.
 * @param label Le libellé humainement lisible.
 */
public record DsnEnumOption(String code, String label) {
    @Override
    public String toString() {
        return code + " - " + label;
    }
}
