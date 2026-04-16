package org.ecommerce.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Label   statusLabel;
    @FXML private Tab     productsTab;
    @FXML private Tab     categoriesTab;
    @FXML private Tab     performanceTab;
    @FXML private Tab     paymentTab;
    @FXML private Tab     reviewsTab;
    @FXML private Tab     activityTab;

    // Injected automatically by FXMLLoader because the fx:include has fx:id="..."
    @FXML private PaymentController      paymentController;
    @FXML private AllReviewsController   reviewsController;
    @FXML private ActivityLogController  activityLogController;

    @FXML
    public void initialize() {
        statusLabel.setText("Connected");

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == paymentTab   && paymentController    != null) paymentController.refresh();
            if (newTab == reviewsTab   && reviewsController    != null) reviewsController.refresh();
            if (newTab == activityTab  && activityLogController != null) activityLogController.refresh();
        });
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
