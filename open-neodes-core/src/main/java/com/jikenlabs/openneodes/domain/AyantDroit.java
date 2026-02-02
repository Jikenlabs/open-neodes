package com.jikenlabs.openneodes.domain;

/**
 * Représente un bénéficiaire (Ayant-droit) lié à un affilié principal.
 */
public final class AyantDroit implements Affilie {
    private final String nom;
    private final String nir;

    /**
     * Crée un nouvel ayant-droit.
     *
     * @param nom Le nom de l'ayant-droit.
     * @param nir Le NIR de l'ayant-droit.
     */
    public AyantDroit(String nom, String nir) {
        this.nom = nom;
        this.nir = nir;
    }

    @Override
    public String getNom() {
        return nom;
    }

    @Override
    public String getNir() {
        return nir;
    }
}
