package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.ecommerce.model.*;
import org.ecommerce.service.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CartController {

    // ── Cart table ────────────────────────────────────────────────────────────
    @FXML private TableView<Cart>             cartTable;
    @FXML private TableColumn<Cart, String>   colCartId;
    @FXML private TableColumn<Cart, String>   colUser;
    @FXML private TableColumn<Cart, String>   colActive;
    @FXML private TableColumn<Cart, String>   colUpdated;

    // ── Items table ───────────────────────────────────────────────────────────
    @FXML private TableView<CartItem>             itemsTable;
    @FXML private TableColumn<CartItem, String>   colProduct;
    @FXML private TableColumn<CartItem, String>   colQty;
    @FXML private TableColumn<CartItem, String>   colPrice;
    @FXML private TableColumn<CartItem, String>   colLineTotal;

    // ── Toolbar buttons ───────────────────────────────────────────────────────
    @FXML private Button convertBtn;
    @FXML private Button deactivateBtn;
    @FXML private Button deleteCartBtn;
    @FXML private Button removeItemBtn;

    // ── Add-product form ──────────────────────────────────────────────────────
    @FXML private HBox              addProductRow;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField         addQtyField;

    @FXML private Label feedbackLabel;
    @FXML private Label itemsTitle;

    private final CartService    cartService    = new CartService();
    private final UserService    userService    = new UserService();
    private final ProductService productService = new ProductService();
    private final OrderService   orderService   = new OrderService();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupCartColumns();
        setupItemColumns();

        try {
            productCombo.setItems(FXCollections.observableArrayList(productService.getAll()));
        } catch (SQLException e) {
            feedback("Failed to load products: " + e.getMessage(), true);
        }

        cartTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                boolean has    = sel != null;
                boolean active = has && sel.isActive();
                deactivateBtn.setDisable(!active);
                deleteCartBtn.setDisable(!has);
                convertBtn.setDisable(!active);
                addProductRow.setDisable(!active);
                loadItems(sel);
            });

        itemsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> removeItemBtn.setDisable(sel == null));

        loadCarts();
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    private void setupCartColumns() {
        colCartId .setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getCartId())));
        colUser   .setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUserFullName()));
        colActive .setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));
        colUpdated.setCellValueFactory(c -> {
            var t = c.getValue().getUpdatedAt();
            return new SimpleStringProperty(t == null ? "" : t.format(FMT));
        });
    }

    private void setupItemColumns() {
        colProduct  .setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getProductName()));
        colQty      .setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
        colPrice    .setCellValueFactory(c -> {
            var p = c.getValue().getUnitPrice();
            return new SimpleStringProperty(p == null ? "" : " " + p.toPlainString());
        });
        colLineTotal.setCellValueFactory(c ->
            new SimpleStringProperty( String.format("%,.2f", c.getValue().getLineTotal())));
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadCarts() {
        try {
            cartTable.setItems(FXCollections.observableArrayList(cartService.getAll()));
            feedbackLabel.setText("");
        } catch (SQLException e) {
            feedback("Failed to load carts: " + e.getMessage(), true);
        }
    }

    private void loadItems(Cart cart) {
        if (cart == null) {
            itemsTable.setItems(FXCollections.emptyObservableList());
            itemsTitle.setText("Cart Items");
            return;
        }
        itemsTitle.setText("Items in Cart #" + cart.getCartId() + " — " + cart.getUserFullName());
        try {
            itemsTable.setItems(FXCollections.observableArrayList(
                cartService.getItems(cart.getCartId())));
        } catch (SQLException e) {
            feedback("Failed to load items: " + e.getMessage(), true);
        }
    }

    // ── New Cart ──────────────────────────────────────────────────────────────

    @FXML
    private void handleNewCart() {
        List<User> customers;
        try {
            customers = userService.getAll();
        } catch (SQLException e) {
            feedback("Failed to load customers: " + e.getMessage(), true);
            return;
        }
        if (customers.isEmpty()) {
            feedback("No users in the system.", true);
            return;
        }

        ComboBox<User> picker = new ComboBox<>(FXCollections.observableArrayList(customers));
        picker.setPromptText("Select customer");
        picker.setPrefWidth(260);

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("New Shopping Cart");
        dialog.setHeaderText("Choose a customer for the new cart:");
        dialog.getDialogPane().setContent(new HBox(8, new Label("Customer:"), picker));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? picker.getValue() : null);
        dialog.initOwner(cartTable.getScene().getWindow());

        dialog.showAndWait().ifPresent(user -> {
            if (user == null) { feedback("No customer selected.", true); return; }
            try {
                Cart cart = cartService.createCart(user.getUserId());
                loadCarts();
                feedback("Cart #" + cart.getCartId() + " created for " + user.getFullName() + ".", false);
            } catch (SQLException e) {
                feedback("Failed to create cart: " + e.getMessage(), true);
            }
        });
    }

    // ── Add Product to Cart ───────────────────────────────────────────────────

    @FXML
    private void handleAddProduct() {
        Cart cart = cartTable.getSelectionModel().getSelectedItem();
        if (cart == null || !cart.isActive()) {
            feedback("Select an active cart first.", true); return;
        }
        Product product = productCombo.getValue();
        if (product == null) { feedback("Select a product.", true); return; }

        int qty;
        String qtyText = addQtyField.getText().trim();
        try {
            qty = qtyText.isEmpty() ? 1 : Integer.parseInt(qtyText);
            if (qty <= 0) { feedback("Quantity must be at least 1.", true); return; }
        } catch (NumberFormatException e) {
            feedback("Quantity must be a whole number.", true); return;
        }

        if (product.getStockQuantity() < qty) {
            feedback("Only " + product.getStockQuantity() + " in stock for \"" + product.getName() + "\".", true);
            return;
        }

        CartItem item = new CartItem();
        item.setCartId(cart.getCartId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(qty);
        item.setUnitPrice(product.getEffectivePrice());

        try {
            cartService.addItem(item);
            addQtyField.clear();
            productCombo.setValue(null);
            loadItems(cart);
            feedback("Added " + qty + "x \"" + product.getName() + "\" to cart #" + cart.getCartId() + ".", false);
        } catch (SQLException e) {
            feedback("Failed to add item: " + e.getMessage(), true);
        }
    }

    // ── Convert Cart to Order ─────────────────────────────────────────────────

    @FXML
    private void handleConvertToOrder() {
        Cart cart = cartTable.getSelectionModel().getSelectedItem();
        if (cart == null || !cart.isActive()) return;

        List<CartItem> cartItems;
        try {
            cartItems = cartService.getItems(cart.getCartId());
        } catch (SQLException e) {
            feedback("Failed to read cart items: " + e.getMessage(), true);
            return;
        }
        if (cartItems.isEmpty()) {
            feedback("Cart is empty. Add products before converting.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Convert cart #" + cart.getCartId() + " (" + cart.getUserFullName()
                + ") with " + cartItems.size() + " item(s) into a new order?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Convert to Order");
        confirm.initOwner(cartTable.getScene().getWindow());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                BigDecimal subtotal = cartItems.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Order order = new Order();
                order.setUserId(cart.getUserId());
                order.setOrderNumber(generateOrderNumber());
                order.setStatus("pending");
                order.setSubtotal(subtotal);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subtotal);
                order.setPaymentStatus("unpaid");

                List<OrderItem> orderItems = cartItems.stream().map(ci -> {
                    OrderItem oi = new OrderItem();
                    oi.setProductId(ci.getProductId());
                    oi.setProductName(ci.getProductName());
                    oi.setQuantity(ci.getQuantity());
                    oi.setUnitPrice(ci.getUnitPrice());
                    oi.setTotalPrice(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
                    oi.setItemStatus("pending");
                    return oi;
                }).collect(Collectors.toList());

                Order created = orderService.create(order, orderItems);
                cartService.deactivate(cart.getCartId());
                loadCarts();
                feedback("Order " + created.getOrderNumber() + " created from cart. Cart deactivated.", false);
            } catch (SQLException e) {
                feedback("Conversion failed: " + e.getMessage(), true);
            }
        });
    }

    // ── Deactivate / Delete Cart ──────────────────────────────────────────────

    @FXML
    private void handleDeactivate() {
        Cart sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null || !sel.isActive()) return;
        try {
            cartService.deactivate(sel.getCartId());
            loadCarts();
            feedback("Cart #" + sel.getCartId() + " deactivated.", false);
        } catch (SQLException e) {
            feedback("Failed to deactivate: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeleteCart() {
        Cart sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete cart #" + sel.getCartId() + " and all its items?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    cartService.delete(sel.getCartId());
                    itemsTable.setItems(FXCollections.emptyObservableList());
                    loadCarts();
                    feedback("Cart deleted.", false);
                } catch (SQLException e) {
                    feedback("Delete failed: " + e.getMessage(), true);
                }
            }
        });
    }

    // ── Remove Cart Item ──────────────────────────────────────────────────────

    @FXML
    private void handleRemoveItem() {
        CartItem sel = itemsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            cartService.removeItem(sel.getCartItemId());
            loadItems(cartTable.getSelectionModel().getSelectedItem());
            feedback("Item removed.", false);
        } catch (SQLException e) {
            feedback("Remove failed: " + e.getMessage(), true);
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() { loadCarts(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void feedback(String msg, boolean error) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(error
            ? "-fx-text-fill: #c0392b; -fx-font-size: 12;"
            : "-fx-text-fill: #27ae60; -fx-font-size: 12;");
    }

    private static String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "ORD-" + ts + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
