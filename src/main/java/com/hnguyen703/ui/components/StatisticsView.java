package com.hnguyen703.ui.components;

import com.hnguyen703.ui.models.ClassStat;
import com.hnguyen703.ui.models.MethodStat;
import com.hnguyen703.utils.Constants;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Component displaying statistics tables for classes and methods
 */
public class StatisticsView extends VBox {

    private final TableView<ClassStat> tblClasses = new TableView<>();
    private final TableView<MethodStat> tblMethods = new TableView<>();

    public StatisticsView() {
        setupLayout();
        setupTables();
    }

    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(10));

        getChildren().addAll(
            new Label(Constants.CLASSES_TABLE_LABEL),
            tblClasses,
            new Label(Constants.TOP_METHODS_TABLE_LABEL),
            tblMethods
        );
    }

    private void setupTables() {
        setupClassesTable();
        setupMethodsTable();
    }

    private void setupClassesTable() {
        TableColumn<ClassStat, String> nameColumn = new TableColumn<>(Constants.CLASS_COLUMN_HEADER);
        nameColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().name));

        TableColumn<ClassStat, Integer> methodsColumn = new TableColumn<>(Constants.METHODS_COLUMN_HEADER);
        methodsColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().methods));

        TableColumn<ClassStat, Integer> attributesColumn = new TableColumn<>(Constants.ATTRIBUTES_COLUMN_HEADER);
        attributesColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().attributes));

        tblClasses.getColumns().add(nameColumn);
        tblClasses.getColumns().add(methodsColumn);
        tblClasses.getColumns().add(attributesColumn);
        tblClasses.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupMethodsTable() {
        TableColumn<MethodStat, String> qualifiedNameColumn = new TableColumn<>(Constants.QUALIFIED_METHOD_COLUMN_HEADER);
        qualifiedNameColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().qualifiedName));

        TableColumn<MethodStat, Integer> locColumn = new TableColumn<>(Constants.LOC_COLUMN_HEADER);
        locColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().loc));

        tblMethods.getColumns().add(qualifiedNameColumn);
        tblMethods.getColumns().add(locColumn);
        tblMethods.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public void updateClassesData(List<ClassStat> classStats) {
        tblClasses.setItems(FXCollections.observableArrayList(classStats));
    }

    public void updateMethodsData(List<MethodStat> methodStats) {
        tblMethods.setItems(FXCollections.observableArrayList(methodStats));
    }

    public void clearData() {
        tblClasses.getItems().clear();
        tblMethods.getItems().clear();
    }
}
