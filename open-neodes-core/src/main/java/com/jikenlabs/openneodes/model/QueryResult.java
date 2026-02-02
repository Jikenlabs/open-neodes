package com.jikenlabs.openneodes.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Encapsule le résultat d'une recherche dans le document DSN.
 * Permet de manipuler facilement des résultats qui peuvent être :
 * - Un bloc unique (DsnBlockInstance)
 * - Une valeur de rubrique unique (String, Integer, etc.)
 * - Une liste de blocs ou de valeurs
 * 
 * @param items Liste des objets résultant de la requête.
 */
public record QueryResult(List<Object> items) {

    /**
     * Constructeur canonique avec garantie d'immutabilité.
     * 
     * @param items Liste des items.
     */
    public QueryResult {
        // Garantir l'immutabilité
        items = (items == null) ? List.of() : List.copyOf(items);
    }

    /**
     * Crée un résultat vide.
     * 
     * @return Un QueryResult vide.
     */
    public static QueryResult empty() {
        return new QueryResult(List.of());
    }

    /**
     * Crée un QueryResult à partir d'un objet ou d'une liste.
     * 
     * @param item L'objet ou la liste d'objets.
     * @return Un nouveau QueryResult.
     */
    @SuppressWarnings("unchecked")
    public static QueryResult of(Object item) {
        if (item == null)
            return empty();
        if (item instanceof List<?> list) {
            return new QueryResult((List<Object>) list);
        }
        if (item instanceof QueryResult qr)
            return qr;
        return new QueryResult(List.of(item));
    }

    /**
     * Vérifie si le résultat est vide.
     * 
     * @return true si vide.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Vérifie si au moins un résultat est présent.
     * 
     * @return true si présent.
     */
    public boolean isPresent() {
        return !items.isEmpty();
    }

    /**
     * Retourne le nombre d'éléments.
     * 
     * @return Le nombre d'éléments.
     */
    public int size() {
        return items.size();
    }

    /**
     * Retourne le premier résultat s'il existe.
     * 
     * @return Un Optional contenant le premier item.
     */
    public Optional<Object> first() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    /**
     * Retourne le premier résultat casté dans le type demandé.
     * 
     * @param <T>  Type attendu.
     * @param type Classe du type attendu.
     * @return Un Optional typé.
     */
    public <T> Optional<T> first(Class<T> type) {
        return items.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    /**
     * Retourne le résultat si c'est un bloc unique.
     * 
     * @return Un Optional de DsnBlockInstance.
     */
    public Optional<DsnBlockInstance> asBlock() {
        return first(DsnBlockInstance.class);
    }

    /**
     * Retourne le résultat si c'est une valeur unique (pas un bloc).
     * 
     * @return Un Optional de la valeur.
     */
    public Optional<Object> asValue() {
        return first().filter(i -> !(i instanceof DsnBlockInstance));
    }

    /**
     * Retourne la valeur sous forme de String si elle existe.
     * 
     * @return Un Optional de String.
     */
    public Optional<String> asString() {
        return asValue().map(Object::toString);
    }

    /**
     * Retourne la liste filtrée de tous les blocs dans les résultats.
     * 
     * @return Une liste de DsnBlockInstance.
     */
    public List<DsnBlockInstance> asBlocks() {
        return items.stream()
                .filter(DsnBlockInstance.class::isInstance)
                .map(DsnBlockInstance.class::cast)
                .toList();
    }

    /**
     * Retourne la liste filtrée de toutes les valeurs (pas des blocs) dans les
     * résultats.
     * 
     * @return Une liste d'objets valeurs.
     */
    public List<Object> asValues() {
        return items.stream()
                .filter(i -> !(i instanceof DsnBlockInstance))
                .toList();
    }

    /**
     * Retourne un stream des items.
     * 
     * @return Le Stream d'objets.
     */
    public Stream<Object> stream() {
        return items.stream();
    }

    /**
     * Retourne la valeur brute (Object) si unique, ou la liste d'objets
     * {@code (List<Object>)}.
     * Utile pour l'affichage ou la sérialisation simple.
     * 
     * @return La valeur unique ou la liste complète.
     */
    public Object toValue() {
        if (items.isEmpty())
            return null;
        return (items.size() == 1) ? items.get(0) : items;
    }

    /**
     * Permet de chaîner les requêtes.
     * Si le résultat contient des blocs, exécute la requête sur chacun d'eux et
     * agrège les résultats.
     * 
     * @param path Le chemin de la sous-requête.
     * @return Un nouveau QueryResult contenant les résultats agrégés.
     */
    public QueryResult query(String path) {
        if (isEmpty())
            return this;
        List<Object> subResults = items.stream()
                .filter(DsnBlockInstance.class::isInstance)
                .map(DsnBlockInstance.class::cast)
                .map(b -> b.query(path))
                .flatMap(qr -> qr.items().stream())
                .toList();
        return new QueryResult(subResults);
    }
}
