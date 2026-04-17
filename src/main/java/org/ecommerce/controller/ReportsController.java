package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.service.ReportService;
import org.ecommerce.service.ReportService.SalesSummary;

import java.util.List;

public class ReportsController {

    // ── Summary cards ─────────────────────────────────────────────────────────
    @FXML private Label cardRevenue;
    @FXML private Label cardOrders;
    @FXML private Label cardAvgOrder;
    @FXML private Label cardCustomers;
    @FXML private Label cardLowStock;

    // ── Top products table ────────────────────────────────────────────────────
    @FXML private TableView<Object[]>             topProductsTable;
    @FXML private TableColumn<Object[], String>   colProdName;
    @FXML private TableColumn<Object[], String>   colProdRevenue;
    @FXML private TableColumn<Object[], String>   colProdUnits;

    // ── Order status table ────────────────────────────────────────────────────
    @FXML private TableView<Object[]>             statusTable;
    @FXML private TableColumn<Object[], String>   colStatus;
    @FXML private TableColumn<Object[], String>   colStatusCount;
    @FXML private TableColumn<Object[], String>   colStatusRev;

    // ── Full text report ──────────────────────────────────────────────────────
    @FXML private TextArea reportArea;

    private final ReportService service = new ReportService();

    @FXML
    public void initialize() {
        setupColumns();
        refresh();
    }

    private void setupColumns() {
        // Top products
        colProdName.setCellValueFactory(c ->
            new SimpleStringProperty((String) c.getValue()[0]));
        colProdRevenue.setCellValueFactory(c -> {
            Object v = c.getValue()[1];
            return new SimpleStringProperty(v == null ? "—"
                : String.format("$%,.2f", v));
        });
        colProdUnits.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue()[2])));

        // Order status
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty((String) c.getValue()[0]));
        colStatusCount.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue()[1])));
        colStatusRev.setCellValueFactory(c -> {
            Object v = c.getValue()[2];
            return new SimpleStringProperty(v == null ? "—"
                : String.format("$%,.2f", v));
        });
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private void refresh() {
        reportArea.setText("Loading…");
        cardRevenue.setText("…");
        cardOrders.setText("…");
        cardAvgOrder.setText("…");
        cardCustomers.setText("…");
        cardLowStock.setText("…");

        Task<Void> task = new Task<>() {
            SalesSummary      summary;
            List<Object[]>    topProducts;
            List<Object[]>    statuses;
            String            fullReport;

            @Override
            protected Void call() throws Exception {
                summary     = service.getSalesSummary();
                topProducts = service.getTopProducts(10);
                statuses    = service.getOrdersByStatus();
                fullReport  = service.buildFullReport();
                return null;
            }

            @Override
            protected void succeeded() {
                // Summary cards
                cardRevenue.setText(String.format("$%,.2f", summary.totalRevenue()));
                cardOrders.setText(String.valueOf(summary.totalOrders()));
                cardAvgOrder.setText(String.format("$%,.2f", summary.avgOrderValue()));
                cardCustomers.setText(String.valueOf(summary.totalCustomers()));
                cardLowStock.setText(String.valueOf(summary.lowStockCount()));

                // Top products table
                topProductsTable.setItems(
                    FXCollections.observableArrayList(topProducts));

                // Status table
                statusTable.setItems(
                    FXCollections.observableArrayList(statuses));

                // Full text report
                reportArea.setText(fullReport);
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                cardRevenue.setText("Error");
                reportArea.setText("Failed to load report:\n" + ex.getMessage());
            }
        };

        Thread t = new Thread(task, "reports-loader");
        t.setDaemon(true);
        t.start();
    }
}
