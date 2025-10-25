package com.hnguyen703.analyzer;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.code.CtInvocation; // Corrected import
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.stream.Collectors;

public class SpoonAnalyzer {

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
    private Set<String> projectClasses = new HashSet<>();
    private Map<String, Map<String, Double>> couplingMatrix;

    // ========================================================================
    // --- CLUSTER NODE INNER CLASS ---
    // ========================================================================
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
    // ========================================================================
    // --- END OF INNER CLASS ---
    // ========================================================================


    /**
     * Analyzes a project using Spoon and builds the call graph.
     */
    public void analyze(String projectPath) {
        analyze(projectPath, false);
    }

    /**
     * Analyzes a project using Spoon and builds the call graph.
     */
    public void analyze(String projectPath, boolean printResults) {
        // Reset counters
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
        projectClasses.clear();
        couplingMatrix = null;

        // 1. Configure Spoon
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath); // Path to the source code
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        // 2. Get all classes defined in the project
        List<CtClass<?>> allClasses = model.getElements(new TypeFilter<>(CtClass.class));
        projectClasses = allClasses.stream()
                .map(CtClass::getQualifiedName)
                .collect(Collectors.toSet());

        classCount = allClasses.size();

        // 3. Collect statistics for each class
        for (CtClass<?> ctClass : allClasses) {
            String className = ctClass.getQualifiedName();

            // Count methods
            Set<CtMethod<?>> methods = ctClass.getMethods();
            methodsPerClass.put(className, methods.size());
            methodCount += methods.size();

            // Count attributes (fields)
            int fieldCount = ctClass.getFields().size();
            attributesPerClass.put(className, fieldCount);
            attributeCount += fieldCount;

            // Process each method
            for (CtMethod<?> method : methods) {
                // Count parameters
                int paramCount = method.getParameters().size();
                if (paramCount > maxParams) {
                    maxParams = paramCount;
                }

                // Count lines of code for method
                if (method.getPosition() != null && method.getPosition().isValidPosition()) {
                    int startLine = method.getPosition().getLine();
                    int endLine = method.getPosition().getEndLine();
                    int loc = endLine - startLine + 1;
                    linesPerMethod.put(className + "." + method.getSimpleName(), loc);
                    lineCount += loc;
                }
            }
        }

        // Count packages
        packageCount = model.getAllPackages().size();

        // 4. Find all method invocations (calls)
        for (CtInvocation<?> invocation : model.getElements(new TypeFilter<>(CtInvocation.class))) {

            // 5. Get Caller (the method/class containing the call)
            CtMethod<?> callerMethod = invocation.getParent(CtMethod.class);
            CtClass<?> callerClass = invocation.getParent(CtClass.class);

            if (callerMethod == null || callerClass == null) {
                continue; // Skip calls not inside a method/class
            }

            // 6. Get Callee (the method/class being called)
            CtMethod<?> calleeMethod = null;
            try {
                // getDeclaration() finds the method definition in the model
                if (invocation.getExecutable().getDeclaration() instanceof CtMethod) {
                    calleeMethod = (CtMethod<?>) invocation.getExecutable().getDeclaration();
                }
            } catch (Exception e) {
                // Callee is not in the model (e.g., java.lang.String)
                continue;
            }

            if (calleeMethod == null) {
                continue; // Callee not found or is not a method
            }

            CtClass<?> calleeClass = calleeMethod.getParent(CtClass.class);
            if (calleeClass == null) {
                continue;
            }

            // 7. Filter: Only keep calls between classes in the project
            if (projectClasses.contains(callerClass.getQualifiedName()) &&
                    projectClasses.contains(calleeClass.getQualifiedName())) {

                String callerSignature = callerClass.getQualifiedName() + "." + callerMethod.getSimpleName();
                String calleeSignature = calleeClass.getQualifiedName() + "." + calleeMethod.getSimpleName();

                // 8. Build the call graph
                callGraph.computeIfAbsent(callerSignature, k -> new HashSet<>()).add(calleeSignature);
            }
        }

        // 9. Once analysis is done, generate the coupling matrix
        this.couplingMatrix = generateCouplingGraphInternal();
    }

    // ========================================================================
    // --- EXERCICE 1: COUPLING (from JDT Analyzer) ---
    // ========================================================================

    public Map<String, Map<String, Double>> getCouplingMatrix() {
        return couplingMatrix;
    }

    private int getTotalRelations() {
        int total = 0;
        for (String callerSignature : callGraph.keySet()) {
            String callerClass = extractClassName(callerSignature);

            for (String calleeSignature : callGraph.get(callerSignature)) {
                String calleeClass = extractClassName(calleeSignature);

                // Only count relations between different project classes
                if (!callerClass.equals(calleeClass)) {
                    total++;
                }
            }
        }
        return total;
    }

    private int getRelationsBetweenAB(String classA, String classB) {
        int count = 0;
        for (String callerSignature : callGraph.keySet()) {
            String callerClass = extractClassName(callerSignature);

            for (String calleeSignature : callGraph.get(callerSignature)) {
                String calleeClass = extractClassName(calleeSignature);

                if ((callerClass.equals(classA) && calleeClass.equals(classB)) ||
                        (callerClass.equals(classB) && calleeClass.equals(classA))) {
                    count++;
                }
            }
        }
        return count;
    }

    // Helper to get class from "com.example.MyClass.myMethod"
    private String extractClassName(String methodSignature) {
        if (methodSignature == null || !methodSignature.contains(".")) {
            return "";
        }
        return methodSignature.substring(0, methodSignature.lastIndexOf("."));
    }

    private double calculateCoupling(String classA, String classB) {
        int relationsBetweenAB = getRelationsBetweenAB(classA, classB);
        int totalRelations = getTotalRelations();
        return (totalRelations == 0) ? 0.0 : (double) relationsBetweenAB / totalRelations;
    }

    // Helper method for clustering
    private Set<String> getAllClasses() {
        return this.projectClasses;
    }

    private Map<String, Map<String, Double>> generateCouplingGraphInternal() {
        Map<String, Map<String, Double>> matrix = new HashMap<>();
        for (String classA : projectClasses) {
            Map<String, Double> couplings = new HashMap<>();
            for (String classB : projectClasses) {
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


    // ========================================================================
    // --- EXERCICE 2 - PART 1: HIERARCHICAL CLUSTERING (from JDT Analyzer) ---
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

                if (couplingMatrix.containsKey(classA) && couplingMatrix.get(classA).containsKey(classB)) {
                    coupling = couplingMatrix.get(classA).get(classB);
                }
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
    // --- EXERCICE 2 - PART 2: MODULE IDENTIFICATION (from JDT Analyzer) ---
    // ========================================================================

    /**
     * Calculates the average coupling *within* a single cluster.
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
        if (node == null || modules.size() >= maxModules) {
            return;
        }

        if (node.leftChild == null && node.rightChild == null) {
            return;
        }

        double internalCoupling = calculateInternalCoupling(node);

        if (internalCoupling > CP) {
            modules.add(node);
        } else {
            findModulesRecursive(node.leftChild, CP, maxModules, modules);
            findModulesRecursive(node.rightChild, CP, maxModules, modules);
        }
    }

    /**
     * Public method to run the clustering analysis and return results as a String.
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

    // ========================================================================
    // --- GETTER METHODS FOR UI ---
    // ========================================================================

    public int getClassCount() { return classCount; }
    public int getLineCount() { return lineCount; }
    public int getMethodCount() { return methodCount; }
    public int getPackageCount() { return packageCount; }
    public int getAttributeCount() { return attributeCount; }
    public int getMaxParams() { return maxParams; }

    public Map<String, Integer> getMethodsPerClass() { return methodsPerClass; }
    public Map<String, Integer> getAttributesPerClass() { return attributesPerClass; }
    public Map<String, Integer> getLinesPerMethod() { return linesPerMethod; }

    public Map<String, Set<String>> getCallGraph() { return callGraph; }

    public Map<String, Map<String, Double>> generateCouplingGraph() {
        if (this.couplingMatrix == null) {
            this.couplingMatrix = generateCouplingGraphInternal();
        }
        return this.couplingMatrix;
    }
}