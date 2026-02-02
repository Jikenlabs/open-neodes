package com.jikenlabs.openneodes.core;

import com.jikenlabs.openneodes.exception.InvalidDsnFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseur pour les lignes DSN suivant la spécification NEODES.
 * Format : Rubrique,'Valeur'
 */
public class DsnLineParser {

    // RegEx optimisée : ancrée, sans groupes de capture non nécessaires et évitant
    // le backtracking excessif
    private static final Pattern DSN_LINE_PATTERN = Pattern.compile("^\\s*([^\\s,]+)\\s*,\\s*'(.*)'.*$",
            Pattern.DOTALL);

    /**
     * Constructeur par défaut.
     */
    public DsnLineParser() {
    }

    /**
     * Parse une seule ligne DSN.
     *
     * @param line La ligne à parser.
     * @return Un DsnToken contenant la clé et la valeur.
     * @throws InvalidDsnFormatException si le format de la ligne est invalide.
     */
    public DsnToken parse(String line) {
        if (line == null) {
            throw new InvalidDsnFormatException("Line cannot be null");
        }

        Matcher matcher = DSN_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            String snippet = line.length() > 64 ? line.substring(0, 61) + "..." : line;
            throw new InvalidDsnFormatException("Invalid DSN line format: " + snippet);
        }

        String key = matcher.group(1).trim();
        String value = matcher.group(2);

        return new DsnToken(key, value);
    }
}
