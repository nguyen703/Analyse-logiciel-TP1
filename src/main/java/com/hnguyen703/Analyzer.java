package com.hnguyen703;

import java.util.stream.Collectors;

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
