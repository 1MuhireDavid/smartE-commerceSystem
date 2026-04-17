package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.ecommerce.model.Category;
import org.ecommerce.model.Product;
import org.ecommerce.model.User;
import org.ecommerce.service.CategoryService;
import org.ecommerce.service.ProductService;
import org.ecommerce.service.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ProductController {

    // ── FXML injections ───────────────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String>  colId;
    @FXML private TableColumn<Product, String>  colName;
    @FXML private TableColumn<Product, String>  colCategory;
    @FXML private TableColumn<Product, String>  colPrice;
    @FXML private TableColumn<Product, String>  colStock;
    @FXML private TableColumn<Product, String>  colCreated;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button reviewsBtn;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label  pageLabel;
    @FXML private Label  resultCountLabel;
    @FXML private Label  feedbackLabel;
    @FXML private ComboBox<Integer> pageSizeBox;
    @FXML private ToggleGroup sortGroup;
    @FXML private ToggleButton sortNameAsc;
    @FXML private ToggleButton sortNameDesc;
    @FXML private ToggleButton sortPriceAsc;
    @FXML private ToggleButton sortPriceDesc;
    @FXML private ToggleButton sortStockAsc;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ProductService  productService  = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final UserService     userService     = new UserService();

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int currentPage = 0;
    private int pageSize    = 15;

    private List<Product> currentProducts = List.of();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        setupSelectionListener();
        setupPageSizeBox();
        loadCategories();
        loadPage();
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> {
            String cat = c.getValue().getCategoryName();
            return new SimpleStringProperty(cat == null ? "—" : cat);
        });
        colPrice.setCellValueFactory(c ->
            new SimpleStringProperty("$" + c.getValue().getPrice().toPlainString()));
        colStock.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getStockQuantity())));
        colCreated.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });
    }

    private void setupSelectionListener() {
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                boolean hasSelection = selected != null;
                editBtn.setDisable(!hasSelection);
                deleteBtn.setDisable(!hasSelection);
                reviewsBtn.setDisable(!hasSelection);
            });
    }

    private void setupPageSizeBox() {
        pageSizeBox.setItems(FXCollections.observableArrayList(10, 15, 25, 50));
        pageSizeBox.setValue(pageSize);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadCategories() {
        try {
            List<Category> cats = categoryService.getAll();
            ObservableList<Category> items = FXCollections.observableArrayList(cats);
            categoryFilter.setItems(items);
        } catch (SQLException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    private void loadPage() {
        try {
            String  keyword    = searchField.getText();
            Category selCat    = categoryFilter.getValue();
            int     categoryId = (selCat == null) ? 0 : selCat.getId();

            if ((keyword != null && !keyword.isBlank()) || categoryId > 0) {
                // Filtered/searched — load all matching, then paginate in-memory
                List<Product> results = productService.search(keyword, categoryId);
                currentProducts = applySort(results);
            } else {
                // No filter — use paginated DB query
                int total = productService.getTotalCount();
                int maxPage = Math.max(0, (total - 1) / pageSize);
                if (currentPage > maxPage) currentPage = maxPage;
                currentProducts = applySort(productService.getPage(pageSize, currentPage * pageSize));
                updatePagination(total);
            }

            int from = currentPage * pageSize;
            int to   = Math.min(from + pageSize, currentProducts.size());
            List<Product> pageItems = (from < currentProducts.size())
                ? currentProducts.subList(from, to)
                : currentProducts;

            productTable.setItems(FXCollections.observableArrayList(pageItems));
            resultCountLabel.setText(currentProducts.size() + " product(s)");
            clearFeedback();
        } catch (SQLException e) {
            showError("DB error: " + e.getMessage());
        }
    }

    private List<Product> applySort(List<Product> products) {
        if (sortNameAsc.isSelected())   return productService.sortByName(products, true);
        if (sortNameDesc.isSelected())  return productService.sortByName(products, false);
        if (sortPriceAsc.isSelected())  return productService.sortByPrice(products, true);
        if (sortPriceDesc.isSelected()) return productService.sortByPrice(products, false);
        if (sortStockAsc.isSelected())  return productService.sortByStock(products, true);
        return products;
    }

    private void updatePagination(int total) {
        int maxPage = (total == 0) ? 0 : (total - 1) / pageSize;
        prevBtn.setDisable(currentPage <= 0);
        nextBtn.setDisable(currentPage >= maxPage);
        pageLabel.setText("Page " + (currentPage + 1) + " of " + (maxPage + 1));
    }

    // ── CRUD handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleAdd() {
        openProductDialog(null).ifPresent(product -> {
            try {
                productService.add(product);
                showSuccess("Product \"" + product.getName() + "\" added.");
                loadPage();
            } catch (SQLException e) {
                showError("Failed to add product: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openProductDialog(selected).ifPresent(updated -> {
            try {
                productService.update(updated);
                showSuccess("Product updated.");
                loadPage();
            } catch (SQLException e) {
                showError("Failed to update product: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    productService.delete(selected.getId());
                    showSuccess("Product deleted.");
                    loadPage();
                } catch (SQLException e) {
                    showError("Failed to delete: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleReviews() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reviews_view.fxml"));
            javafx.scene.Parent root = loader.load();
            ReviewsController ctrl = loader.getController();
            ctrl.initForProduct(selected);

            Stage stage = new Stage();
            stage.setTitle("Reviews — " + selected.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initOwner(productTable.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            showError("Could not open reviews: " + e.getMessage());
        }
    }

    // ── Search / sort / page handlers ─────────────────────────────────────────

    @FXML private void handleSearch()   { currentPage = 0; loadPage(); }
    @FXML private void handleSort()     { currentPage = 0; loadPage(); }
    @FXML private void handleClear()    { searchField.clear(); categoryFilter.setValue(null); handleSearch(); }
    @FXML private void handlePrev()     { if (currentPage > 0) { currentPage--; loadPage(); } }
    @FXML private void handleNext()     { currentPage++; loadPage(); }
    @FXML private void handlePageSize() {
        Integer val = pageSizeBox.getValue();
        if (val != null) { pageSize = val; currentPage = 0; loadPage(); }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private Optional<Product> openProductDialog(Product existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/product_dialog.fxml"));
            DialogPane pane = loader.load();
            ProductDialogController ctrl = loader.getController();
            List<User> sellers = userService.getSellers();
            if (sellers.isEmpty()) {
                showError("No active sellers found. Add a user with role 'seller' or 'admin' first.");
                return Optional.empty();
            }
            ctrl.setSellers(sellers);
            ctrl.setCategories(categoryService.getAll());
            if (existing != null) ctrl.setProduct(existing);

            Dialog<Product> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.initOwner(productTable.getScene().getWindow());
            dialog.setResultConverter(btn -> {
                if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    return ctrl.buildProduct(existing);
                }
                return null;
            });
            // Block the OK button until validation passes
            Button okButton = (Button) pane.lookupButton(
                pane.getButtonTypes().stream()
                    .filter(t -> t.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElseThrow());
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                if (!ctrl.validate()) e.consume();
            });
            return dialog.showAndWait();
        } catch (IOException | SQLException e) {
            showError("Could not open dialog: " + e.getMessage());
            return Optional.empty();
        }
    }

    // ── Feedback helpers ──────────────────────────────────────────────────────

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void clearFeedback() {
        feedbackLabel.setText("");
    }
}
