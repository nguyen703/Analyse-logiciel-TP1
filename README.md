# Manuel d'installation et d'utilisation

Lien dépôt : https://github.com/nguyen703/Analyse-logiciel-TP1  
Lien démo :  
TP1: https://drive.google.com/file/d/1wmeupOskjJY02dHnDa-lZ5M8qQfAtg8k/view?usp=sharing  
TP2: https://drive.google.com/file/d/1QG2syALo9HIHgWGUSbo01g-I5YiXYqae/view?usp=sharing

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
     - **[TP2]** Métriques de couplage entre classes
     - **[TP2]** Identification de modules par clustering hiérarchique

3. **Explorer les résultats**
   - Onglet **Statistics** : visualisation des données calculées.
   - Onglet **Call Graph** : visualisation du graphe d’appel des méthodes.
   - **[TP2]** Onglet **Coupling Graph** : visualisation du graphe de couplage pondéré entre classes avec :
     - Graphe interactif coloré par intensité de couplage
     - Panneau textuel détaillant toutes les relations
     - Statistiques de couplage (total, moyenne, relation la plus forte)
     - Code couleur : 🔴 Rouge (>10%), 🟠 Orange (5-10%), 🟢 Vert (<5%)

### 🔄 Rafraîchir une analyse

- Modifiez les fichiers sources, puis relancez l’analyse pour mettre à jour les données.

---

## 4. Fonctionnalités TP2

### Analyse de couplage

Le programme calcule le couplage entre toutes les classes :

- **Formule** : `Couplage(A,B) = Relations(A,B) / TotalRelations`
  - `Relations(A,B)` : nombre d'appels entre classe A et classe B
  - `TotalRelations` : nombre total d'appels dans le projet
- Le système utilise le graphe d'appel créé lors de l'analyse
- Seules les classes du projet sont comptées (pas les classes Java externes)
- La matrice de couplage est sauvegardée en mémoire

**Affichage** :

- Graphe avec GraphStream : chaque ligne = une relation de couplage
- Couleurs par intensité : Rouge (>10%), Orange (5-10%), Vert (<5%)
- Liste des relations triées du poids le plus fort au plus faible
- Statistiques : total de relations, moyenne, relation la plus forte

### Clustering hiérarchique

Le programme construit un arbre (dendrogramme) et trouve des groupes de classes :

**1. Construction de l'arbre** (`buildDendrogram()`) :

- Départ : chaque classe = un groupe
- Répétition :
  - Calcul du couplage entre tous les groupes
  - Fusion des deux groupes les plus couplés
  - Création d'un nouveau nœud parent
- Fin : un seul groupe racine

**2. Trouver les modules** (`identifyModules()`) :

- Parcours de l'arbre depuis la racine
- Pour chaque nœud : calcul du couplage interne moyen
- Si `CouplageInterne > CP` → ce nœud est un module
- Sinon → vérifier les enfants
- Maximum : M/2 modules pour M classes
- Un nœud devient module → on ne descend plus dans cette branche

**Résultats** :

- Console : arbre construit + liste des modules + leurs classes + couplage interne
- Interface : visible dans l'onglet Coupling Graph

### Deux versions disponibles

**1. Version JDT** (`Analyzer.java` + `AnalyzerUI.java`) :

- Utilise Eclipse JDT pour lire le code
- Visiteurs personnalisés pour extraire les données
- Commande : `./gradlew run`

**2. Version Spoon** (`SpoonAnalyzer.java` + `AnalyzerSpoonUI.java`) :

- Utilise Spoon pour lire le code
- Utilise `TypeFilter` pour collecter les éléments
- Utilise `CtInvocation` pour les appels de méthodes

---

## 5. Structure du projet

```
src/
 ├── main/java/com/hnguyen703/
 │     ├── AnalyzerUI.java           → Interface graphique principale (JDT)
 │     ├── AnalyzerSpoonUI.java      → [TP2] Interface graphique (Spoon)
 │     ├── analyzer/
 │     │   ├── Analyzer.java         → Analyseur JDT + couplage + clustering
 │     │   └── SpoonAnalyzer.java    → [TP2] Analyseur Spoon + couplage + clustering
 │     ├── visitors/                 → Visiteurs JDT pour l'AST
 │     └── ui/
 │         └── components/
 │             ├── StatisticsView.java
 │             ├── CallGraphView.java
 │             └── CouplingGraphView.java  → [TP2] Graphe de couplage
 ├── main/java/dossier/test/         → Fichiers Java de test
 │
 ├── build.gradle                    → Configuration Gradle
 ├── StructureProjet.md              → Présentation de la structure du projet
 └── README.md                       → Ce manuel d'installation et d'utilisation
```

---

## 6. Exemples d'utilisation

### Analyser un projet simple

Modifiez la classe main dans `build.gradle` si nécessaire pour choisir entre JDT et Spoon.  
For Spoon:
`mainClass = 'com.hnguyen703.AnalyzerSpoonUI'`  
For JDT:
`mainClass = 'com.hnguyen703.AnalyzerUI'`

```bash
./gradlew run
# Sélectionner le dossier src/main/java/dossier/test
# Cliquer sur "Analyser"
```

### Visualiser le couplage

1. Lancer l'analyse d'un projet
2. Naviguer vers l'onglet "Coupling Graph"
3. Observer les relations colorées selon leur intensité
4. Consulter les statistiques détaillées dans le panneau textuel

### Identifier les modules

Les modules identifiés sont automatiquement affichés dans la console après l'analyse, avec :

- Le nombre de modules trouvés
- Les classes appartenant à chaque module
- Le couplage interne de chaque module
