package com.jikenlabs.openneodes.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un enregistrement d'affiliation liant un affilié à une adhésion
 * d'entreprise.
 *
 * @param principal       L'affilié principal (salarié).
 * @param option          L'option d'affiliation.
 * @param population      La population concernée.
 * @param idAdhesionCible L'identifiant technique de l'adhésion cible.
 * @param ayantsDroit     La liste des ayants-droit.
 */
public record Affiliation(
        Affilie principal,
        String option,
        String population,
        String idAdhesionCible,
        List<AyantDroit> ayantsDroit) {
    /**
     * Constructeur compact avec initialisation de liste.
     */
    public Affiliation {
        if (ayantsDroit == null) {
            ayantsDroit = new ArrayList<>();
        }
    }
}
