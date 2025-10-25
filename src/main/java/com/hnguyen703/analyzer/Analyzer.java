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

    private final Map<String, Set<String>> callGraph = new HashMap<>();
    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }


    public void analyze(String projectPath) throws IOException {
        analyze(projectPath, true);
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

        try (var pathStream = Files.walk(Paths.get(projectPath))) {
            pathStream
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> parseFile(file.toFile()));
        }

        if (printResults) {
            printResults();
        }
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
     * Generate a weighted coupling graph for all classes in the application
     * Returns a matrix where matrix[classA][classB] = coupling weight between classA and classB
     */
    public Map<String, Map<String, Double>> generateCouplingGraph() {
        Map<String, Map<String, Double>> couplingMatrix = new HashMap<>();

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
                couplingMatrix.put(classA, couplings);
            }
        }

        return couplingMatrix;
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
