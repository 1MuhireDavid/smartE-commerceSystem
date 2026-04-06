package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.model.Category;
import org.example.util.ValidationUtil;

import java.util.List;

public class CategoryDialogController {

    @FXML private TextField nameField;
    @FXML private TextField slugField;
    @FXML private TextField displayOrderField;
    @FXML private CheckBox  activeCheck;
    @FXML private Label     errorLabel;

    public void setCategory(Category category) {
        nameField.setText(category.getName());
        slugField.setText(category.getSlug() == null ? "" : category.getSlug());
        displayOrderField.setText(String.valueOf(category.getDisplayOrder()));
        activeCheck.setSelected(category.isActive());
    }

    public boolean validate() {
        List<String> errors = ValidationUtil.validateCategory(nameField.getText());
        String orderText = displayOrderField.getText().trim();
        if (!orderText.isEmpty()) {
            try {
                Integer.parseInt(orderText);
            } catch (NumberFormatException e) {
                errors.add("Display order must be a whole number.");
            }
        }
        if (!errors.isEmpty()) {
            errorLabel.setText(ValidationUtil.joinErrors(errors));
            return false;
        }
        errorLabel.setText("");
        return true;
    }

    public Category buildCategory(Category existing) {
        Category c = (existing != null) ? existing : new Category();
        String name = nameField.getText().trim();
        c.setName(name);

        String slug = slugField.getText().trim();
        // Auto-generate slug from name if left blank
        if (slug.isEmpty()) {
            slug = name.toLowerCase()
                       .replaceAll("[^a-z0-9]+", "-")
                       .replaceAll("^-|-$", "");
        }
        c.setSlug(slug);

        String orderText = displayOrderField.getText().trim();
        c.setDisplayOrder(orderText.isEmpty() ? 0 : Integer.parseInt(orderText));
        c.setActive(activeCheck.isSelected());
        return c;
    }
}
