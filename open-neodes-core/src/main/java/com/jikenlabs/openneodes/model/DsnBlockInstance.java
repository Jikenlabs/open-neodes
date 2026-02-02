package com.jikenlabs.openneodes.model;

import com.jikenlabs.openneodes.norm.DsnBlockDefinition;
import com.jikenlabs.openneodes.norm.RubriqueDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Instance concrète d'un bloc DSN dans un document.
 * Utilise SequencedMap pour préserver l'ordre des rubriques et Object pour
 * stocker les valeurs typées.
 */
public class DsnBlockInstance {
    private final String key;
    private final DsnBlockDefinition definition;
    private final SequencedMap<String, Object> values = new LinkedHashMap<>();
    private final Map<String, List<DsnBlockInstance>> children = new LinkedHashMap<>();

    /**
     * Crée un bloc avec la clé spécifiée.
     *
     * @param key La clé du bloc.
     */
    public DsnBlockInstance(String key) {
        this(key, null);
    }

    /**
     * Crée un bloc avec la clé et la définition spécifiées.
     *
     * @param key        La clé du bloc.
     * @param definition La définition du bloc.
     */
    public DsnBlockInstance(String key, DsnBlockDefinition definition) {
        this.key = key;
        this.definition = definition;
    }

    /**
     * Récupère la clé du bloc.
     *
     * @return La clé du bloc.
     */
    public String getKey() {
        return key;
    }

    /**
     * Récupère la définition du bloc.
     *
     * @return La définition du bloc.
     */
    public DsnBlockDefinition getDefinition() {
        return definition;
    }

    /**
     * Ajoute une valeur à ce bloc.
     *
     * @param rubriqueKey La clé de la rubrique.
     * @param value       La valeur.
     */
    public void addValue(String rubriqueKey, Object value) {
        values.put(rubriqueKey, value);
    }

    /**
     * Récupère une valeur.
     *
     * @param rubriqueKeyOrName La clé ou le nom de la rubrique.
     * @return La valeur.
     */
    public Object getValue(String rubriqueKeyOrName) {
        // Essai par clé d'abord
        if (values.containsKey(rubriqueKeyOrName)) {
            return values.get(rubriqueKeyOrName);
        }
        // Essai par nom si la définition est disponible
        if (definition != null) {
            for (Map.Entry<String, RubriqueDefinition> entry : definition.fields().entrySet()) {
                if (entry.getValue().name().equalsIgnoreCase(rubriqueKeyOrName)) {
                    return values.get(entry.getKey());
                }
            }
        }
        return null;
    }

    /**
     * Récupère une valeur typée.
     *
     * @param rubriqueKeyOrName La clé ou le nom de la rubrique.
     * @param type              Le type attendu.
     * @param <T>               Le type générique.
     * @return La valeur typée.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String rubriqueKeyOrName, Class<T> type) {
        return (T) getValue(rubriqueKeyOrName);
    }

    /**
     * Ajoute un bloc enfant.
     *
     * @param child Le bloc enfant.
     */
    public void addChild(DsnBlockInstance child) {
        children.computeIfAbsent(child.getKey(), k -> new ArrayList<>()).add(child);
    }

    /**
     * Récupère les enfants d'un type donné.
     *
     * @param blockKey La clé du bloc enfant.
     * @return La liste des enfants pour cette clé.
     */
    public List<DsnBlockInstance> getChildren(String blockKey) {
        return children.getOrDefault(blockKey, List.of());
    }

    /**
     * Récupère toutes les valeurs.
     *
     * @return Les valeurs du bloc.
     */
    public SequencedMap<String, Object> getValues() {
        return values;
    }

    /**
     * Récupère tous les enfants.
     *
     * @return La map des enfants.
     */
    public Map<String, List<DsnBlockInstance>> getChildren() {
        return children;
    }

    /**
     * Récupère une valeur par son nom technique ou par son nom lisible.
     * 
     * @param name Nom de la rubrique.
     * @return La valeur ou null.
     */
    public Object getValueByName(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }

        if (definition != null) {
            for (Map.Entry<String, RubriqueDefinition> entry : definition.fields().entrySet()) {
                if (name.equalsIgnoreCase(entry.getValue().name())) {
                    return values.get(entry.getKey());
                }
            }
        }
        return null;
    }

    /**
     * Détache tous les enfants.
     */
    public void detachChildren() {
        children.clear();
    }

    /**
     * Récupère les enfants d'un type donné par leur nom lisible.
     *
     * @param name Nom lisible du bloc.
     * @return La liste des enfants correspondants.
     */
    public List<DsnBlockInstance> getChildrenByName(String name) {
        List<DsnBlockInstance> found = new ArrayList<>();
        for (List<DsnBlockInstance> list : children.values()) {
            if (!list.isEmpty()) {
                DsnBlockInstance first = list.get(0);
                if (first.definition != null && name.equalsIgnoreCase(first.definition.name())) {
                    found.addAll(list);
                } else if (name.equalsIgnoreCase(first.key)) {
                    found.addAll(list);
                }
            }
        }
        return found;
    }

    /**
     * Recherche une valeur ou un bloc via un chemin "humain" ou technique.
     * Supporte la recherche profonde récursive (peut sauter des niveaux).
     *
     * @param path Chemin avec points (ex: "Entreprise.Siren").
     * @return Le résultat de la requête (QueryResult).
     */
    public QueryResult query(String path) {
        if (path == null || path.isBlank()) {
            return QueryResult.of(this);
        }

        String[] parts = path.split("\\.", 2);
        String currentToken = parts[0];
        String remainingPath = (parts.length > 1) ? parts[1] : null;

        List<Object> results = new ArrayList<>();
        collectResults(this, currentToken, remainingPath, results);

        return new QueryResult(results);
    }

    private void collectResults(DsnBlockInstance node, String token, String remaining, List<Object> results) {
        // 1. Recherche à ce niveau précis (Rubrique ou enfant direct)
        Object val = node.getValueByName(token);
        if (val != null) {
            if (remaining == null) {
                results.add(val);
            }
            // Si remaining != null, on ne descend pas dans une rubrique scalaire.
        }

        List<DsnBlockInstance> matches = node.getChildrenByName(token);
        for (DsnBlockInstance match : matches) {
            if (remaining == null) {
                results.add(match);
            } else {
                QueryResult subResult = match.query(remaining);
                results.addAll(subResult.items());
            }
        }

        // 2. Recherche profonde récursive dans tous les enfants
        // On continue à descendre pour trouver d'autres occurrences du MÊME token plus
        // bas
        for (List<DsnBlockInstance> childList : node.getChildren().values()) {
            for (DsnBlockInstance child : childList) {
                collectResults(child, token, remaining, results);
            }
        }
    }

    /**
     * Transforme ce bloc en une Map "humaine" récursive.
     * Utile pour la sérialisation JSON.
     *
     * @return Une Map avec les noms lisibles en clés.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> content = new LinkedHashMap<>();

        // 1. Rubriques
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String label = entry.getKey();
            if (definition != null && definition.fields().containsKey(entry.getKey())) {
                label = definition.fields().get(entry.getKey()).name();
            }
            content.put(label, entry.getValue());
        }

        // 2. Enfants
        for (Map.Entry<String, List<DsnBlockInstance>> entry : children.entrySet()) {
            List<DsnBlockInstance> childrenList = entry.getValue();
            if (childrenList.isEmpty())
                continue;

            String childName = (childrenList.get(0).definition != null)
                    ? childrenList.get(0).definition.name()
                    : entry.getKey();

            if (childrenList.size() == 1) {
                content.put(childName, childrenList.get(0).toMap().get(childName));
            } else {
                List<Object> simplifiedList = new ArrayList<>();
                for (DsnBlockInstance child : childrenList) {
                    simplifiedList.add(child.toMap().get(childName));
                }
                content.put(childName, simplifiedList);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put((definition != null) ? definition.name() : key, content);
        return result;
    }
}
