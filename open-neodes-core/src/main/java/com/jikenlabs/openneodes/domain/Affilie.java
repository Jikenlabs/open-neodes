package com.jikenlabs.openneodes.domain;

/**
 * Interface scellée pour tous les types d'affiliés dans une DSN.
 */
public sealed interface Affilie permits SalarieAffilie, AyantDroit {
    /**
     * Récupère le nom.
     *
     * @return Le nom de l'affilié.
     */
    String getNom();

    /**
     * Récupère le NIR.
     *
     * @return Le NIR de l'affilié.
     */
    String getNir();
}
