package com.jikenlabs.openneodes.exception;

/**
 * Levée lorsqu'une valeur d'énumération ne correspond à aucune option autorisée
 * dans la norme DSN.
 */
public class InvalidEnumValueException extends DsnBusinessException {
    /**
     * Constructeur avec détails complets.
     *
     * @param message     Le message d'erreur.
     * @param lineNumber  Le numéro de ligne.
     * @param rubriqueId  L'identifiant de la rubrique.
     * @param faultyValue La valeur fautive.
     */
    public InvalidEnumValueException(String message, int lineNumber, String rubriqueId, String faultyValue) {
        super(message, lineNumber, rubriqueId, faultyValue);
    }
}
