package com.jikenlabs.openneodes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un lot complet de déclarations DSN (S05...S95).
 */
public class DsnLot {
    private final DsnBlockInstance header;
    private final List<DsnGroup> groups = new ArrayList<>();
    private DsnBlockInstance footer;

    /**
     * Crée un lot avec l'en-tête spécifié.
     * 
     * @param header L'en-tête du lot.
     */
    public DsnLot(DsnBlockInstance header) {
        this.header = header;
    }

    /**
     * Récupère l'en-tête du lot (S05).
     * 
     * @return L'instance du bloc S05.
     */
    public DsnBlockInstance getHeader() {
        return header;
    }

    /**
     * Récupère la liste des groupes du lot.
     * 
     * @return La liste des groupes.
     */
    public List<DsnGroup> getGroups() {
        return groups;
    }

    /**
     * Ajoute un groupe au lot.
     * 
     * @param group Le groupe à ajouter.
     */
    public void addGroup(DsnGroup group) {
        this.groups.add(group);
    }

    /**
     * Récupère le pied de lot (S95).
     * 
     * @return L'instance du bloc S95.
     */
    public DsnBlockInstance getFooter() {
        return footer;
    }

    /**
     * Définit le pied de lot.
     * 
     * @param footer L'instance du bloc S95.
     */
    public void setFooter(DsnBlockInstance footer) {
        this.footer = footer;
    }
}
