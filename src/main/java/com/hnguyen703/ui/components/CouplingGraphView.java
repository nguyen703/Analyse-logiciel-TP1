package com.hnguyen703.ui.components;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

import java.util.*;

/**
 * Component for displaying the weighted coupling graph between classes
 */
public class CouplingGraphView extends StackPane {

    private Graph graph;
    private TextArea outputArea;
    private SplitPane splitPane;

    public CouplingGraphView() {
        initializeGraph();
    }

    private void initializeGraph() {
        graph = new SingleGraph("CouplingGraph");

        // Enhanced stylesheet for weighted coupling graph
        String stylesheet =
            "node {" +
            "   fill-color: #4A90E2;" +
            "   size: 30px;" +
            "   text-size: 14px;" +
            "   text-alignment: center;" +
            "   text-background-mode: rounded-box;" +
            "   text-background-color: rgba(255, 255, 255, 200);" +
            "   text-padding: 5px;" +
            "   stroke-mode: plain;" +
            "   stroke-color: #2E5C8A;" +
            "   stroke-width: 2px;" +
            "}" +
            "edge {" +
            "   fill-color: #7B68EE;" +
            "   arrow-size: 10px, 5px;" +
            "   text-size: 12px;" +
            "   text-color: #333;" +
            "   text-background-mode: rounded-box;" +
            "   text-background-color: rgba(255, 255, 255, 220);" +
            "   text-padding: 3px;" +
            "}" +
            "edge.strong {" +
            "   fill-color: #FF4444;" +
            "   size: 3px;" +
            "}" +
            "edge.medium {" +
            "   fill-color: #FFA500;" +
            "   size: 2px;" +
            "}" +
            "edge.weak {" +
            "   fill-color: #90EE90;" +
            "   size: 1px;" +
            "}";

        graph.setAttribute("ui.stylesheet", stylesheet);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        FxViewer viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();

        FxViewPanel panel = (FxViewPanel) viewer.addDefaultView(false);

        // Create text area for output
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        outputArea.setText("Analyse en attente...\n\nSélectionnez un projet et cliquez sur 'Analyser' pour générer le graphe de couplage.");

        // Create split pane with graph on left and output on right
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(panel, outputArea);
        splitPane.setDividerPositions(0.6); // 60% for graph, 40% for text

        getChildren().add(splitPane);
    }

    /**
     * Updates the coupling graph with weighted edges
     * @param couplingMatrix Map of class pairs to coupling weights
     */
    public void updateCouplingGraph(Map<String, Map<String, Double>> couplingMatrix) {
        // Clear existing graph
        graph.clear();

        if (couplingMatrix == null || couplingMatrix.isEmpty()) {
            outputArea.setText("Aucun couplage détecté entre les classes.");
            return;
        }

        // Build the text output
        StringBuilder output = new StringBuilder();
        output.append("=== Graphe de couplage pondéré ===\n\n");
        output.append("Format: Classe A <-> Classe B : Poids de couplage (pourcentage)\n\n");
        output.append("Légende des couleurs:\n");
        output.append("  🔴 Rouge (forte)   : Couplage > 10%\n");
        output.append("  🟠 Orange (moyenne) : Couplage entre 5% et 10%\n");
        output.append("  🟢 Vert (faible)   : Couplage < 5%\n\n");
        output.append("─────────────────────────────────────────────────\n\n");

        // Sort by class name for consistent output
        List<String> sortedClasses = new ArrayList<>(couplingMatrix.keySet());
        Collections.sort(sortedClasses);

        int totalEdges = 0;
        List<String> couplingDetails = new ArrayList<>();

        // Add nodes and weighted edges
        for (Map.Entry<String, Map<String, Double>> entry : couplingMatrix.entrySet()) {
            String classA = entry.getKey();

            // Add node for class A if not exists
            if (graph.getNode(classA) == null) {
                graph.addNode(classA).setAttribute("ui.label", classA);
            }

            // Sort couplings by weight (descending)
            List<Map.Entry<String, Double>> sortedCouplings = new ArrayList<>(entry.getValue().entrySet());
            sortedCouplings.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> coupling : sortedCouplings) {
                String classB = coupling.getKey();
                double weight = coupling.getValue();

                // Skip if no coupling
                if (weight == 0.0) {
                    continue;
                }

                // Add node for class B if not exists
                if (graph.getNode(classB) == null) {
                    graph.addNode(classB).setAttribute("ui.label", classB);
                }

                // Create edge with weight
                String edgeId = classA + "<->" + classB;
                String reverseEdgeId = classB + "<->" + classA;

                // Avoid duplicate edges (undirected graph)
                if (graph.getEdge(edgeId) == null && graph.getEdge(reverseEdgeId) == null) {
                    var edge = graph.addEdge(edgeId, classA, classB, false);

                    // Format weight as percentage
                    String weightLabel = String.format("%.2f%%", weight * 100);
                    edge.setAttribute("ui.label", weightLabel);
                    edge.setAttribute("weight", weight);

                    // Style edge based on coupling strength
                    String strengthIndicator;
                    if (weight > 0.1) {
                        edge.setAttribute("ui.class", "strong");
                        strengthIndicator = "🔴";
                    } else if (weight > 0.05) {
                        edge.setAttribute("ui.class", "medium");
                        strengthIndicator = "🟠";
                    } else {
                        edge.setAttribute("ui.class", "weak");
                        strengthIndicator = "🟢";
                    }

                    // Add to output text
                    couplingDetails.add(String.format("%s  %s <-> %s : %.4f (%.2f%%)",
                        strengthIndicator, classA, classB, weight, weight * 100));
                    totalEdges++;
                }
            }
        }

        // Sort coupling details by weight (extract from string) for better readability
        couplingDetails.sort((a, b) -> {
            double weightA = Double.parseDouble(a.substring(a.lastIndexOf(":") + 2, a.lastIndexOf("(") - 1));
            double weightB = Double.parseDouble(b.substring(b.lastIndexOf(":") + 2, b.lastIndexOf("(") - 1));
            return Double.compare(weightB, weightA);
        });

        // Append all coupling details
        for (String detail : couplingDetails) {
            output.append(detail).append("\n");
        }

        // Summary statistics
        output.append("\n─────────────────────────────────────────────────\n");
        output.append(String.format("\n📊 Statistiques:\n"));
        output.append(String.format("   • Nombre total de relations de couplage: %d\n", totalEdges));
        output.append(String.format("   • Nombre de classes avec couplage: %d\n", couplingMatrix.size()));

        // Calculate average coupling
        double totalWeight = couplingDetails.stream()
            .mapToDouble(s -> Double.parseDouble(s.substring(s.lastIndexOf(":") + 2, s.lastIndexOf("(") - 1)))
            .sum();
        double avgCoupling = totalEdges > 0 ? totalWeight / totalEdges : 0.0;
        output.append(String.format("   • Couplage moyen: %.4f (%.2f%%)\n", avgCoupling, avgCoupling * 100));

        // Find strongest coupling
        if (!couplingDetails.isEmpty()) {
            String strongest = couplingDetails.get(0);
            output.append(String.format("   • Couplage le plus fort: %s\n",
                strongest.substring(strongest.indexOf("<->"))));
        }

        // Update the text area
        outputArea.setText(output.toString());
    }

    /**
     * Clears the coupling graph
     */
    public void clearGraph() {
        graph.clear();
    }
}

