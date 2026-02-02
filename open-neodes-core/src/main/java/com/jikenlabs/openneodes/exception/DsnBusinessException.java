package com.jikenlabs.openneodes.exception;

/**
 * Exception levée lorsqu'une valeur DSN viole les règles de gestion (par ex.
 * enum invalide,
 * format invalide).
 */
public class DsnBusinessException extends RuntimeException {
    /** Le numéro de ligne où l'erreur est survenue. */
    private final int lineNumber;
    /** L'identifiant de la rubrique (par ex. S21.G00.30.001). */
    private final String rubriqueId;
    /** La valeur invalide. */
    private final String faultyValue;

    /**
     * Constructeur avec tous les détails.
     *
     * @param message     Le message d'erreur.
     * @param lineNumber  Le numéro de ligne.
     * @param rubriqueId  L'identifiant de la rubrique.
     * @param faultyValue La valeur fautive.
     */
    public DsnBusinessException(String message, int lineNumber, String rubriqueId, String faultyValue) {
        super(formatMessage(message, lineNumber, rubriqueId, faultyValue));
        this.lineNumber = lineNumber;
        this.rubriqueId = rubriqueId;
        this.faultyValue = faultyValue;
    }

    private static String formatMessage(String message, int lineNumber, String rubriqueId, String faultyValue) {
        String displayValue = com.jikenlabs.openneodes.engine.DsnSecurityRules.isSensitive(rubriqueId)
                ? com.jikenlabs.openneodes.engine.DsnSecurityRules.mask(faultyValue)
                : faultyValue;
        return String.format("%s [Line %d, Rubrique %s, Value '%s']", message, lineNumber, rubriqueId, displayValue);
    }

    /**
     * Récupère le numéro de ligne.
     *
     * @return Le numéro de ligne.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Récupère l'identifiant de la rubrique.
     *
     * @return L'identifiant de la rubrique.
     */
    public String getRubriqueId() {
        return rubriqueId;
    }

    /**
     * Récupère la valeur fautive.
     *
     * @return La valeur invalide.
     */
    public String getFaultyValue() {
        return faultyValue;
    }
}
