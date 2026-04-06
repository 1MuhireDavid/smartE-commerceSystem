package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.model.User;
import org.example.service.OrderService;
import org.example.service.ProductService;
import org.example.service.UserService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateOrderController {

    // ── Customer selection ────────────────────────────────────────────────────
    @FXML private ComboBox<User>    customerCombo;

    // ── Item entry ────────────────────────────────────────────────────────────
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField         qtyField;
    @FXML private Label             priceLabel;

    // ── Items table ───────────────────────────────────────────────────────────
    @FXML private TableView<OrderItem>             itemsTable;
    @FXML private TableColumn<OrderItem, String>   colProduct;
    @FXML private TableColumn<OrderItem, String>   colQty;
    @FXML private TableColumn<OrderItem, String>   colUnit;
    @FXML private TableColumn<OrderItem, String>   colLine;

    // ── Totals ────────────────────────────────────────────────────────────────
    @FXML private TextField discountField;
    @FXML private Label     subtotalLabel;
    @FXML private Label     totalLabel;

    @FXML private Label     errorLabel;

    private final ObservableList<OrderItem> items = FXCollections.observableArrayList();

    private final OrderService   orderService   = new OrderService();
    private final UserService    userService    = new UserService();
    private final ProductService productService = new ProductService();

    private Runnable onSuccess;

    @FXML
    public void initialize() {
        try {
            setupColumns();
            itemsTable.setItems(items);

            // Load customers and products
            try {
                customerCombo.setItems(FXCollections.observableArrayList(userService.getAll()));
                productCombo.setItems(FXCollections.observableArrayList(productService.getAll()));
            } catch (SQLException e) {
                errorLabel.setText("Failed to load data: " + e.getMessage());
            }

            // When a product is selected, show its price
            productCombo.setOnAction(e -> {
                Product p = productCombo.getValue();
                if (p != null)
                    priceLabel.setText("$" + p.getEffectivePrice().toPlainString());
                else
                    priceLabel.setText("");
            });

            // Recalculate totals whenever items or discount change
            items.addListener((javafx.collections.ListChangeListener<OrderItem>) c -> recalculate());
            discountField.textProperty().addListener((obs, old, val) -> recalculate());
        } catch (Exception ex) {
            ex.printStackTrace();
            if (errorLabel != null)
                errorLabel.setText("Init error: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    public void setOnSuccess(Runnable callback) { this.onSuccess = callback; }

    // ── Add item ──────────────────────────────────────────────────────────────

    @FXML
    private void handleAddItem() {
        Product product = productCombo.getValue();
        if (product == null) { error("Select a product."); return; }
        String qtyText = qtyField.getText().trim();
        if (qtyText.isEmpty()) { error("Enter a quantity."); return; }
        int qty;
        try {
            qty = Integer.parseInt(qtyText);
            if (qty <= 0) { error("Quantity must be at least 1."); return; }
        } catch (NumberFormatException e) {
            error("Quantity must be a whole number."); return;
        }
        if (product.getStockQuantity() < qty) {
            error("Only " + product.getStockQuantity() + " in stock for \"" + product.getName() + "\"."); return;
        }

        // Merge if same product already in list
        for (OrderItem existing : items) {
            if (existing.getProductId() == product.getId()) {
                existing.setQuantity(existing.getQuantity() + qty);
                existing.setTotalPrice(existing.getUnitPrice()
                    .multiply(BigDecimal.valueOf(existing.getQuantity())));
                itemsTable.refresh();
                recalculate();
                clearItemForm();
                return;
            }
        }

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(qty);
        item.setUnitPrice(product.getEffectivePrice());
        item.setTotalPrice(product.getEffectivePrice().multiply(BigDecimal.valueOf(qty)));
        item.setItemStatus("pending");
        items.add(item);
        clearItemForm();
        errorLabel.setText("");
    }

    @FXML
    private void handleRemoveItem() {
        OrderItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) items.remove(selected);
    }

    // ── Submit order ──────────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        User customer = customerCombo.getValue();
        if (customer == null) { error("Select a customer."); return; }
        if (items.isEmpty())  { error("Add at least one item."); return; }

        BigDecimal discount;
        try {
            String dt = discountField.getText().trim();
            discount = dt.isEmpty() ? BigDecimal.ZERO : new BigDecimal(dt);
            if (discount.compareTo(BigDecimal.ZERO) < 0)
                { error("Discount cannot be negative."); return; }
        } catch (NumberFormatException e) {
            error("Discount must be a valid number."); return;
        }

        BigDecimal subtotal = items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        // Check total = subtotal - discount (schema constraint)
        if (discount.compareTo(subtotal) > 0) {
            error("Discount ($" + discount.toPlainString()
                + ") exceeds subtotal ($" + subtotal.toPlainString() + ")."); return;
        }

        Order order = new Order();
        order.setUserId(customer.getUserId());
        order.setOrderNumber(generateOrderNumber());
        order.setStatus("pending");
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setPaymentStatus("unpaid");

        try {
            orderService.create(order, new ArrayList<>(items));
            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (SQLException e) {
            error("Failed to create order: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setupColumns() {
        colProduct.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getProductName()));
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colUnit.setCellValueFactory(c -> {
            var p = c.getValue().getUnitPrice();
            return new SimpleStringProperty(p == null ? "" : "$" + p.toPlainString());
        });
        colLine.setCellValueFactory(c -> {
            var t = c.getValue().getTotalPrice();
            return new SimpleStringProperty(t == null ? "" : "$" + t.toPlainString());
        });
    }

    private void recalculate() {
        BigDecimal subtotal = items.stream()
            .map(i -> i.getTotalPrice() == null ? BigDecimal.ZERO : i.getTotalPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotalLabel.setText("$" + String.format("%,.2f", subtotal));

        BigDecimal discount = BigDecimal.ZERO;
        try {
            String dt = discountField.getText().trim();
            if (!dt.isEmpty()) discount = new BigDecimal(dt);
        } catch (NumberFormatException ignored) {}

        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);
        totalLabel.setText("$" + String.format("%,.2f", total));
    }

    private void clearItemForm() {
        productCombo.setValue(null);
        qtyField.clear();
        priceLabel.setText("");
    }

    private void closeDialog() {
        ((Stage) itemsTable.getScene().getWindow()).close();
    }

    private void error(String msg) {
        errorLabel.setText(msg);
    }

    private static String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "ORD-" + ts + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
