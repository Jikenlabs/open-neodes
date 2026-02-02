package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.exception.DsnBusinessException;
import com.jikenlabs.openneodes.model.DsnEnumOption;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Logique pour mapper les valeurs brutes de chaînes DSN vers des types Java
 * enrichis en fonction des définitions.
 */
public class DsnValueMapper {

    /**
     * Constructeur par défaut.
     */
    public DsnValueMapper() {
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    /**
     * Mappe une valeur brute vers son type final et la valide.
     *
     * @param rawValue   La valeur chaîne du fichier DSN.
     * @param definition La définition de la rubrique.
     * @param lineNumber Le numéro de ligne courant pour le rapport d'erreur.
     * @param rubriqueId L'ID de la rubrique pour le rapport d'erreur.
     * @return La valeur typée (String, BigDecimal, LocalDate ou DsnEnumOption).
     * @throws DsnBusinessException Si la conversion ou la validation échoue.
     */
    public Object map(String rawValue, RubriqueDefinition definition, int lineNumber, String rubriqueId) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }

        // Gérer les Enums en premier
        if (definition.options() != null && !definition.options().isEmpty()) {
            String label = definition.options().get(rawValue);
            if (label == null) {
                throw new com.jikenlabs.openneodes.exception.InvalidEnumValueException("Invalid enum code", lineNumber,
                        rubriqueId, rawValue);
            }
            return new DsnEnumOption(rawValue, label);
        }

        return switch (definition.type()) {
            case "N" -> parseNumber(rawValue, lineNumber, rubriqueId);
            case "D" -> parseDate(rawValue, lineNumber, rubriqueId);
            default -> rawValue; // "X" ou inconnu reste String
        };
    }

    private BigDecimal parseNumber(String value, int lineNumber, String rubriqueId) {
        try {
            // Les valeurs numériques DSN utilisent le point comme séparateur décimal
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new DsnBusinessException("Invalid numeric format", lineNumber, rubriqueId, value);
        }
    }

    private LocalDate parseDate(String value, int lineNumber, String rubriqueId) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DsnBusinessException("Invalid date format (expected JJMMAAAA)", lineNumber, rubriqueId, value);
        }
    }
}
