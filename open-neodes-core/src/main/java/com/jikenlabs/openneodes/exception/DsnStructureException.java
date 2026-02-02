package com.jikenlabs.openneodes.exception;

/**
 * Levée lorsqu'un bloc n'est pas autorisé dans le contexte de la nature de
 * déclaration courante.
 */
public class DsnStructureException extends RuntimeException {
    /**
     * Constructeur avec message d'erreur.
     *
     * @param message Le message d'erreur.
     */
    public DsnStructureException(String message) {
        super(message);
    }
}
