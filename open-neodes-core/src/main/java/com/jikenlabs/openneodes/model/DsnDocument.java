package com.jikenlabs.openneodes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Conteneur racine pour un document DSN parsé.
 */
public class DsnDocument {
    private final List<DsnBlockInstance> rootBlocks = new ArrayList<>();
    private int totalRubrics = 0;
    private String normVersion;

    /**
     * Constructeur par défaut.
     */
    public DsnDocument() {
    }

    /**
     * Ajoute un bloc racine au document.
     *
     * @param block Le bloc à ajouter.
     */
    public void addRootBlock(DsnBlockInstance block) {
        rootBlocks.add(block);
    }

    /**
     * Récupère la liste des blocs racines.
     *
     * @return La liste modifiable des blocs racines.
     */
    public List<DsnBlockInstance> getRootBlocks() {
        return rootBlocks;
    }

    /**
     * Récupère le nombre total de rubriques.
     *
     * @return Le nombre total de rubriques traitées.
     */
    public int getTotalRubrics() {
        return totalRubrics;
    }

    /**
     * Définit le nombre total de rubriques.
     *
     * @param totalRubrics Le nombre total de rubriques traitées.
     */
    public void setTotalRubrics(int totalRubrics) {
        this.totalRubrics = totalRubrics;
    }

    /**
     * Récupère la version de la norme utilisée.
     * 
     * @return La version de la norme.
     */
    public String getNormVersion() {
        return normVersion;
    }

    /**
     * Définit la version de la norme utilisée.
     * 
     * @param normVersion La version de la norme.
     */
    public void setNormVersion(String normVersion) {
        this.normVersion = normVersion;
    }

    /**
     * Recherche une valeur ou un bloc via un chemin "humain" ou technique.
     * Supporte la recherche profonde récursive sur tout le document.
     *
     * @param path Le chemin pointé (ex: "Siren").
     * @return Le résultat de la requête (QueryResult).
     */
    public QueryResult query(String path) {
        if (path == null || path.isBlank())
            return QueryResult.empty();

        List<Object> results = new ArrayList<>();
        for (DsnBlockInstance root : rootBlocks) {
            QueryResult subResult = root.query(path);
            results.addAll(subResult.items());
        }

        return new QueryResult(results);
    }

    /**
     * Exporte le document complet en Map humaine.
     * 
     * @return Map humaine.
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (DsnBlockInstance root : rootBlocks) {
            result.putAll(root.toMap());
        }
        return result;
    }
}
