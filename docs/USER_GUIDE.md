# 📖 Guide Utilisateur open-neodes

Ce guide vous aide à démarrer avec la bibliothèque **open-neodes** pour traiter vos fichiers DSN.

!!! note "Installation"
    La bibliothèque est disponible sous forme d'artefact Maven. Ajoutez ceci à votre `pom.xml` :

```xml
<dependency>
    <groupId>com.jikenlabs</groupId>
    <artifactId>open-neodes-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 🚀 Parser une DSN

Le point d'entrée principal est la classe `DsnParser`.

### Lecture simple
```java
import com.jikenlabs.openneodes.facade.DsnParser;
import com.jikenlabs.openneodes.model.DsnDocument;
import java.io.File;

File dsnFile = new File("src/test/resources/declaration.dsn");
DsnDocument doc = DsnParser.parseAutoDetect(dsnFile);

System.out.println("Norme détectée : " + doc.getNormVersion());
```

### Lecture Asynchrone (Virtual Threads)
Pour ne pas bloquer votre thread principal, utilisez la variante asynchrone :
```java
DsnParser.parseAutoDetectAsync(inputStream)
         .thenAccept(doc -> {
             System.out.println("Document parsé !");
         });
```

## 🔍 Extraire des données (Query Engine)

**open-neodes** propose un moteur de requêtage fluide pour éviter de parcourir manuellement des structures de données complexes.

### Récupérer une valeur unique
```java
String siren = doc.query("Entreprise.Siren")
                  .asString()
                  .orElseThrow();
```

### Récupérer une liste de valeurs
```java
List<String> noms = doc.query("Individu.NomFamille")
                       .asValues()
                       .stream()
                       .map(Object::toString)
                       .toList();
```

### Navigation hiérarchique
Vous pouvez chaîner les requêtes pour naviguer dans la structure :
```java
var contrat = doc.query("Individu")
                 .query("Contrat")
                 .query("Nature");
```

## 🛡️ Gestion des Erreurs

La bibliothèque lève des exceptions spécifiques en cas de problème :
- `InvalidDsnFormatException` : Si le fichier n'est pas une DSN valide.
- `DefinitionNotFoundException` : Si la version de la norme n'est pas supportée.
- `DsnBusinessException` : Erreurs liées à la logique métier durant le parsing.
