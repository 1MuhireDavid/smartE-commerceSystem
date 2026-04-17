package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import org.ecommerce.model.Category;
import org.ecommerce.service.CategoryService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CategoryController {

    @FXML private TableView<Category>            categoryTable;
    @FXML private TableColumn<Category, String>  colId;
    @FXML private TableColumn<Category, String>  colName;
    @FXML private TableColumn<Category, String>  colParent;
    @FXML private TableColumn<Category, String>  colSlug;
    @FXML private TableColumn<Category, String>  colActive;
    @FXML private TableColumn<Category, String>  colCreated;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Label  feedbackLabel;

    private final CategoryService service = new CategoryService();
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupColumns();
        categoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean has = sel != null;
                editBtn.setDisable(!has);
                deleteBtn.setDisable(!has);
            });
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getName()));
        colParent.setCellValueFactory(c -> {
            String p = c.getValue().getParentName();
            return new SimpleStringProperty(p == null ? "—" : p);
        });
        colSlug.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSlug() == null ? "" : c.getValue().getSlug()));
        colActive.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isActive() ? "Yes" : "No"));
        colCreated.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });
    }

    private void loadData() {
        try {
            List<Category> cats = service.getAll();
            categoryTable.setItems(FXCollections.observableArrayList(cats));
            clearFeedback();
        } catch (SQLException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        openDialog(null).ifPresent(cat -> {
            try {
                service.add(cat);
                showSuccess("Category \"" + cat.getName() + "\" added.");
                loadData();
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            } catch (SQLException e) {
                showError("DB error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openDialog(selected).ifPresent(updated -> {
            try {
                service.update(updated);
                showSuccess("Category updated.");
                loadData();
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            } catch (SQLException e) {
                showError("DB error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete category \"" + selected.getName() + "\"?");
        confirm.setContentText("Products in this category will have their category cleared.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getId());
                    showSuccess("Category deleted.");
                    loadData();
                } catch (SQLException e) {
                    showError("Failed to delete: " + e.getMessage());
                }
            }
        });
    }

    private Optional<Category> openDialog(Category existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/category_dialog.fxml"));
            DialogPane pane = loader.load();
            CategoryDialogController ctrl = loader.getController();
            if (existing != null) ctrl.setCategory(existing);

            Dialog<Category> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.initOwner(categoryTable.getScene().getWindow());
            dialog.setResultConverter(btn -> {
                if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    return ctrl.buildCategory(existing);
                return null;
            });
            Button okBtn = (Button) pane.lookupButton(
                pane.getButtonTypes().stream()
                    .filter(t -> t.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElseThrow());
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                if (!ctrl.validate()) e.consume();
            });
            return dialog.showAndWait();
        } catch (IOException e) {
            showError("Could not open dialog: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void clearFeedback() { feedbackLabel.setText(""); }
}
