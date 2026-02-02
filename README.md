# 🚀 open-neodes

[![Java Version](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![Built with Maven](https://img.shields.io/badge/Built%20with-Maven-C71A36.svg?style=flat-square&logo=apache-maven)](https://maven.apache.org/)

**open-neodes** est une bibliothèque Java 25 haute performance, conçue pour parser, valider et manipuler les fichiers DSN (*Déclaration Sociale Nominative*). 
Le projet est basé sur les spécifications officielles de la norme DSN disponibles sur : [net-entreprises.fr](https://www.net-entreprises.fr/declaration/norme-et-documentation-dsn/)

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/djtalez)

---

## ✨ Points Forts

- **⚡ Java 25 Natif** : Exploite les **Virtual Threads (Project Loom)** pour des entrées/sorties non-bloquantes, le **Pattern Matching** pour un parsing robuste, et les **Records** immuables.
- **📚 Registre de Normes Dynamique** : Définitions des normes DSN basées sur YAML. Supportez de nouvelles versions (P25V01, P26V01, etc.) instantanément.
- **🌊 Architecture de Streaming** : Traitez des fichiers DSN massifs avec une consommation mémoire constante.
- **🔍 API de Requêtage Fluide** : Naviguez sans effort dans les hiérarchies complexes via le moteur `query`.

---

## 🛠️ Documentation

- 📖 [**Guide Utilisateur**](docs/USER_GUIDE.md) : Apprendre à intégrer la bibliothèque et parser des fichiers.
- 🛠️ [**Guide Développeur**](docs/DEVELOPER_GUIDE.md) : Comprendre l'architecture et contribuer au projet.

---

## 🚀 Démarrage Rapide

```java
import com.jikenlabs.openneodes.facade.DsnParser;
import com.jikenlabs.openneodes.model.DsnDocument;
import java.io.FileInputStream;

// Parsing asynchrone utilisant les Virtual Threads
InputStream is = new FileInputStream("declaration.dsn");
DsnDocument doc = DsnParser.parseAutoDetectAsync(is).join();

### 🔍 Requêtage Fluide et Puissant

Arrêtez de lutter avec les Maps imbriquées. Utilisez le moteur de `query` :

```java
// Extraction d'une valeur unique profondément imbriquée
String siren = doc.query("Entreprise.Siren").asString().orElse("Par défaut");

// Gestion de résultats multiples (ex: liste d'individus)
List<Object> tousLesNoms = doc.query("Individu.NomFamille").asValues();

// Chaînage fluide pour les traversées complexes
doc.query("Individu")
   .query("Contrat")
   .query("Nature")
   .asString();
```

---

## 🏗️ Architecture

Le projet suit une architecture modulaire propre, inspirée de l'hexagonal :

- `com.jikenlabs.openneodes.facade` : API de haut niveau pour les utilisateurs.
- `com.jikenlabs.openneodes.core` : Entrées/sorties bas niveau, tokenisation ISO-8859-1 et streaming.
- `com.jikenlabs.openneodes.engine` : Machine à états à pile pour la reconstruction de la hiérarchie.
- `com.jikenlabs.openneodes.norm` : Registre YAML et gestion des métadonnées.
- `com.jikenlabs.openneodes.model` : Records Java modernes pour la représentation du document.

---

## 💖 Soutenir le Projet

Si **open-neodes** vous est utile et que vous souhaitez soutenir son développement, vous pouvez m'aider de plusieurs façons :

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/djtalez)


Votre soutien aide à :
- 🔧 Maintenir et améliorer la bibliothèque
- 📚 Enrichir la documentation
- 🚀 Ajouter de nouvelles fonctionnalités
- ☕ Me garder motivé avec du café !

Merci à tous les contributeurs et sponsors ! 🙏

---

## 🏢 Support Professionnel

Besoin d'un **support garanti** et des **mises à jour annuelles des normes DSN** pour votre entreprise ?

**Jiken Labs** propose une offre de support professionnel incluant :

- 📅 **Mise à jour annuelle** des fichiers de normes (P26V01, P27V01, etc.) dès leur publication officielle
- 🛠️ **Support technique prioritaire** par email
- 🔧 **Assistance à l'intégration** dans vos systèmes existants
- 📋 **SLA garanti** pour les environnements de production

👉 **[En savoir plus sur www.jikenlabs.com/open-neodes](https://www.jikenlabs.com/open-neodes)**

---

## 📄 Licence

Distribué sous la licence **Apache 2.0**. Voir `LICENSE` pour plus d'informations.


