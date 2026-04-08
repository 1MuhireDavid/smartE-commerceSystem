package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.ecommerce.model.Order;
import org.ecommerce.service.OrderService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrdersController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> colId;
    @FXML private TableColumn<Order, String> colNumber;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colPayment;
    @FXML private TableColumn<Order, String> colDate;
    @FXML private Button viewBtn;
    @FXML private Button updateStatusBtn;
    @FXML private Button deleteBtn;
    @FXML private Label  feedbackLabel;

    private final OrderService service = new OrderService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(OrderService.ORDER_STATUSES));
        setupColumns();
        orderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean has = sel != null;
                viewBtn.setDisable(!has);
                updateStatusBtn.setDisable(!has);
                deleteBtn.setDisable(!has);
            });
        loadData(null, null);
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getOrderId())));
        colNumber.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getOrderNumber()));
        colCustomer.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUserFullName()));
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                String color = switch (status) {
                    case "processing" -> "#2980b9";
                    case "shipped"    -> "#8e44ad";
                    case "delivered"  -> "#27ae60";
                    case "cancelled"  -> "#e74c3c";
                    default           -> "#f39c12";
                };
                setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                       + "-fx-background-radius:4;-fx-padding:2 8;");
            }
        });
        colTotal.setCellValueFactory(c -> {
            var amt = c.getValue().getTotalAmount();
            return new SimpleStringProperty(amt == null ? "—" : String.format("%,.2f", amt));
        });
        colPayment.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getPaymentStatus()));
        colPayment.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                String color = switch (status) {
                    case "paid"     -> "#27ae60";
                    case "refunded" -> "#8e44ad";
                    default         -> "#f39c12";
                };
                setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                       + "-fx-background-radius:4;-fx-padding:2 8;");
            }
        });
        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getOrderedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(FMT));
        });
    }

    private void loadData(String keyword, String status) {
        try {
            List<Order> orders = service.search(keyword, status);
            orderTable.setItems(FXCollections.observableArrayList(orders));
            clearFeedback();
        } catch (SQLException e) {
            showError("DB error: " + e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        loadData(searchField.getText(), statusFilter.getValue());
    }

    @FXML private void handleClear() {
        searchField.clear();
        statusFilter.setValue(null);
        loadData(null, null);
    }

    @FXML
    private void handleView() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            // Load full order with items
            Order full = service.getById(selected.getOrderId());
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/order_detail_view.fxml"));
            Scene scene = new Scene(loader.load());
            OrderDetailController ctrl = loader.getController();
            ctrl.setOrder(full);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(orderTable.getScene().getWindow());
            stage.setTitle("Order — " + full.getOrderNumber());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (SQLException e) {
            showError("Could not load order: " + e.getMessage());
        } catch (IOException e) {
            showError("Could not open detail view: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateStatus() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ChoiceDialog<String> dlg = new ChoiceDialog<>(selected.getStatus(), OrderService.ORDER_STATUSES);
        dlg.setTitle("Update Order Status");
        dlg.setHeaderText("Order: " + selected.getOrderNumber());
        dlg.setContentText("New status:");
        dlg.initOwner(orderTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(newStatus -> {
            if (newStatus.equals(selected.getStatus())) return;
            try {
                service.updateStatus(selected.getOrderId(), newStatus);
                showSuccess("Status updated to \"" + newStatus + "\".");
                loadData(searchField.getText(), statusFilter.getValue());
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleCreateOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/create_order_dialog.fxml"));
            Scene scene = new Scene(loader.load());
            CreateOrderController ctrl = loader.getController();
            ctrl.setOnSuccess(() -> loadData(null, null));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(orderTable.getScene().getWindow());
            stage.setTitle("Create New Order");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            showError("Could not open create order dialog: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete order \"" + selected.getOrderNumber() + "\" and all its items?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getOrderId());
                    showSuccess("Order deleted.");
                    loadData(searchField.getText(), statusFilter.getValue());
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #27ae60;");
        feedbackLabel.setText(msg);
    }
    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b;");
        feedbackLabel.setText(msg);
    }
    private void clearFeedback() { feedbackLabel.setText(""); }
}
