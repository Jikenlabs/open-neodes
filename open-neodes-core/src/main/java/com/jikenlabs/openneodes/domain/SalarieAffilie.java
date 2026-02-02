package com.jikenlabs.openneodes.domain;

/**
 * Représente l'employé principal (Salarié) en tant qu'affilié.
 */
public final class SalarieAffilie implements Affilie {
    private final String nom;
    private final String nir;

    /**
     * Crée un nouveau salarié affilié.
     *
     * @param nom Le nom du salarié.
     * @param nir Le NIR du salarié.
     */
    public SalarieAffilie(String nom, String nir) {
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
