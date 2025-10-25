package com.hnguyen703.analyzer;

import java.util.stream.Collectors;

import com.hnguyen703.analyzer.visitors.MethodDeclarationVisitor;
import com.hnguyen703.analyzer.visitors.MethodInvocationVisitor;
import com.hnguyen703.analyzer.visitors.TypeDeclarationVisitor;
import com.hnguyen703.analyzer.visitors.VariableDeclarationFragmentVisitor;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Analyzer {

    private int classCount = 0;
    private int lineCount = 0;
    private int methodCount = 0;
    private int packageCount = 0;
    private int attributeCount = 0;

    private int maxParams = 0;

    private final Map<String, Integer> methodsPerClass = new HashMap<>();
    private final Map<String, Integer> attributesPerClass = new HashMap<>();
    private final Map<String, Integer> linesPerMethod = new HashMap<>();
    private Map<String, Map<String, Double>> couplingMatrix;

    private final Map<String, Set<String>> callGraph = new HashMap<>();
    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }


    public void analyze(String projectPath) throws IOException {
        analyze(projectPath, true);
    }

    public static class ClusterNode {
        String name;
        Set<String> classes; // The classes in this cluster
        ClusterNode leftChild;
        ClusterNode rightChild;
        double mergeCoupling; // The coupling value when this node was formed

        // Constructor for a leaf (single class)
        public ClusterNode(String className) {
            this.name = className;
            this.classes = new HashSet<>();
            this.classes.add(className);
            this.leftChild = null;
            this.rightChild = null;
            this.mergeCoupling = 0.0;
        }

        // Constructor for a merged cluster
        public ClusterNode(ClusterNode c1, ClusterNode c2, double mergeCoupling) {
            this.name = "(" + c1.name + ", " + c2.name + ")";
            this.classes = new HashSet<>(c1.classes);
            this.classes.addAll(c2.classes);
            this.leftChild = c1;
            this.rightChild = c2;
            this.mergeCoupling = mergeCoupling;
        }

        public Set<String> getClasses() {
            return classes;
        }

        public String getName() {
            return name;
        }
    }

    public void analyze(String projectPath, boolean printResults) throws IOException {
        // Reset counters for fresh analysis
        classCount = 0;
        lineCount = 0;
        methodCount = 0;
        packageCount = 0;
        attributeCount = 0;
        maxParams = 0;
        methodsPerClass.clear();
        attributesPerClass.clear();
        linesPerMethod.clear();
        callGraph.clear();
        couplingMatrix = null;

        try (var pathStream = Files.walk(Paths.get(projectPath))) {
            pathStream
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> parseFile(file.toFile()));
        }

        this.couplingMatrix = generateCouplingGraphInternal();

        if (printResults) {
            printResults();
        }


        double CP_THRESHOLD = 0.05; // Example threshold: 5%
        String clusteringResults = runClusteringAnalysis(CP_THRESHOLD);
        System.out.println(clusteringResults);
    }

    private void printResults() {
        // ---- Résultats ----
        System.out.println("1. Nombre de classes: " + classCount);
        System.out.println("2. Nombre de lignes de code: " + lineCount);
        System.out.println("3. Nombre total de méthodes: " + methodCount);
        System.out.println("4. Nombre total de packages: " + packageCount);

        double avgMethods = classCount == 0 ? 0 : (double) methodCount / classCount;
        double avgLines = methodCount == 0 ? 0 : (double) lineCount / methodCount;
        double avgAttrs = classCount == 0 ? 0 : (double) attributeCount / classCount;

        System.out.println("5. Nombre moyen de méthodes par classe: " + avgMethods);
        System.out.println("6. Nombre moyen de lignes de code par méthode: " + avgLines);
        System.out.println("7. Nombre moyen d'attributs par classe: " + avgAttrs);

        // ---- 8. Top 10% classes par nombre de méthodes ----
        int topNMethods = Math.max(1, (int) Math.ceil(classCount * 0.1));
        methodsPerClass.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topNMethods)
                .forEach(e -> System.out.println("8. Classe avec beaucoup de méthodes: " + e.getKey() + " (" + e.getValue() + ")"));

        // ---- 9. Top 10% classes par nombre d'attributs ----
        int topNAttrs = Math.max(1, (int) Math.ceil(classCount * 0.1));
        attributesPerClass.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topNAttrs)
                .forEach(e -> System.out.println("9. Classe avec beaucoup d'attributs: " + e.getKey() + " (" + e.getValue() + ")"));

        // ---- 10. Intersection des deux ----
        Set<String> topMethodsClasses = methodsPerClass.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topNMethods)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<String> topAttrsClasses = attributesPerClass.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topNAttrs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        topMethodsClasses.retainAll(topAttrsClasses);
        System.out.println("10. Classes dans les deux catégories: " + topMethodsClasses);

        // ---- 11. Classes avec plus de X méthodes ----
        int X = 5; // valeur choisie, peut être modifiée
        methodsPerClass.entrySet().stream()
                .filter(e -> e.getValue() > X)
                .forEach(e -> System.out.println("11. Classe avec > " + X + " méthodes: " + e.getKey() + " (" + e.getValue() + ")"));

        // ---- 12. Top 10% méthodes par LOC ----
        System.out.println("12. Top 10% méthodes par lignes de code:");
        int topNLines = Math.max(1, (int) Math.ceil(linesPerMethod.size() * 0.1));

        linesPerMethod.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topNLines)
                .forEach(e -> System.out.println("- " + e.getKey() + " (" + e.getValue() + " lignes de code)"));

        // ---- 13. Nombre max de paramètres ----
        System.out.println("13. Nombre maximal de paramètres par méthode: " + maxParams);

        // Generate and display coupling graph
        System.out.println("\n=== Graphe de couplage pondéré ===");
        Map<String, Map<String, Double>> couplingMatrix = generateCouplingGraph();
        printCouplingGraph(couplingMatrix);

        System.out.println("\n=== Graphe de couplage pondéré ===");
        // Use the cached matrix instead of regenerating
        printCouplingGraph(this.couplingMatrix);
    }

    public double calculateCoupling(String classA, String classB) {
        // Calculate coupling between two classes A and B
        // Couplage(A,B) = Relations between A and B / Total relations in the application

        int relationsBetweenAB = getRelationsBetweenAB(classA, classB);
        int totalRelations = getTotalRelations();

        if (totalRelations == 0) {
            return 0.0;
        }

        return (double) relationsBetweenAB / totalRelations;
    }

    /**
     * Public method for the UI to get the coupling graph.
     * Uses the cached matrix.
     */
    public Map<String, Map<String, Double>> generateCouplingGraph() {
        if (this.couplingMatrix == null) {
            // This ensures it can be called even if analyze() wasn't (e.g., testing)
            this.couplingMatrix = generateCouplingGraphInternal();
        }
        return this.couplingMatrix;
    }

    /**
     * Internal method to actually build the matrix.
     */
    private Map<String, Map<String, Double>> generateCouplingGraphInternal() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        // Get all unique class names
        Set<String> allClasses = getAllClasses();

        // Calculate coupling for each pair of classes
        for (String classA : allClasses) {
            Map<String, Double> couplings = new HashMap<>();

            for (String classB : allClasses) {
                if (!classA.equals(classB)) {
                    double coupling = calculateCoupling(classA, classB);
                    if (coupling > 0.0) {
                        couplings.put(classB, coupling);
                    }
                }
            }

            if (!couplings.isEmpty()) {
                matrix.put(classA, couplings);
            }
        }

        return matrix;
    }

    /**
     * Get all unique class names from the analyzed project
     * Only includes classes that are actually defined in the project (not external classes)
     */
    private Set<String> getAllClasses() {
        Set<String> classes = new HashSet<>();

        // Only add classes that are defined in the analyzed project
        // These are classes that appear in methodsPerClass or attributesPerClass
        classes.addAll(methodsPerClass.keySet());
        classes.addAll(attributesPerClass.keySet());


        return classes;
    }

    /**
     * Print the coupling graph in a readable format
     */
    private void printCouplingGraph(Map<String, Map<String, Double>> couplingMatrix) {
        if (couplingMatrix.isEmpty()) {
            System.out.println("Aucun couplage détecté entre les classes.");
            return;
        }

        System.out.println("Format: Classe A <-> Classe B : Poids de couplage (pourcentage)\n");

        // Sort by class name for consistent output
        List<String> sortedClasses = new ArrayList<>(couplingMatrix.keySet());
        Collections.sort(sortedClasses);

        int totalEdges = 0;
        for (String classA : sortedClasses) {
            Map<String, Double> couplings = couplingMatrix.get(classA);

            // Sort couplings by weight (descending)
            List<Map.Entry<String, Double>> sortedCouplings = new ArrayList<>(couplings.entrySet());
            sortedCouplings.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> entry : sortedCouplings) {
                String classB = entry.getKey();
                double weight = entry.getValue();

                // Print coupling with percentage
                System.out.printf("  %s <-> %s : %.4f (%.2f%%)\n",
                    classA, classB, weight, weight * 100);
                totalEdges++;
            }
        }

        System.out.println("\nNombre total de relations de couplage: " + totalEdges);
        System.out.println("Nombre de classes avec couplage: " + couplingMatrix.size());
    }

    private int getRelationsBetweenAB(String classA, String classB) {
        // Count relations (method calls) between classes A and B
        // A relation exists when a method in class A calls a method in class B
        // or when a method in class B calls a method in class A

        int count = 0;

        // Get all project-defined classes
        Set<String> projectClasses = getAllClasses();

        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet()) {
            String caller = entry.getKey(); // Format: ClassName.methodName
            String callerClass = extractClassName(caller);

            for (String callee : entry.getValue()) {
                String calleeClass = extractClassName(callee);

                // Only count if both classes are defined in the project
                if (!projectClasses.contains(callerClass) || !projectClasses.contains(calleeClass)) {
                    continue;
                }

                // Check if this is a relation between A and B (in either direction)
                if ((callerClass.equals(classA) && calleeClass.equals(classB)) ||
                    (callerClass.equals(classB) && calleeClass.equals(classA))) {
                    count++;
                }
            }
        }

        return count;
    }

    private int getTotalRelations() {
        // Count all relations (method calls) in the application
        // Each entry in callGraph represents relations from one method
        // Only count relations between classes defined in the project

        int total = 0;

        // Get all project-defined classes
        Set<String> projectClasses = getAllClasses();

        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet()) {
            String caller = entry.getKey();
            String callerClass = extractClassName(caller);

            // Skip if caller is not a project class
            if (!projectClasses.contains(callerClass)) {
                continue;
            }

            for (String callee : entry.getValue()) {
                String calleeClass = extractClassName(callee);

                // Only count relations between different project classes
                if (!callerClass.equals(calleeClass) && projectClasses.contains(calleeClass)) {
                    total++;
                }
            }
        }

        return total;
    }

    private String extractClassName(String methodSignature) {
        // Extract class name from method signature (format: ClassName.methodName)
        if (methodSignature == null || !methodSignature.contains(".")) {
            return "";
        }

        int lastDotIndex = methodSignature.lastIndexOf(".");
        String className = methodSignature.substring(0, lastDotIndex);

        // Handle cases where the method invocation includes object references
        // e.g., "testRunner.run" -> extract the last component before the method
        if (className.contains(".")) {
            int secondLastDot = className.lastIndexOf(".");
            className = className.substring(secondLastDot + 1);
        }

        // Capitalize first letter if needed (for consistency)
        if (!className.isEmpty() && Character.isLowerCase(className.charAt(0))) {
            // This might be an instance variable, try to infer class name
            // For simple cases, capitalize it
            className = Character.toUpperCase(className.charAt(0)) + className.substring(1);
        }

        return className;
    }


    private void parseFile(File file) {
        try {
            String source = new String(Files.readAllBytes(file.toPath()));
            lineCount += source.split("\n").length;

            ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            // ---- Packages ----
            if (cu.getPackage() != null) {
                packageCount++;
            }

            // ---- Classes ----
            TypeDeclarationVisitor typeVisitor = new TypeDeclarationVisitor();
            cu.accept(typeVisitor);
            classCount += typeVisitor.getTypes().size();

            for (TypeDeclaration type : typeVisitor.getTypes()) {
                String className = type.getName().toString();

                // ---- Méthodes ----
                MethodDeclarationVisitor methodVisitor = new MethodDeclarationVisitor();
                type.accept(methodVisitor);
                int nbMethods = methodVisitor.getMethods().size();
                methodsPerClass.put(className, nbMethods);
                methodCount += nbMethods;

                // Lignes par méthode
                for (MethodDeclaration m : methodVisitor.getMethods()) {
                    int start = cu.getLineNumber(m.getStartPosition());
                    int end = cu.getLineNumber(m.getStartPosition() + m.getLength());
                    int loc = end - start + 1;
                    linesPerMethod.put(className + "." + m.getName(), loc);
                    // Q13 : nombre max de paramètres
                    int nbParams = m.parameters().size();
                    if (nbParams > maxParams) {
                        maxParams = nbParams;
                    }
                }

                // ---- Attributs ----
                VariableDeclarationFragmentVisitor varVisitor = new VariableDeclarationFragmentVisitor();
                type.accept(varVisitor);
                int nbAttrs = varVisitor.getVariables().size();
                attributesPerClass.put(className, nbAttrs);
                attributeCount += nbAttrs;

                for (MethodDeclaration m : methodVisitor.getMethods()) {
                    String methodName = className + "." + m.getName();
                    MethodInvocationVisitor invVisitor = new MethodInvocationVisitor();
                    invVisitor.setCurrentMethod(methodName);
                    m.accept(invVisitor);

                    // Ajouter au graphe global
                    Map<String, Set<String>> localGraph = invVisitor.getCallGraph();
                    for (var entry : localGraph.entrySet()) {
                        callGraph.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur de lecture du fichier: " + file.getPath());
        }
    }

    // ========================================================================
    // --- EXERCICE 2 - PART 1: HIERARCHICAL CLUSTERING ---
    // ========================================================================

    public ClusterNode buildDendrogram() {
        if (couplingMatrix == null) {
            this.couplingMatrix = generateCouplingGraphInternal();
        }

        // 1. Initialize clusters (one for each class)
        List<ClusterNode> activeClusters = new ArrayList<>();
        for (String className : getAllClasses()) {
            activeClusters.add(new ClusterNode(className));
        }

        // 2. Loop until only one cluster remains
        while (activeClusters.size() > 1) {
            ClusterNode c1 = null;
            ClusterNode c2 = null;
            double maxCoupling = -1.0;

            // Find the two "closest" (most coupled) clusters
            for (int i = 0; i < activeClusters.size(); i++) {
                for (int j = i + 1; j < activeClusters.size(); j++) {
                    ClusterNode clusterA = activeClusters.get(i);
                    ClusterNode clusterB = activeClusters.get(j);

                    // Use average-linkage coupling
                    double currentCoupling = calculateClusterCoupling(clusterA, clusterB);

                    if (currentCoupling > maxCoupling) {
                        maxCoupling = currentCoupling;
                        c1 = clusterA;
                        c2 = clusterB;
                    }
                }
            }

            if (c1 == null || maxCoupling <= 0) {
                // No more positive coupling found, stop merging
                break;
            }

            // 3. Merge them
            ClusterNode merged = new ClusterNode(c1, c2, maxCoupling);

            // 4. Remove old clusters, add the new merged cluster
            activeClusters.remove(c1);
            activeClusters.remove(c2);
            activeClusters.add(merged);
        }

        // 5. Return the root of the dendrogram
        // If multiple roots remain (due to no coupling), create a virtual root
        if (activeClusters.size() > 1) {
            ClusterNode virtualRoot = new ClusterNode(activeClusters.get(0), activeClusters.get(1), 0.0);
            for(int i = 2; i < activeClusters.size(); i++) {
                virtualRoot = new ClusterNode(virtualRoot, activeClusters.get(i), 0.0);
            }
            return virtualRoot;
        }

        return activeClusters.isEmpty() ? null : activeClusters.get(0);
    }

    /**
     * Calculates the average-linkage coupling between two clusters.
     */
    private double calculateClusterCoupling(ClusterNode clusterA, ClusterNode clusterB) {
        if (this.couplingMatrix == null) return 0.0;

        double totalCoupling = 0;
        int pairCount = 0;

        for (String classA : clusterA.classes) {
            for (String classB : clusterB.classes) {
                double coupling = 0.0;

                // Check A -> B
                if (couplingMatrix.containsKey(classA) && couplingMatrix.get(classA).containsKey(classB)) {
                    coupling = couplingMatrix.get(classA).get(classB);
                }
                // Check B -> A (since matrix might be symmetrical)
                else if (couplingMatrix.containsKey(classB) && couplingMatrix.get(classB).containsKey(classA)) {
                    coupling = couplingMatrix.get(classB).get(classA);
                }

                totalCoupling += coupling;
                pairCount++;
            }
        }

        return (pairCount == 0) ? 0.0 : totalCoupling / pairCount;
    }


    // ========================================================================
    // --- EXERCICE 2 - PART 2: MODULE IDENTIFICATION ---
    // ========================================================================

    /**
     * Calculates the average coupling *within* a single cluster.
     * Required for the CP threshold check.
     */
    public double calculateInternalCoupling(ClusterNode cluster) {
        if (cluster.classes.size() < 2) {
            return 0.0; // No internal coupling possible
        }
        if (this.couplingMatrix == null) return 0.0;

        double totalCoupling = 0;
        int pairCount = 0;
        List<String> classList = new ArrayList<>(cluster.classes);

        for (int i = 0; i < classList.size(); i++) {
            for (int j = i + 1; j < classList.size(); j++) {
                String classA = classList.get(i);
                String classB = classList.get(j);

                double coupling = 0.0;
                if (couplingMatrix.containsKey(classA) && couplingMatrix.get(classA).containsKey(classB)) {
                    coupling = couplingMatrix.get(classA).get(classB);
                } else if (couplingMatrix.containsKey(classB) && couplingMatrix.get(classB).containsKey(classA)) {
                    coupling = couplingMatrix.get(classB).get(classA);
                }

                totalCoupling += coupling;
                pairCount++;
            }
        }

        return (pairCount == 0) ? 0.0 : totalCoupling / pairCount;
    }

    /**
     * Identifies modules based on the dendrogram and rules.
     */
    public List<ClusterNode> identifyModules(ClusterNode dendrogramRoot, double CP, int M) {
        List<ClusterNode> modules = new ArrayList<>();
        int maxModules = M / 2; // Rule: max M/2 modules

        findModulesRecursive(dendrogramRoot, CP, maxModules, modules);

        return modules;
    }

    /**
     * Recursive helper to find modules.
     */
    private void findModulesRecursive(ClusterNode node, double CP, int maxModules, List<ClusterNode> modules) {
        // Stop if node is null (leaf) or module limit is hit
        if (node == null || modules.size() >= maxModules) {
            return;
        }

        // Stop if this is a leaf node (single class)
        if (node.leftChild == null && node.rightChild == null) {
            return;
        }

        // Rule: Average internal coupling must be > CP
        double internalCoupling = calculateInternalCoupling(node);

        if (internalCoupling > CP) {
            // This node is a cohesive module. Add it and STOP traversing this branch.
            // This respects the "single branch" rule.
            modules.add(node);
        } else {
            // This node is not cohesive enough. Check its children.
            findModulesRecursive(node.leftChild, CP, maxModules, modules);
            findModulesRecursive(node.rightChild, CP, maxModules, modules);
        }
    }

    /**
     * Public method to run the clustering analysis and return results as a String.
     * This can be called from the UI.
     */
    public String runClusteringAnalysis(double couplingThreshold) {
        StringBuilder output = new StringBuilder();
        output.append("=== Analyse de Clustering Hiérarchique ===\n\n");

        ClusterNode dendroRoot = buildDendrogram();

        if (dendroRoot == null) {
            output.append("Impossible de construire le dendrogramme.\n");
            return output.toString();
        }

        output.append("Dendrogramme construit.\n");
        output.append("Racine: ").append(dendroRoot.name).append("\n");

        int M = getAllClasses().size(); // Total number of classes
        List<ClusterNode> modules = identifyModules(dendroRoot, couplingThreshold, M);

        output.append("\n=== Modules Identifiés (Couplage Interne > ").append(String.format("%.4f", couplingThreshold)).append(") ===\n");
        output.append("Nombre total de classes (M): ").append(M).append("\n");
        output.append("Limite de modules (M/2): ").append(M/2).append("\n");
        output.append("Nombre de modules trouvés: ").append(modules.size()).append("\n");

        int i = 1;
        for (ClusterNode module : modules) {
            output.append("\nModule ").append(i).append(": (Couplage Interne: ").append(String.format("%.4f", calculateInternalCoupling(module))).append(")\n");
            output.append("  - Classes: ").append(module.classes).append("\n");
            i++;
        }

        return output.toString();
    }

    // Main
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java Analyzer <project_path>");
            return;
        }
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(args[0], true); // Print results when running standalone
    }

    public int getClassCount() { return classCount; }
    public int getLineCount() { return lineCount; }
    public int getMethodCount() { return methodCount; }
    public int getPackageCount() { return packageCount; }
    public int getAttributeCount() { return attributeCount; }
    public int getMaxParams() { return maxParams; }

    public Map<String,Integer> getMethodsPerClass() { return methodsPerClass; }
    public Map<String,Integer> getAttributesPerClass() { return attributesPerClass; }
    public Map<String,Integer> getLinesPerMethod() { return linesPerMethod; }
}
