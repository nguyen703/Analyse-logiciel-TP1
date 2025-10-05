package com.hnguyen703.ui.components;

import com.hnguyen703.utils.Constants;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

import java.util.Map;
import java.util.Set;

/**
 * Component for displaying the call graph visualization
 */
public class CallGraphView extends StackPane {
    
    private Graph graph;

    public CallGraphView() {
        initializeGraph();
    }
    
    private void initializeGraph() {
        graph = new SingleGraph("CallGraph");
        graph.setAttribute("ui.stylesheet", Constants.GRAPH_STYLESHEET);

        FxViewer viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        
        FxViewPanel panel = (FxViewPanel) viewer.addDefaultView(false);
        getChildren().add(panel);
    }
    
    /**
     * Updates the call graph with new data
     * @param callGraph Map of caller to set of callees
     */
    public void updateCallGraph(Map<String, Set<String>> callGraph) {
        // Clear existing graph
        graph.clear();
        
        if (callGraph == null || callGraph.isEmpty()) {
            return;
        }
        
        // Add nodes and edges
        for (String caller : callGraph.keySet()) {
            if (graph.getNode(caller) == null) {
                graph.addNode(caller).setAttribute("ui.label", caller);
            }
            
            for (String callee : callGraph.get(caller)) {
                if (graph.getNode(callee) == null) {
                    graph.addNode(callee).setAttribute("ui.label", callee);
                }
                
                String edgeId = caller + "->" + callee;
                if (graph.getEdge(edgeId) == null) {
                    graph.addEdge(edgeId, caller, callee, true);
                }
            }
        }
    }
    
    /**
     * Clears the call graph
     */
    public void clearGraph() {
        graph.clear();
    }
}