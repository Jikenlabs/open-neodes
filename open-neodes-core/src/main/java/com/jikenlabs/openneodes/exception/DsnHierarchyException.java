package com.jikenlabs.openneodes.exception;

/**
 * Exception levée lorsque la hiérarchie DSN n'est pas respectée.
 */
public class DsnHierarchyException extends RuntimeException {
    /**
     * Constructeur avec message.
     *
     * @param message Le message d'erreur.
     */
    public DsnHierarchyException(String message) {
        super(message);
    }
}
