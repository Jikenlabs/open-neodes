package com.jikenlabs.openneodes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un groupe de déclarations au sein d'un lot DSN (S06...S96).
 */
public class DsnGroup {
    private final DsnBlockInstance header;
    private final List<List<String>> declarations = new ArrayList<>();
    private DsnBlockInstance footer;

    /**
     * Crée un groupe avec l'en-tête spécifié.
     * 
     * @param header L'en-tête du groupe.
     */
    public DsnGroup(DsnBlockInstance header) {
        this.header = header;
    }

    /**
     * Récupère l'en-tête du groupe.
     * 
     * @return L'instance du bloc S06.
     */
    public DsnBlockInstance getHeader() {
        return header;
    }

    /**
     * Récupère la liste des déclarations (lignes brutes).
     * 
     * @return La liste des déclarations.
     */
    public List<List<String>> getDeclarations() {
        return declarations;
    }

    /**
     * Ajoute une déclaration au groupe.
     * 
     * @param lines Les lignes de la déclaration.
     */
    public void addDeclaration(List<String> lines) {
        this.declarations.add(lines);
    }

    /**
     * Récupère le pied de groupe (S96).
     * 
     * @return L'instance du bloc S96.
     */
    public DsnBlockInstance getFooter() {
        return footer;
    }

    /**
     * Définit le pied de groupe.
     * 
     * @param footer L'instance du bloc S96.
     */
    public void setFooter(DsnBlockInstance footer) {
        this.footer = footer;
    }
}
