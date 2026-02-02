package com.jikenlabs.openneodes.engine;

import com.jikenlabs.openneodes.model.DsnBlockInstance;

/**
 * Écouteur pour les événements de parsing DSN.
 */
@FunctionalInterface
public interface DsnBlockListener {
    /**
     * Appelé lorsqu'une instance de bloc (et tous ses enfants) a été entièrement
     * parsée.
     *
     * @param block L'instance de bloc terminée.
     */
    void onBlockCompleted(DsnBlockInstance block);
}
