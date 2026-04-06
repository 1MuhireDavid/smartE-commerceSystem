package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.dao.ReviewDAO;
import org.example.dao.UserDAO;
import org.example.model.Product;
import org.example.model.Review;
import org.example.model.User;
import org.example.util.ValidationUtil;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewsController {

    @FXML private Label  titleLabel;
    @FXML private TableView<Review>             reviewTable;
    @FXML private TableColumn<Review, String>   colCustomer;
    @FXML private TableColumn<Review, String>   colRating;
    @FXML private TableColumn<Review, String>   colVerified;   // re-used for "Approved"
    @FXML private TableColumn<Review, String>   colComment;    // re-used for body
    @FXML private TableColumn<Review, String>   colDate;
    @FXML private TextField  customerField;    // used as review title
    @FXML private TextField  ratingField;
    @FXML private TextArea   commentArea;      // review body
    @FXML private CheckBox   verifiedCheck;    // used as isApproved
    @FXML private Button     deleteBtn;
    @FXML private Label      errorLabel;

    private final ReviewDAO dao     = new ReviewDAO();
    private final UserDAO   userDAO = new UserDAO();
    private Product product;

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void initForProduct(Product product) {
        this.product = product;
        titleLabel.setText("Reviews for: " + product.getName());
        setupColumns();
        reviewTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> deleteBtn.setDisable(sel == null));
        loadReviews();
    }

    private void setupColumns() {
        // userName is the reviewer's display name
        colCustomer.setCellValueFactory(c -> {
            String name = c.getValue().getUserName();
            return new SimpleStringProperty(name == null ? "—" : name);
        });
        // getStars() returns e.g. "★★★☆☆"
        colRating.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStars()));
        // colVerified repurposed as "Approved"
        colVerified.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isApproved() ? "✓" : ""));
        // colComment repurposed for review body
        colComment.setCellValueFactory(c -> {
            String body = c.getValue().getBody();
            return new SimpleStringProperty(body == null ? "" : body);
        });
        colDate.setCellValueFactory(c -> {
            var dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });
    }

    private void loadReviews() {
        try {
            List<Review> reviews = dao.findByProductId(product.getId());
            reviewTable.setItems(FXCollections.observableArrayList(reviews));
        } catch (SQLException e) {
            errorLabel.setText("Failed to load reviews: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddReview() {
        // customerField holds the reviewer's username (must exist in users table)
        List<String> errors = ValidationUtil.validateReview(
            customerField.getText(), ratingField.getText(), commentArea.getText());
        if (!errors.isEmpty()) {
            errorLabel.setText(ValidationUtil.joinErrors(errors));
            return;
        }
        errorLabel.setText("");

        try {
            // Look up user by username; reviewer must be a registered user
            User reviewer = userDAO.findByUsername(customerField.getText().trim());
            if (reviewer == null) {
                errorLabel.setText("Username \"" + customerField.getText().trim()
                    + "\" not found. Enter a valid username.");
                return;
            }

            Review review = new Review(
                product.getId(),
                reviewer.getUserId(),
                null,                                       // orderId (not linked)
                Integer.parseInt(ratingField.getText().trim()),
                customerField.getText().trim(),             // title
                commentArea.getText().trim()                // body
            );
            review.setApproved(verifiedCheck.isSelected());

            dao.insert(review);
            customerField.clear();
            ratingField.clear();
            commentArea.clear();
            verifiedCheck.setSelected(false);
            loadReviews();
        } catch (SQLException e) {
            errorLabel.setText("Failed to save review: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteReview() {
        Review selected = reviewTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this review?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    dao.delete(selected.getReviewId());
                    loadReviews();
                } catch (SQLException e) {
                    errorLabel.setText("Delete failed: " + e.getMessage());
                }
            }
        });
    }
}
