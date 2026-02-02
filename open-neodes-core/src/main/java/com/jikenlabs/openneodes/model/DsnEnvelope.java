package com.jikenlabs.openneodes.model;

import java.util.List;

/**
 * Enveloppe technique représentant la structure technique complète d'un fichier
 * DSN.
 *
 * @param header       Le bloc d'en-tête (S10).
 * @param declarations La liste des déclarations (S20).
 * @param footer       Le bloc de pied de page (S90).
 */
public record DsnEnvelope(
                DsnBlockInstance header,
                List<DsnBlockInstance> declarations,
                DsnBlockInstance footer) {
}
