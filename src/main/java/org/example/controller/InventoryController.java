package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.model.Inventory;
import org.example.service.InventoryService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class InventoryController {

    @FXML private TextField   searchField;
    @FXML private CheckBox    lowStockOnly;
    @FXML private TableView<Inventory>            invTable;
    @FXML private TableColumn<Inventory, String>  colProduct;
    @FXML private TableColumn<Inventory, String>  colCategory;
    @FXML private TableColumn<Inventory, String>  colStock;
    @FXML private TableColumn<Inventory, String>  colReserved;
    @FXML private TableColumn<Inventory, String>  colAvailable;
    @FXML private TableColumn<Inventory, String>  colReorder;
    @FXML private TableColumn<Inventory, String>  colStatus;
    @FXML private TableColumn<Inventory, String>  colUpdated;
    @FXML private Button updateBtn;
    @FXML private Button adjustBtn;
    @FXML private Label  lowStockBanner;
    @FXML private Label  feedbackLabel;

    private final InventoryService service = new InventoryService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupColumns();
        invTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean has = sel != null;
                updateBtn.setDisable(!has);
                adjustBtn.setDisable(!has);
            });
        loadData();
    }

    private void setupColumns() {
        colProduct.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getProductName()));
        colCategory.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCategoryName()));
        colStock.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQtyInStock())));
        colReserved.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getReservedQty())));
        colAvailable.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getAvailableQty())));
        colReorder.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getReorderLevel())));
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isLowStock() ? "LOW STOCK" : "OK"));
        colUpdated.setCellValueFactory(c -> {
            var dt = c.getValue().getLastUpdated();
            return new SimpleStringProperty(dt == null ? "" : dt.format(FMT));
        });

        // Highlight low-stock rows in red
        invTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Inventory item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isLowStock()) {
                    setStyle("-fx-background-color: #fde8e8;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadData() {
        String keyword = searchField.getText();
        boolean lowOnly = lowStockOnly.isSelected();
        try {
            List<Inventory> rows = lowOnly
                ? service.getLowStock()
                : service.search(keyword);
            invTable.setItems(FXCollections.observableArrayList(rows));

            int lowCount = service.countLowStock();
            if (lowCount > 0) {
                lowStockBanner.setText("Warning: " + lowCount + " product(s) are at or below reorder level.");
            } else {
                lowStockBanner.setText("");
            }
            clearFeedback();
        } catch (SQLException e) {
            showError("DB error: " + e.getMessage());
        }
    }

    @FXML private void handleSearch() { loadData(); }

    @FXML private void handleClear() {
        searchField.clear();
        lowStockOnly.setSelected(false);
        loadData();
    }

    @FXML
    private void handleUpdate() {
        Inventory selected = invTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dlg = new TextInputDialog(String.valueOf(selected.getQtyInStock()));
        dlg.setTitle("Set Stock Quantity");
        dlg.setHeaderText("Product: " + selected.getProductName());
        dlg.setContentText("New quantity in stock:");
        dlg.initOwner(invTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty < 0) { showError("Quantity cannot be negative."); return; }
                service.updateStock(selected.getProductId(), qty, selected.getReorderLevel());
                showSuccess("Stock updated to " + qty + ".");
                loadData();
            } catch (NumberFormatException e) {
                showError("Enter a valid integer.");
            } catch (IllegalArgumentException | SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleAdjust() {
        Inventory selected = invTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dlg = new TextInputDialog("0");
        dlg.setTitle("Adjust Stock");
        dlg.setHeaderText("Product: " + selected.getProductName()
            + "  (current: " + selected.getQtyInStock() + ")");
        dlg.setContentText("Adjustment (positive to add, negative to subtract):");
        dlg.initOwner(invTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(input -> {
            try {
                int delta = Integer.parseInt(input.trim());
                service.adjustStock(selected.getProductId(), delta);
                showSuccess("Stock adjusted by " + (delta >= 0 ? "+" : "") + delta + ".");
                loadData();
            } catch (NumberFormatException e) {
                showError("Enter a valid integer (e.g. -5 or +10).");
            } catch (IllegalArgumentException | SQLException e) {
                showError("Adjust failed: " + e.getMessage());
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
