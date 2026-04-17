package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.model.Order;
import org.ecommerce.model.Payment;
import org.ecommerce.service.OrderService;
import org.ecommerce.service.PaymentService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentController {

    // ── Toolbar ───────────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button           deleteBtn;
    @FXML private Button           updateStatusBtn;

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Payment>             paymentTable;
    @FXML private TableColumn<Payment, String>   colId;
    @FXML private TableColumn<Payment, String>   colOrder;
    @FXML private TableColumn<Payment, String>   colMethod;
    @FXML private TableColumn<Payment, String>   colAmount;
    @FXML private TableColumn<Payment, String>   colStatus;
    @FXML private TableColumn<Payment, String>   colTxId;
    @FXML private TableColumn<Payment, String>   colPaidAt;

    // ── Record payment form ───────────────────────────────────────────────────
    @FXML private ComboBox<Order>   orderCombo;
    @FXML private ComboBox<String>  methodCombo;
    @FXML private TextField         amountField;
    @FXML private TextField         txIdField;

    @FXML private Label feedbackLabel;

    private final PaymentService service = new PaymentService();
    private final OrderService   orderService = new OrderService();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(PaymentService.STATUSES));
        methodCombo .setItems(FXCollections.observableArrayList(PaymentService.METHODS));
        methodCombo.setValue("card");

        setupColumns();

        paymentTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                boolean has = sel != null;
                deleteBtn.setDisable(!has);
                updateStatusBtn.setDisable(!has);
            });

        // Load orders for the create form
        try {
            orderCombo.setItems(FXCollections.observableArrayList(orderService.getAll()));
        } catch (SQLException e) {
            feedback("Failed to load orders: " + e.getMessage(), true);
        }

        // Reload orders fresh every time the dropdown opens
        orderCombo.setOnShowing(e -> {
            try {
                Order current = orderCombo.getValue();
                orderCombo.setItems(FXCollections.observableArrayList(orderService.getAll()));
                if (current != null)
                    orderCombo.getItems().stream()
                        .filter(o -> o.getOrderId() == current.getOrderId())
                        .findFirst()
                        .ifPresent(orderCombo::setValue);
            } catch (SQLException ex) {
                feedback("Failed to refresh orders: " + ex.getMessage(), true);
            }
        });

        // Auto-fill amount when an order is selected
        orderCombo.setOnAction(e -> {
            Order sel = orderCombo.getValue();
            if (sel != null && sel.getTotalAmount() != null)
                amountField.setText(sel.getTotalAmount().toPlainString());
        });

        loadAll();
    }

    // ── Columns ───────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId    .setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getPaymentId())));
        colOrder .setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getOrderNumber()));
        colMethod.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colAmount.setCellValueFactory(c -> {
            var a = c.getValue().getAmount();
            return new SimpleStringProperty(a == null ? "" : "$" + String.format("%,.2f", a));
        });
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));
        colTxId  .setCellValueFactory(c -> {
            String tx = c.getValue().getTransactionId();
            return new SimpleStringProperty(tx == null ? "—" : tx);
        });
        colPaidAt.setCellValueFactory(c -> {
            var t = c.getValue().getPaidAt();
            return new SimpleStringProperty(t == null ? "—" : t.format(FMT));
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAll() {
        try {
            paymentTable.setItems(FXCollections.observableArrayList(service.getAll()));
            feedbackLabel.setText("");
        } catch (SQLException e) {
            feedback("Failed to load payments: " + e.getMessage(), true);
        }
    }

    @FXML private void handleSearch() {
        try {
            List<Payment> results = service.search(
                searchField.getText(), statusFilter.getValue());
            paymentTable.setItems(FXCollections.observableArrayList(results));
        } catch (SQLException e) {
            feedback("Search failed: " + e.getMessage(), true);
        }
    }

    @FXML private void handleClear() {
        searchField.clear();
        statusFilter.setValue(null);
        loadAll();
    }

    // ── Record new payment ────────────────────────────────────────────────────

    @FXML
    private void handleRecord() {
        Order order = orderCombo.getValue();
        if (order == null)            { feedback("Select an order.", true); return; }
        String amtText = amountField.getText().trim();
        if (amtText.isEmpty())        { feedback("Amount is required.", true); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                { feedback("Amount must be greater than 0.", true); return; }
        } catch (NumberFormatException e) {
            feedback("Amount must be a valid number.", true); return;
        }

        Payment p = new Payment();
        p.setOrderId(order.getOrderId());
        p.setPaymentMethod(methodCombo.getValue());
        String tx = txIdField.getText().trim();
        p.setTransactionId(tx.isEmpty() ? null : tx);
        p.setAmount(amount);
        p.setStatus("pending");

        try {
            service.record(p);
            clearForm();
            loadAll();
            feedback("Payment recorded (ID " + p.getPaymentId() + ").", false);
        } catch (SQLException e) {
            feedback("Failed to record payment: " + e.getMessage(), true);
        }
    }

    // ── Update status ─────────────────────────────────────────────────────────

    @FXML
    private void handleUpdateStatus() {
        Payment selected = paymentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ChoiceDialog<String> dlg = new ChoiceDialog<>(selected.getStatus(), PaymentService.STATUSES);
        dlg.setTitle("Update Payment Status");
        dlg.setHeaderText("Payment #" + selected.getPaymentId());
        dlg.setContentText("New status:");
        dlg.initOwner(paymentTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(newStatus -> {
            if (newStatus.equals(selected.getStatus())) return;
            try {
                service.updateStatus(selected.getPaymentId(), newStatus);
                // Keep order payment_status in sync
                if ("completed".equals(newStatus))
                    orderService.updatePaymentStatus(selected.getOrderId(), "paid");
                else if ("refunded".equals(newStatus))
                    orderService.updatePaymentStatus(selected.getOrderId(), "refunded");
                loadAll();
                feedback("Status updated to \"" + newStatus + "\".", false);
            } catch (SQLException e) {
                feedback("Update failed: " + e.getMessage(), true);
            }
        });
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @FXML
    private void handleDelete() {
        Payment selected = paymentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete payment #" + selected.getPaymentId() + "?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getPaymentId());
                    loadAll();
                    feedback("Payment deleted.", false);
                } catch (SQLException e) {
                    feedback("Delete failed: " + e.getMessage(), true);
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearForm() {
        orderCombo.setValue(null);
        amountField.clear();
        txIdField.clear();
        methodCombo.setValue("card");
    }

    private void feedback(String msg, boolean error) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(error
            ? "-fx-text-fill: #c0392b; -fx-font-size: 12;"
            : "-fx-text-fill: #27ae60; -fx-font-size: 12;");
    }
}
