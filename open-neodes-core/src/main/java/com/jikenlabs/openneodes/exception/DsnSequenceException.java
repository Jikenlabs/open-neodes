package com.jikenlabs.openneodes.exception;

/**
 * Levée lorsque les blocs DSN sont rencontrés hors de leur séquence technique
 * (S10
 * -> S20 -> S90).
 */
public class DsnSequenceException extends RuntimeException {
    /**
     * Constructeur avec message uniquement.
     *
     * @param message Le message d'erreur.
     */
    public DsnSequenceException(String message) {
        super(message);
    }
}
