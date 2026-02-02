package com.jikenlabs.openneodes.norm;

import java.util.HashMap;
import java.util.Map;

/**
 * Définitions pour les blocs techniques non-standard (ex: Concentrateur
 * S10.G00.95).
 * Ces définitions sont organisées par éditeur/fournisseur.
 */
public class DsnTechnicalDefinitions {

    private DsnTechnicalDefinitions() {
        // Utility class
    }

    /** Bloc Concentrateur standard. */
    public static final String CONCENTRATEUR_BLOCK = "S10.G00.95";
    /** Bloc de métadonnées techniques. */
    public static final String TECH_METADATA_BLOCK = "S20.G00.96";

    /**
     * Retourne les blocs communs.
     * 
     * @return Blocs techniques communs à plusieurs éditeurs.
     */
    public static Map<String, DsnBlockDefinition> getCommonBlocks() {
        Map<String, DsnBlockDefinition> blocks = new HashMap<>();

        Map<String, RubriqueDefinition> concentrateurFields = new HashMap<>();
        concentrateurFields.put("S10.G00.95.001", new RubriqueDefinition("nomUtilisateur", "X", 80, null));
        concentrateurFields.put("S10.G00.95.002", new RubriqueDefinition("prenomUtilisateur", "X", 80, null));
        concentrateurFields.put("S10.G00.95.003", new RubriqueDefinition("siret", "X", 14, null));
        concentrateurFields.put("S10.G00.95.006", new RubriqueDefinition("modeTransmission", "X", 10, null));
        concentrateurFields.put("S10.G00.95.007", new RubriqueDefinition("siretEmetteur", "X", 14, null));
        concentrateurFields.put("S10.G00.95.008", new RubriqueDefinition("dateHeureEnvoi", "X", 14, null));
        concentrateurFields.put("S10.G00.95.009", new RubriqueDefinition("raisonSociale", "X", 80, null));
        concentrateurFields.put("S10.G00.95.900", new RubriqueDefinition("tokenAuthentification", "X", 100, null));
        concentrateurFields.put("S10.G00.95.901", new RubriqueDefinition("emailContact", "X", 100, null));
        blocks.put(CONCENTRATEUR_BLOCK, new DsnBlockDefinition("Concentrateur", concentrateurFields));

        return blocks;
    }

    /**
     * Retourne les blocs SAGE.
     * 
     * @return Blocs techniques spécifiques à l'éditeur SAGE.
     */
    public static Map<String, DsnBlockDefinition> getSageBlocks() {
        Map<String, DsnBlockDefinition> blocks = new HashMap<>();

        // Entreprise (S21.G00.06) - Extension Sage
        Map<String, RubriqueDefinition> entFields = new HashMap<>();
        entFields.put("S21.G00.06.903", new RubriqueDefinition("NomExtension", "X", 80, null));
        blocks.put("S21.G00.06", new DsnBlockDefinition("EntrepriseSage", entFields));

        // Etablissement (S21.G00.11) - Extension Sage
        Map<String, RubriqueDefinition> etbFields = new HashMap<>();
        etbFields.put("S21.G00.11.110", new RubriqueDefinition("EtbTag1", "X", 50, null));
        etbFields.put("S21.G00.11.111", new RubriqueDefinition("EtbTag2", "X", 50, null));
        etbFields.put("S21.G00.11.112", new RubriqueDefinition("EtbTag3", "X", 50, null));
        etbFields.put("S21.G00.11.904", new RubriqueDefinition("EnseigneExtension", "X", 80, null));
        blocks.put("S21.G00.11", new DsnBlockDefinition("EtablissementSage", etbFields));

        return blocks;
    }

    /**
     * Retourne les blocs FIDUCIAL.
     * 
     * @return Blocs techniques spécifiques à l'éditeur FIDUCIAL.
     */
    public static Map<String, DsnBlockDefinition> getFiducialBlocks() {
        Map<String, DsnBlockDefinition> blocks = new HashMap<>();

        // Concentrateur (S10.G00.95) - Extension Fiducial
        Map<String, RubriqueDefinition> concFields = new HashMap<>();
        concFields.put("S10.G00.95.010", new RubriqueDefinition("idConcentrateur", "X", 50, null));
        blocks.put(CONCENTRATEUR_BLOCK, new DsnBlockDefinition("ConcentrateurFiducial", concFields));

        // Metadonnées Techniques (S20.G00.96) - Extension Fiducial
        Map<String, RubriqueDefinition> techFields = new HashMap<>();
        techFields.put("S20.G00.96.010", new RubriqueDefinition("versionMetadonnees", "X", 10, null));
        techFields.put("S20.G00.96.902", new RubriqueDefinition("idTechniqueDeclaration", "X", 50, null));
        blocks.put(TECH_METADATA_BLOCK, new DsnBlockDefinition("MetadonneesFiducial", techFields));

        // Lieu de Travail (S21.G00.85) - Extension Fiducial
        Map<String, RubriqueDefinition> lieuFields = new HashMap<>();
        lieuFields.put("S21.G00.85.850", new RubriqueDefinition("LibelleLieuExtension", "X", 80, null));
        blocks.put("S21.G00.85", new DsnBlockDefinition("TravailLieuFiducial", lieuFields));

        return blocks;
    }

    /**
     * @return Toutes les définitions techniques fusionnées.
     * @deprecated Utiliser les méthodes spécifiques par fournisseur si possible.
     */
    @Deprecated
    public static Map<String, DsnBlockDefinition> getTechnicalBlocks() {
        Map<String, DsnBlockDefinition> all = new HashMap<>();
        // Note: L'ordre de fusion peut avoir un impact si des clés se chevauchent entre
        // éditeurs.
        all.putAll(getCommonBlocks());
        // Merge SAGE
        getSageBlocks().forEach((k, v) -> merge(all, k, v));
        // Merge FIDUCIAL
        getFiducialBlocks().forEach((k, v) -> merge(all, k, v));
        return all;
    }

    private static void merge(Map<String, DsnBlockDefinition> target, String key, DsnBlockDefinition def) {
        DsnBlockDefinition existing = target.get(key);
        if (existing == null) {
            target.put(key, def);
        } else {
            Map<String, RubriqueDefinition> mergedFields = new HashMap<>(existing.fields());
            mergedFields.putAll(def.fields());
            target.put(key, new DsnBlockDefinition(existing.name(), mergedFields));
        }
    }
}
