package org.ecommerce.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ecommerce.model.User;

public class UserDialogController {

    @FXML private TextField    fullNameField;
    @FXML private TextField    usernameField;
    @FXML private TextField    emailField;
    @FXML private TextField    phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox     activeCheck;
    @FXML private PasswordField passwordField;
    @FXML private Label        errorLabel;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("customer", "seller", "admin"));
        roleCombo.setValue("customer");
    }

    /** Pre-fill the form when editing an existing user. */
    public void setUser(User u) {
        fullNameField.setText(u.getFullName());
        usernameField.setText(u.getUsername());
        emailField.setText(u.getEmail());
        phoneField.setText(u.getPhone() == null ? "" : u.getPhone());
        roleCombo.setValue(u.getRole());
        activeCheck.setSelected(u.isActive());
        // password blank means "keep existing"
    }

    /** Returns true if all required fields are valid; shows inline error otherwise. */
    public boolean validate() {
        String name  = fullNameField.getText().trim();
        String uname = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String role  = roleCombo.getValue();

        if (name.isEmpty()) {
            setError("Full name is required.");
            return false;
        }
        if (uname.isEmpty()) {
            setError("Username is required.");
            return false;
        }
        if (uname.length() < 3) {
            setError("Username must be at least 3 characters.");
            return false;
        }
        if (email.isEmpty()) {
            setError("Email is required.");
            return false;
        }
        if (!email.contains("@") || !email.contains(".")) {
            setError("Enter a valid email address.");
            return false;
        }
        if (role == null || role.isEmpty()) {
            setError("Role is required.");
            return false;
        }
        clearError();
        return true;
    }

    /**
     * Build and return a User from the current form values.
     *
     * @param existing the user being edited, or null when creating a new one
     */
    public User buildUser(User existing) {
        User u = new User();
        if (existing != null) {
            u.setUserId(existing.getUserId());
            u.setCreatedAt(existing.getCreatedAt());
        }
        u.setFullName(fullNameField.getText().trim());
        u.setUsername(usernameField.getText().trim());
        u.setEmail(emailField.getText().trim());
        u.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
        u.setRole(roleCombo.getValue());
        u.setActive(activeCheck.isSelected());

        String pw = passwordField.getText();
        if (!pw.isEmpty()) {
            // In a real app: BCrypt.hashpw(pw, BCrypt.gensalt())
            // For this demo we store the raw password (or a simple marker)
            u.setPasswordHash(pw);
        } else if (existing != null) {
            u.setPasswordHash(existing.getPasswordHash());
        } else {
            u.setPasswordHash("changeme");
        }
        return u;
    }

    private void setError(String msg) { errorLabel.setText(msg); }
    private void clearError()         { errorLabel.setText(""); }
}
