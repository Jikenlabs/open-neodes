package com.jikenlabs.openneodes.engine;

/**
 * Énumération des états possibles lors du parsing d'un fichier DSN.
 */
public enum DsnFileState {
    /** Début du traitement. */
    START,
    /** Lecture des blocs S10 (En-tête). */
    READING_S10,
    /** Lecture des blocs S20 (Déclarations). */
    READING_S20,
    /** Lecture des blocs S90 (Fichiers techniques). */
    READING_S90,
    /** Fin du traitement. */
    END
}
