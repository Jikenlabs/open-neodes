package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.domain.Affiliation;
import com.jikenlabs.openneodes.domain.AyantDroit;
import com.jikenlabs.openneodes.domain.SalarieAffilie;
import com.jikenlabs.openneodes.exception.DsnBusinessException;
import com.jikenlabs.openneodes.model.DsnBlockInstance;
import com.jikenlabs.openneodes.model.DsnDocument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service pour réconcilier les blocs DSN techniques en objets Affiliation
 * spécifiques au domaine.
 */
public class DsnAffiliationReconciler {

    /**
     * Constructeur par défaut.
     */
    public DsnAffiliationReconciler() {
    }

    /**
     * Réconcilie le document DSN pour extraire les affiliations.
     *
     * @param document Le document DSN.
     * @return La liste des affiliations.
     */
    public List<Affiliation> reconcile(DsnDocument document) {
        List<Affiliation> results = new ArrayList<>();
        Set<String> validAdhesions = collectValidAdhesions(document);
        List<DsnBlockInstance> entreprises = new ArrayList<>();

        for (DsnBlockInstance root : document.getRootBlocks()) {
            collectBlocks(root, "S21.G00.06", entreprises);
        }

        for (DsnBlockInstance entreprise : entreprises) {
            for (DsnBlockInstance etablissement : entreprise.getChildren("S21.G00.11")) {
                for (DsnBlockInstance individu : etablissement.getChildren("S21.G00.30")) {
                    // In Monthly Norm, Affiliation is a child of Contrat
                    for (DsnBlockInstance contrat : individu.getChildren("S21.G00.40")) {
                        results.addAll(processContrat(individu, contrat, validAdhesions));
                    }
                    // Handle case where it might still be directly under Individu if norm allowed
                    // it
                    results.addAll(processIndividuDirectly(individu, validAdhesions));
                }
            }
        }
        return results;
    }

    private void collectBlocks(DsnBlockInstance current, String key, List<DsnBlockInstance> found) {
        if (key.equals(current.getKey())) {
            found.add(current);
        }
        for (List<DsnBlockInstance> childrenList : current.getChildren().values()) {
            for (DsnBlockInstance child : childrenList) {
                collectBlocks(child, key, found);
            }
        }
    }

    private Set<String> collectValidAdhesions(DsnDocument document) {
        Set<String> ids = new HashSet<>();
        List<DsnBlockInstance> adhesions = new ArrayList<>();
        for (DsnBlockInstance root : document.getRootBlocks()) {
            collectBlocks(root, "S21.G00.15", adhesions);
        }

        for (DsnBlockInstance adhesion : adhesions) {
            String id = (String) adhesion.getValue("IdentifiantTechniqueAdhesion");
            if (id != null)
                ids.add(id);
        }
        return ids;
    }

    private List<Affiliation> processContrat(DsnBlockInstance individu, DsnBlockInstance contrat,
            Set<String> validAdhesions) {
        return processAffiliations(individu, contrat, validAdhesions);
    }

    private List<Affiliation> processIndividuDirectly(DsnBlockInstance individu, Set<String> validAdhesions) {
        return processAffiliations(individu, individu, validAdhesions);
    }

    private List<Affiliation> processAffiliations(DsnBlockInstance individu, DsnBlockInstance parent,
            Set<String> validAdhesions) {
        List<Affiliation> affiliations = new ArrayList<>();
        String nom = (String) individu.getValue("NomFamille");
        String nir = (String) individu.getValue("Identifiant");
        SalarieAffilie salarie = new SalarieAffilie(nom, nir);

        for (DsnBlockInstance affBlock : parent.getChildren("S21.G00.70")) {
            String targetAdhesion = (String) affBlock.getValue("IdentifiantTechniqueAdhesion");

            if (targetAdhesion != null && !validAdhesions.contains(targetAdhesion)) {
                throw new DsnBusinessException("Coherence error: Target Adhesion not found in Bloc 15", 0,
                        "IdentifiantTechniqueAdhesion", targetAdhesion);
            }

            List<AyantDroit> ayantsDroit = new ArrayList<>();
            for (DsnBlockInstance adBlock : affBlock.getChildren("S21.G00.73")) {
                ayantsDroit.add(new AyantDroit(
                        (String) adBlock.getValue("NomFamille"),
                        (String) adBlock.getValue("Nir")));
            }

            affiliations.add(new Affiliation(
                    salarie,
                    (String) affBlock.getValue("Option"),
                    (String) affBlock.getValue("Population"),
                    targetAdhesion,
                    ayantsDroit));
        }
        return affiliations;
    }
}
