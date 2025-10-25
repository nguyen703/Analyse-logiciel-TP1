package com.hnguyen703;

import com.hnguyen703.analyzer.SpoonAnalyzer;
import com.hnguyen703.ui.components.CallGraphView;
import com.hnguyen703.ui.components.CouplingGraphView;
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

public class AnalyzerSpoonUI extends Application {

    private final SummaryPanel summaryPanel = new SummaryPanel();
    private final StatisticsView statisticsView = new StatisticsView();
    private final CallGraphView callGraphView = new CallGraphView();
    private final CouplingGraphView couplingGraphView = new CouplingGraphView();

    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    @Override
    public void stop() throws Exception {
        super.stop();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void start(Stage stage) {
        TextField pathField = new TextField();
        pathField.setPromptText(Constants.PATH_PROMPT_TEXT);
        pathField.setMinWidth(300);
        Button browseBtn = new Button(Constants.BROWSE_BUTTON_TEXT);
        Button runBtn = new Button(Constants.ANALYZE_BUTTON_TEXT);

        loadingIndicator.setMaxSize(20, 20);
        loadingIndicator.setVisible(false);

        HBox top = new HBox(8, new Label(Constants.PROJECT_FOLDER_LABEL), pathField, browseBtn, runBtn, loadingIndicator);
        top.setPadding(new Insets(10));
        top.setAlignment(Pos.CENTER_LEFT);

        browseBtn.setOnAction(ev -> {
            File selectedFile = FileUtils.chooseDirectory(stage, "Choisir le dossier projet");
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
            }
        });

        runBtn.setOnAction(ev -> analyzeAndRender(pathField.getText()));

        VBox statsRoot = new VBox(10, top, new Separator(), summaryPanel, statisticsView);
        statsRoot.setPadding(new Insets(10));
        Tab tabStats = new Tab(Constants.STATISTICS_TAB, statsRoot);
        tabStats.setClosable(false);

        VBox graphRoot = new VBox(10, top.getChildren().get(0), callGraphView);
        graphRoot.setPadding(new Insets(10));
        Tab tabGraph = new Tab(Constants.CALL_GRAPH_TAB, graphRoot);
        tabGraph.setClosable(false);

        VBox couplingRoot = new VBox(10, new Label("Graphe de couplage pondéré entre les classes"), couplingGraphView);
        couplingRoot.setPadding(new Insets(10));
        Tab tabCoupling = new Tab(Constants.COUPLING_GRAPH_TAB, couplingRoot);
        tabCoupling.setClosable(false);

        TabPane tabs = new TabPane(tabStats, tabGraph, tabCoupling);

        Scene scene = new Scene(tabs, Constants.APP_WIDTH, Constants.APP_HEIGHT);
        stage.setScene(scene);
        stage.setTitle(Constants.APP_TITLE + " (Spoon)");
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
            protected Void call() {
                try {
                    SpoonAnalyzer analyzer = new SpoonAnalyzer();
                    analyzer.analyze(projectPath, true);

                    Platform.runLater(() -> {
                        updateUI(analyzer);
                        loadingIndicator.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        new Alert(Alert.AlertType.ERROR, Constants.ANALYSIS_ERROR_PREFIX + ex.getMessage()).showAndWait();
                        ex.printStackTrace();
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updateUI(SpoonAnalyzer analyzer) {
        summaryPanel.updateSummary(
                analyzer.getClassCount(),
                analyzer.getLineCount(),
                analyzer.getMethodCount(),
                analyzer.getPackageCount(),
                analyzer.getAttributeCount(),
                analyzer.getMaxParams()
        );

        updateStatisticsView(analyzer);

        callGraphView.updateCallGraph(analyzer.getCallGraph());

        Map<String, Map<String, Double>> couplingMatrix = analyzer.generateCouplingGraph();
        couplingGraphView.updateCouplingGraph(couplingMatrix);
    }


    private void updateStatisticsView(SpoonAnalyzer analyzer) {
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

        Map<String, Integer> linesPerMethod = analyzer.getLinesPerMethod();
        int topN = Math.max(1, (int) Math.ceil(linesPerMethod.size() * 0.1));

        List<MethodStat> methodStats = linesPerMethod.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new MethodStat(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        statisticsView.updateMethodsData(methodStats);
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        launch(args);
    }
}
