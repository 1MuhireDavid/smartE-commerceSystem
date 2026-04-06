package org.example.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.service.PerformanceService;
import org.example.service.PerformanceService.BenchmarkResult;

import java.sql.SQLException;
import java.util.List;

public class PerformanceController {

    @FXML private TextField  keywordField;
    @FXML private TableView<BenchmarkResult>            resultsTable;
    @FXML private TableColumn<BenchmarkResult, String>  colLabel;
    @FXML private TableColumn<BenchmarkResult, Long>    colTotal;
    @FXML private TableColumn<BenchmarkResult, Double>  colAvg;
    @FXML private TableColumn<BenchmarkResult, Long>    colCount;
    @FXML private TextArea   reportArea;
    @FXML private Label      feedbackLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final PerformanceService service = new PerformanceService();

    @FXML
    public void initialize() {
        colLabel.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().label()));
        colTotal.setCellValueFactory(c ->
            new SimpleLongProperty(c.getValue().totalMs()).asObject());
        colAvg.setCellValueFactory(c ->
            new SimpleDoubleProperty(
                Math.round(c.getValue().avgMs() * 1000.0) / 1000.0).asObject());
        colCount.setCellValueFactory(c ->
            new SimpleLongProperty(c.getValue().resultCount()).asObject());

        keywordField.setText("Samsung");
        reportArea.setText("Press \"Run Benchmark\" to start.");
    }

    @FXML
    private void handleRunBenchmark() {
        String keyword = keywordField.getText().isBlank() ? "a" : keywordField.getText().trim();

        progressIndicator.setVisible(true);
        feedbackLabel.setText("");
        reportArea.setText("Running benchmark… please wait.");

        // Run in a background thread to keep the UI responsive
        Task<List<BenchmarkResult>> task = new Task<>() {
            @Override
            protected List<BenchmarkResult> call() throws SQLException {
                return service.runAll(keyword);
            }
        };

        task.setOnSucceeded(e -> {
            List<BenchmarkResult> results = task.getValue();
            resultsTable.setItems(FXCollections.observableArrayList(results));
            reportArea.setText(PerformanceService.summarise(results));
            progressIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            feedbackLabel.setText("Benchmark failed: " + ex.getMessage());
            reportArea.setText("Error: " + ex.getMessage());
            progressIndicator.setVisible(false);
        });

        new Thread(task, "benchmark-thread").start();
    }
}
