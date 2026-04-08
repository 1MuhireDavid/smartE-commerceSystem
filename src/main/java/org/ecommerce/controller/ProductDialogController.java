package org.ecommerce.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.model.Category;
import org.ecommerce.model.Product;
import org.ecommerce.model.User;
import org.ecommerce.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;

public class ProductDialogController {

    @FXML private TextField       nameField;
    @FXML private ComboBox<User>  sellerCombo;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField       priceField;
    @FXML private TextField       stockField;
    @FXML private TextArea        descriptionArea;
    @FXML private Label           errorLabel;

    public void setSellers(List<User> sellers) {
        sellerCombo.setItems(FXCollections.observableArrayList(sellers));
        if (!sellers.isEmpty()) sellerCombo.setValue(sellers.get(0));
    }

    public void setCategories(List<Category> categories) {
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
    }

    public void setProduct(Product product) {
        nameField.setText(product.getName());
        priceField.setText(product.getPrice().toPlainString());
        stockField.setText(String.valueOf(product.getStockQuantity()));
        descriptionArea.setText(product.getDescription() == null ? "" : product.getDescription());

        sellerCombo.getItems().stream()
            .filter(u -> u.getUserId() == product.getSellerId())
            .findFirst()
            .ifPresent(sellerCombo::setValue);

        categoryCombo.getItems().stream()
            .filter(c -> c.getId() == product.getCategoryId())
            .findFirst()
            .ifPresent(categoryCombo::setValue);
    }

    public boolean validate() {
        if (sellerCombo.getValue() == null) {
            errorLabel.setText("Seller is required.");
            return false;
        }
        if (categoryCombo.getValue() == null) {
            errorLabel.setText("Category is required.");
            return false;
        }
        List<String> errors = ValidationUtil.validateProduct(
            nameField.getText(), priceField.getText(), stockField.getText(), categoryCombo.getValue() != null ? categoryCombo.getValue().toString() : null);
        if (!errors.isEmpty()) {
            errorLabel.setText(ValidationUtil.joinErrors(errors));
            return false;
        }
        errorLabel.setText("");
        return true;
    }

    public Product buildProduct(Product existing) {
        Product p = (existing != null) ? existing : new Product();
        p.setName(nameField.getText().trim());
        p.setDescription(descriptionArea.getText() == null ? "" : descriptionArea.getText().trim());
        p.setBasePrice(new BigDecimal(priceField.getText().trim()));
        p.setStockQuantity(Integer.parseInt(stockField.getText().trim()));

        User selectedSeller = sellerCombo.getValue();
        p.setSellerId(selectedSeller.getUserId());
        p.setSellerName(selectedSeller.getFullName());

        Category selectedCat = categoryCombo.getValue();
        if (selectedCat != null) {
            p.setCategoryId(selectedCat.getId());
            p.setCategoryName(selectedCat.getName());
        } else {
            p.setCategoryId(0);
            p.setCategoryName(null);
        }
        return p;
    }
}
