package com.hnguyen703;

import com.hnguyen703.analyzer.Analyzer;
import com.hnguyen703.ui.components.CallGraphView;
import com.hnguyen703.ui.components.StatisticsView;
import com.hnguyen703.ui.components.SummaryPanel;
import com.hnguyen703.ui.models.ClassStat;
import com.hnguyen703.ui.models.MethodStat;
import com.hnguyen703.utils.Constants;
import com.hnguyen703.utils.FileUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyzerUI extends Application {

    // UI Components
    private final SummaryPanel summaryPanel = new SummaryPanel();
    private final StatisticsView statisticsView = new StatisticsView();
    private final CallGraphView callGraphView = new CallGraphView();

    // Loading indicator
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    private Analyzer currentAnalyzer;

    @Override
    public void start(Stage stage) {
        // Top bar : chemin + boutons
        TextField pathField = new TextField();
        pathField.setPromptText(Constants.PATH_PROMPT_TEXT);
        pathField.setMinWidth(300);
        Button browseBtn = new Button(Constants.BROWSE_BUTTON_TEXT);
        Button runBtn = new Button(Constants.ANALYZE_BUTTON_TEXT);

        // Configure loading indicator
        loadingIndicator.setMaxSize(20, 20);
        loadingIndicator.setVisible(false);

        HBox top = new HBox(8, new Label(Constants.PROJECT_FOLDER_LABEL), pathField, browseBtn, runBtn, loadingIndicator);
        top.setPadding(new Insets(10));
        top.setAlignment(Pos.CENTER_LEFT);

        // Actions
        browseBtn.setOnAction(ev -> {
            File selectedFile = FileUtils.chooseDirectory(stage, "Choisir le dossier projet");
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
            }
        });

        runBtn.setOnAction(ev -> analyzeAndRender(pathField.getText()));

        // Onglet 1 : Statistiques
        VBox statsRoot = new VBox(10, top, new Separator(), summaryPanel, statisticsView);
        statsRoot.setPadding(new Insets(10));
        Tab tabStats = new Tab(Constants.STATISTICS_TAB, statsRoot);
        tabStats.setClosable(false);

        // Onglet 2 : Call Graph
        VBox graphRoot = new VBox(10, top.getChildren().get(0), callGraphView);
        graphRoot.setPadding(new Insets(10));
        Tab tabGraph = new Tab(Constants.CALL_GRAPH_TAB, graphRoot);
        tabGraph.setClosable(false);

        // TabPane principal
        TabPane tabs = new TabPane(tabStats, tabGraph);

        Scene scene = new Scene(tabs, Constants.APP_WIDTH, Constants.APP_HEIGHT);
        stage.setScene(scene);
        stage.setTitle(Constants.APP_TITLE);
        stage.show();
    }

    private void analyzeAndRender(String projectPath) {
        if (!FileUtils.isValidPath(projectPath)) {
            new Alert(Alert.AlertType.WARNING, Constants.SELECT_PROJECT_FOLDER_ERROR).showAndWait();
            return;
        }

        loadingIndicator.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Analyzer analyzer = new Analyzer();
                    analyzer.analyze(projectPath, false);
                    currentAnalyzer = analyzer;

                    Platform.runLater(() -> {
                        updateUI(analyzer);
                        loadingIndicator.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        new Alert(Alert.AlertType.ERROR, Constants.ANALYSIS_ERROR_PREFIX + ex.getMessage()).showAndWait();
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updateUI(Analyzer analyzer) {
        // Update summary panel
        summaryPanel.updateSummary(
            analyzer.getClassCount(),
            analyzer.getLineCount(),
            analyzer.getMethodCount(),
            analyzer.getPackageCount(),
            analyzer.getAttributeCount(),
            analyzer.getMaxParams()
        );

        // Update statistics view
        updateStatisticsView(analyzer);

        // Update call graph
        callGraphView.updateCallGraph(analyzer.getCallGraph());
    }

    private void updateStatisticsView(Analyzer analyzer) {
        // Classes (méthodes / attributs)
        Map<String, Integer> methodsPerClass = analyzer.getMethodsPerClass();
        Map<String, Integer> attributesPerClass = analyzer.getAttributesPerClass();

        List<ClassStat> classStats = methodsPerClass.keySet().stream()
            .map(className -> new ClassStat(
                className,
                methodsPerClass.getOrDefault(className, 0),
                attributesPerClass.getOrDefault(className, 0)
            ))
            .sorted(Comparator.comparingInt((ClassStat cs) -> cs.methods).reversed())
            .collect(Collectors.toList());

        statisticsView.updateClassesData(classStats);

        // Top 10% méthodes par LOC
        Map<String, Integer> linesPerMethod = analyzer.getLinesPerMethod();
        int topN = Math.max(1, (int) Math.ceil(linesPerMethod.size() * 0.1));

        List<MethodStat> methodStats = linesPerMethod.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(topN)
            .map(entry -> new MethodStat(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        statisticsView.updateMethodsData(methodStats);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
