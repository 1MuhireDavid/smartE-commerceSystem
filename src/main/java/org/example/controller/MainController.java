package org.example.controller;

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

    @FXML
    public void initialize() {
        statusLabel.setText("Connected");
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
