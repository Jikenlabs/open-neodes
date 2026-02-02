package com.jikenlabs.openneodes.model;

import java.util.stream.Stream;

/**
 * Hiérarchie scellée représentant les événements d'un lot DSN.
 */
public sealed interface DsnLotEvent {

    /**
     * Événement d'en-tête de lot.
     * 
     * @param header L'instance du bloc S10.
     */
    record LotHeaderEvent(DsnBlockInstance header) implements DsnLotEvent {
    }

    /**
     * Événement d'en-tête de groupe.
     * 
     * @param header L'instance du bloc S20 d'envoi.
     */
    record GroupHeaderEvent(DsnBlockInstance header) implements DsnLotEvent {
    }

    /**
     * Événement de déclaration DSN.
     * Contient un Stream permettant de lire les lignes de la déclaration de manière
     * paresseuse.
     * 
     * @param lines Stream de lignes de la déclaration.
     */
    record DeclarationEvent(Stream<String> lines) implements DsnLotEvent {
    }

    /**
     * Événement de pied de groupe.
     * 
     * @param footer L'instance du bloc S20 de fin (optionnel selon structure).
     */
    record GroupFooterEvent(DsnBlockInstance footer) implements DsnLotEvent {
    }

    /**
     * Événement de pied de lot.
     * 
     * @param footer L'instance du bloc S90.
     */
    record LotFooterEvent(DsnBlockInstance footer) implements DsnLotEvent {
    }

    /**
     * Événement d'erreur.
     * 
     * @param error L'exception rencontrée.
     */
    record ErrorEvent(Throwable error) implements DsnLotEvent {
    }
}
