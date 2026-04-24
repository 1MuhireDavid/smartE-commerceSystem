package org.ecommerce.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.dao.ReviewDAO;
import org.ecommerce.model.Review;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AllReviewsController {

    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  approvedFilter;
    @FXML private TableView<Review>            reviewTable;
    @FXML private TableColumn<Review, String>  colProduct;
    @FXML private TableColumn<Review, String>  colCustomer;
    @FXML private TableColumn<Review, String>  colRating;
    @FXML private TableColumn<Review, String>  colApproved;
    @FXML private TableColumn<Review, String>  colComment;
    @FXML private TableColumn<Review, String>  colDate;
    @FXML private Button approveBtn;
    @FXML private Button deleteBtn;
    @FXML private Label  feedbackLabel;

    private final ReviewDAO dao = new ReviewDAO();
    private List<Review> allReviews = List.of();

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        approvedFilter.setItems(FXCollections.observableArrayList("All", "Approved", "Pending"));
        approvedFilter.setValue("All");

        setupColumns();
        reviewTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean has = sel != null;
                deleteBtn.setDisable(!has);
                approveBtn.setDisable(sel == null || sel.isApproved());
            });
        loadReviews();
    }

    private void setupColumns() {
        colProduct.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getProductName()));
        colCustomer.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUserName()));
        colRating.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStars()));
        colApproved.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isApproved() ? "✓" : ""));
        colComment.setCellValueFactory(c -> {
            String body = c.getValue().getBody();
            return new SimpleStringProperty(body == null ? "" : body);
        });
        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });
    }

    /** Called by MainController when this tab gains focus, so data stays fresh. */
    public void refresh() {
        loadReviews();
    }

    private void loadReviews() {
        try {
            allReviews = dao.findAll();
            applyFilter();
            feedbackLabel.setText("");
        } catch (SQLException e) {
            showError("Failed to load reviews: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String kw  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String flt = approvedFilter.getValue();

        List<Review> filtered = allReviews.stream()
            .filter(r -> kw.isEmpty() || r.getProductName().toLowerCase().contains(kw))
            .filter(r -> switch (flt == null ? "All" : flt) {
                case "Approved" -> r.isApproved();
                case "Pending"  -> !r.isApproved();
                default         -> true;
            })
            .collect(Collectors.toList());

        reviewTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML private void handleSearch() { applyFilter(); }

    @FXML private void handleClear() {
        searchField.clear();
        approvedFilter.setValue("All");
        applyFilter();
    }

    @FXML
    private void handleApprove() {
        Review selected = reviewTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isApproved()) return;
        try {
            dao.approve(selected.getReviewId());
            showSuccess("Review approved.");
            loadReviews();
        } catch (SQLException e) {
            showError("Approve failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Review selected = reviewTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this review?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    dao.delete(selected.getReviewId());
                    showSuccess("Review deleted.");
                    loadReviews();
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }

    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12;");
        feedbackLabel.setText(msg);
    }
}
