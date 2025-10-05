# Manuel d’installation et d’utilisation

Lien dépôt : https://github.com/nguyen703/Analyse-logiciel-TP1
Lien démo : https://drive.google.com/file/d/1wmeupOskjJY02dHnDa-lZ5M8qQfAtg8k/view?usp=sharing

## 1. Prérequis

- **Java 17** ou une version ultérieure  
  (vérifiez avec `java -version`)
- **Gradle 8+** installé  
  (vérifiez avec `gradle -v`)
- Un IDE compatible (IntelliJ IDEA, Eclipse, ...). Personnellement, j’utilise IntelliJ IDEA.

---

## 2. Installation

1. **Cloner le dépôt du projet** :

   ```bash
   git clone https://github.com/nguyen703/Analyse-logiciel-TP1.git
   cd analyseur-java
   ```

2. **Télécharger les dépendances et compiler le projet et lancer l’application simplement par** :

   ```bash
   ./gradlew run
   ```

    L’interface graphique devrait s’ouvrir automatiquement.

    Au cas où vous avez des erreurs avec JAVA_HOME, vous pouvez spécifier le chemin de votre JDK manuellement
    dans le terminal :

    ```bash
    export JAVA_HOME=/chemin/vers/votre/jdk
    ```

    Pour savoir où se trouve votre JDK, vous pouvez exécuter la commande suivante dans votre terminal :

    ```bash
    /usr/libexec/java_home -V
    ```
---

## 3. Utilisation

### Étapes principales

1. **Charger le projet à analyser**
   - Dans l’interface, cliquez le bouton `Parcourir` et sélectionnez un dossier contenant des fichiers `.java`.

2. **Lancer l’analyse**
   - Cliquez sur le bouton "Analyser".
   - Le programme parcourt les fichiers, construit l’arbre syntaxique (AST) et calcule les métriques suivantes :
     - Nombre de classes, méthodes, lignes, packages
     - Moyenne de lignes par méthode et d’attributs par classe
     - Classes avec le plus grand nombre de méthodes/attributs

3. **Explorer les résultats**
   - Onglet **Statistics** : visualisation des données calculées.
   - Onglet **Call Graph** : visualisation du graphe d’appel des méthodes.

### 🔄 Rafraîchir une analyse
   - Modifiez les fichiers sources, puis relancez l’analyse pour mettre à jour les données.

---

## 4. Structure du projet

```
src/
 ├── main/java/com/hnguyen703/
 │     ├── AnalyzerUI.java        → Interface graphique principale
 │     ├── analyzer/              → Logique d’analyse
 │     ├── visitors/              → Visiteurs JDT pour l’AST
 │     └── ui/                    → Composants d’affichage
 ├── main/java/dossier/test/      → Fichiers Java de test
 │
 ├── build.gradle                 → Configuration Gradle
 ├── StructureProjet.md           → Présentation de la structure du projet
 └── README.md                    → Ce manuel d’installation et d’utilisation
```
