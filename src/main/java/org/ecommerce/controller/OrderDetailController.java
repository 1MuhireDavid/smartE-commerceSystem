package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.model.Order;
import org.ecommerce.model.OrderItem;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderDetailController {

    @FXML private Label titleLabel;
    @FXML private Label lblCustomer;
    @FXML private Label lblOrderNumber;
    @FXML private Label lblStatus;
    @FXML private Label lblPayment;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTotal;
    @FXML private Label lblOrderedAt;

    @FXML private TableView<OrderItem>             itemsTable;
    @FXML private TableColumn<OrderItem, String>   colProduct;
    @FXML private TableColumn<OrderItem, String>   colQty;
    @FXML private TableColumn<OrderItem, String>   colUnitPrice;
    @FXML private TableColumn<OrderItem, String>   colLineTotal;
    @FXML private TableColumn<OrderItem, String>   colItemStatus;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colProduct.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getProductName()));
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colUnitPrice.setCellValueFactory(c -> {
            var p = c.getValue().getUnitPrice();
            return new SimpleStringProperty(p == null ? "—" : String.format("$%,.2f", p));
        });
        colLineTotal.setCellValueFactory(c -> {
            var t = c.getValue().getTotalPrice();
            return new SimpleStringProperty(t == null ? "—" : String.format("$%,.2f", t));
        });
        colItemStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemStatus()));
    }

    /** Populate the view with the given order (must include items list). */
    public void setOrder(Order order) {
        titleLabel.setText("Order Details — " + order.getOrderNumber());
        lblCustomer.setText(order.getUserFullName());
        lblOrderNumber.setText(order.getOrderNumber());
        lblStatus.setText(order.getStatus());
        lblPayment.setText(order.getPaymentStatus());
        lblSubtotal.setText(order.getSubtotal() == null ? "—"
            : String.format("$%,.2f", order.getSubtotal()));
        lblDiscount.setText(order.getDiscountAmount() == null ? "—"
            : String.format("$%,.2f", order.getDiscountAmount()));
        lblTotal.setText(order.getTotalAmount() == null ? "—"
            : String.format("$%,.2f", order.getTotalAmount()));
        lblOrderedAt.setText(order.getOrderedAt() == null ? "—"
            : order.getOrderedAt().format(FMT));

        List<OrderItem> items = order.getItems();
        if (items != null) {
            itemsTable.setItems(FXCollections.observableArrayList(items));
        }
    }
}
