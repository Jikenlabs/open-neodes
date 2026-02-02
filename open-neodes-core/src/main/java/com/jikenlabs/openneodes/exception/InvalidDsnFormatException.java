package com.jikenlabs.openneodes.exception;

/**
 * Exception levée lorsqu'une ligne DSN ne respecte pas le format attendu.
 */
public class InvalidDsnFormatException extends RuntimeException {
    /**
     * Constructeur avec message d'erreur.
     *
     * @param message Le message d'erreur.
     */
    public InvalidDsnFormatException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause.
     *
     * @param message Le message d'erreur.
     * @param cause   La cause de l'erreur.
     */
    public InvalidDsnFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
