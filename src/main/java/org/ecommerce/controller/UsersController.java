package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import org.ecommerce.model.User;
import org.ecommerce.service.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class UsersController {

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private TableView<User>               userTable;
    @FXML private TableColumn<User, Long>       colId;
    @FXML private TableColumn<User, String>     colName;
    @FXML private TableColumn<User, String>     colUsername;
    @FXML private TableColumn<User, String>     colEmail;
    @FXML private TableColumn<User, String>     colRole;
    @FXML private TableColumn<User, String>     colActive;
    @FXML private TableColumn<User, String>     colCreated;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Label  feedbackLabel;

    private final UserService service = new UserService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList(
            "customer", "seller", "admin"));
        setupColumns();
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean has = sel != null;
                editBtn.setDisable(!has);
                deleteBtn.setDisable(!has);
            });
        loadData(null, null);
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new javafx.beans.property.SimpleLongProperty(c.getValue().getUserId()).asObject());
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getFullName()));
        colUsername.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUsername()));
        colEmail.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getRole()));
        colActive.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isActive() ? "Yes" : "No"));
        colCreated.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(FMT));
        });
    }

    private void loadData(String keyword, String role) {
        try {
            List<User> users = service.search(keyword, role);
            userTable.setItems(FXCollections.observableArrayList(users));
            clearFeedback();
        } catch (SQLException e) {
            showError("DB error: " + e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        loadData(searchField.getText(), roleFilter.getValue());
    }

    @FXML private void handleClear() {
        searchField.clear();
        roleFilter.setValue(null);
        loadData(null, null);
    }

    @FXML
    private void handleAdd() {
        openDialog(null).ifPresent(u -> {
            try {
                service.add(u);
                showSuccess("User \"" + u.getUsername() + "\" created.");
                loadData(null, null);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            } catch (SQLException e) {
                showError("DB error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openDialog(selected).ifPresent(u -> {
            try {
                service.update(u);
                showSuccess("User updated.");
                loadData(null, null);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            } catch (SQLException e) {
                showError("DB error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete user \"" + selected.getUsername() + "\"?");
        confirm.setContentText("This will also delete all their orders, reviews and wishlist items.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getUserId());
                    showSuccess("User deleted.");
                    loadData(null, null);
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    private Optional<User> openDialog(User existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/user_dialog.fxml"));
            DialogPane pane = loader.load();
            UserDialogController ctrl = loader.getController();
            if (existing != null) ctrl.setUser(existing);

            Dialog<User> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.initOwner(userTable.getScene().getWindow());
            dialog.setResultConverter(btn -> {
                if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    return ctrl.buildUser(existing);
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
        feedbackLabel.setStyle("-fx-text-fill: #27ae60;");
        feedbackLabel.setText(msg);
    }
    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b;");
        feedbackLabel.setText(msg);
    }
    private void clearFeedback() { feedbackLabel.setText(""); }
}
