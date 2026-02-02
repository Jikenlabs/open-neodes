package com.jikenlabs.openneodes.facade;

import com.jikenlabs.openneodes.model.DsnBlockInstance;

/**
 * Interface pour recevoir les événements de parsage d'un lot DSN en streaming.
 */
public interface DsnLotListener {
    /**
     * Appelé quand l'en-tête du lot (S05) est lu.
     * 
     * @param header L'instance du bloc S05.
     */
    void onLotHeader(DsnBlockInstance header);

    /**
     * Appelé quand un nouveau groupe (S06) commence.
     * 
     * @param groupHeader L'instance du bloc S06.
     */
    void onGroupHeader(DsnBlockInstance groupHeader);

    /**
     * Appelé pour chaque déclaration DSN trouvée dans le groupe.
     * 
     * @param lines Le flux de lignes de la déclaration (lazy).
     */
    void onDeclaration(java.util.stream.Stream<String> lines);

    /**
     * Appelé quand le pied d'un groupe (S96) est lu.
     * 
     * @param groupFooter L'instance du bloc S96.
     */
    void onGroupFooter(DsnBlockInstance groupFooter);

    /**
     * Appelé quand le pied du lot (S95) est lu.
     * 
     * @param lotFooter L'instance du bloc S95.
     */
    void onLotFooter(DsnBlockInstance lotFooter);

    /**
     * Appelé en cas d'erreur de parsage.
     * 
     * @param e L'exception rencontrée.
     */
    void onError(Throwable e);

    /**
     * Appelé quand le parsage du lot est terminé.
     */
    void onComplete();
}
