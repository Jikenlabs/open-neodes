package com.jikenlabs.openneodes.norm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Définition pure d'un Bloc DSN (rubriques).
 * La hiérarchie est maintenant gérée par DsnBlockMapping.
 *
 * @param name   Nom lisible du bloc.
 * @param fields Map des clés de rubriques vers leurs définitions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DsnBlockDefinition(
                String name,
                Map<String, RubriqueDefinition> fields) {
        /**
         * Constructeur avec tri automatique des champs.
         * 
         * @param name   Nom du bloc.
         * @param fields Champs du bloc.
         */
        public DsnBlockDefinition {
                fields = new java.util.TreeMap<>(fields);
        }
}
