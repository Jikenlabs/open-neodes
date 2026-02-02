# 🛠️ Guide Développeur open-neodes

Ce guide est destiné aux développeurs souhaitant contribuer au projet ou comprendre son fonctionnement interne.

## 🏗️ Architecture Modulaire

Le projet est composé de :

### **`open-neodes-core`** : Le cœur de la bibliothèque.
    
- `core` : Tokenisation ISO-8859-1 et streaming bas niveau.
- `engine` : Machine à états (Stack-based) pour reconstruire la hiérarchie DSN.
- `norm` : Registre des normes chargé dynamiquement depuis les fichiers YAML.
- `facade` : API simplifiée pour l'utilisateur final.


## 🌊 Le Moteur de Parsing

Le parsing s'appuie sur le `DsnHierarchicalParser` qui utilise une pile (`Stack`) pour suivre la profondeur des blocs DSN.
- Chaque ligne est lue en streaming.
- Le moteur détermine s'il faut ouvrir un nouveau bloc, fermer le courant, ou ajouter une rubrique.

## 📚 Ajouter une nouvelle Norme

La bibliothèque n'embarque pas de code "hardcodé" pour les structures DSN. Tout est défini dans `src/main/resources/norm-*.yaml`.


## 🧪 Tests et Couverture

Nous visons une couverture de code minimale de **80%**.

- Exécuter les tests : `mvn test`
- Vérifier la couverture (JaCoCo) : `mvn jacoco:report`
- Les rapports sont disponibles dans `target/site/jacoco/index.html` de chaque module.

## 🚀 Performance et Virtual Threads

**open-neodes** est optimisé pour Java 25. L'utilisation de `Executors.newVirtualThreadPerTaskExecutor()` permet de traiter des centaines de fichiers DSN en parallèle de manière extrêmement efficace sur le plan des ressources système.
