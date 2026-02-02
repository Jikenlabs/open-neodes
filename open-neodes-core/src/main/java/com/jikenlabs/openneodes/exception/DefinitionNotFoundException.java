package com.jikenlabs.openneodes.exception;

/**
 * Exception levée lorsqu'une définition de rubrique n'est pas trouvée dans le
 * registre.
 */
public class DefinitionNotFoundException extends RuntimeException {
    /**
     * Crée une exception de définition introuvable.
     *
     * @param key La clé de la rubrique.
     */
    public DefinitionNotFoundException(String key) {
        super("Definition not found for rubrique: " + key);
    }
}
