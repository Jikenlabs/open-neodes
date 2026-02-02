package com.jikenlabs.openneodes.engine;

import java.util.Set;

/**
 * Centralise les règles de sécurité pour le traitement des données DSN.
 * Notamment l'identification des données personnelles (PII).
 */
public class DsnSecurityRules {

    private DsnSecurityRules() {
    }

    /**
     * Liste des rubriques contenant des données hautement sensibles (PII).
     * Ces rubriques doivent être masquées dans les logs et les messages d'erreur.
     */
    private static final Set<String> SENSITIVE_RUBRIQUES = Set.of(
            // Individu - NIR (Numéro de Sécurité Sociale)
            "S21.G00.30.001",
            // Individu - Nom de famille
            "S21.G00.30.002",
            // Individu - Prénoms
            "S21.G00.30.004",
            // Individu - Date de naissance
            "S21.G00.30.006",
            // Individu - Adresse (Voie, Code Postal, Localité)
            "S21.G00.30.008",
            "S21.G00.30.012",
            "S21.G00.30.013",
            // Rémunération - Montant
            "S21.G00.51.011",
            "S21.G00.51.013",
            // IBAN (Coordonnées bancaires)
            "S20.G00.07.007",
            "S21.G00.20.005");

    /**
     * Vérifie si une rubrique est considérée comme sensible.
     * 
     * @param rubriqueId L'identifiant de la rubrique.
     * @return true si sensible.
     */
    public static boolean isSensitive(String rubriqueId) {
        if (rubriqueId == null)
            return false;

        // Match exact
        if (SENSITIVE_RUBRIQUES.contains(rubriqueId))
            return true;

        // Match par préfixe pour certaines catégories (ex: tout ce qui touche au
        // paiement S20.G00.07)
        return rubriqueId.startsWith("S20.G00.07.") || rubriqueId.startsWith("S21.G00.30.");
    }

    /**
     * Masque une valeur sensible.
     * 
     * @param value La valeur brute.
     * @return La valeur masquée.
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty())
            return value;
        if (value.length() <= 4)
            return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
