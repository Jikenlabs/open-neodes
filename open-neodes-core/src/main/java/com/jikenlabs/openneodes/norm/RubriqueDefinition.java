package com.jikenlabs.openneodes.norm;

import java.util.Map;

/**
 * Définition des métadonnées d'une rubrique DSN.
 *
 * @param name    Nom lisible de la rubrique.
 * @param type    Type de donnée (X pour alphanumérique, N pour numérique, D
 *                pour date).
 * @param length  Longueur maximale attendue.
 * @param options Mapping optionnel pour les énumérations (ex: "01" ->
 *                "masculin").
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record RubriqueDefinition(
                String name,
                String type,
                Integer length,
                Map<String, String> options) {
}
