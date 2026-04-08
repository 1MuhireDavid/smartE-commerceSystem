package org.ecommerce.util;

import org.ecommerce.dao.UserDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Centralised input-validation helpers used by all dialog controllers.
 * Returns a list of error messages; an empty list means all fields are valid.
 */
public class ValidationUtil {

    private static final UserDAO userdao = new UserDAO();
    private ValidationUtil() {}

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // ── User validation ───────────────────────────────────────────────────────

    public static List<String> validateUser(String fullName, String username, String email) {
        List<String> errors = new ArrayList<>();

        if (isBlank(fullName))
            errors.add("Full name is required.");
        try{
            if (isBlank(username) || userdao.existsByUsername(username)){
                errors.add("Valid Username is required.");
            }
            else if (username.trim().length() < 3)
                errors.add("Username must be at least 3 characters.");
            if (isBlank(email)) {
                errors.add("Email is required.");
            } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                errors.add("Enter a valid email address (e.g. user@example.com).");
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }


        return errors;
    }


    // ── Product validation ────────────────────────────────────────────────────

    public static List<String> validateProduct(String name, String priceText,
                                               String stockText, String categoryName) {
        List<String> errors = new ArrayList<>();
        if (isBlank(name))
            errors.add("Product name is required.");
        else if (name.length() > 200)
            errors.add("Product name must be 200 characters or fewer.");

        if (isBlank(priceText)) {
            errors.add("Price is required.");
        } else {
            try {
                BigDecimal price = new BigDecimal(priceText.trim());
                if (price.compareTo(BigDecimal.ZERO) < 0)
                    errors.add("Price must be 0 or greater.");
                if (price.scale() > 2)
                    errors.add("Price may have at most 2 decimal places.");
            } catch (NumberFormatException e) {
                errors.add("Price must be a valid number (e.g. 9.99).");
            }
        }

        if (isBlank(stockText)) {
            errors.add("Stock quantity is required.");
        } else {
            try {
                int stock = Integer.parseInt(stockText.trim());
                if (stock < 0)
                    errors.add("Stock quantity must be 0 or greater.");
            } catch (NumberFormatException e) {
                errors.add("Stock quantity must be a whole number.");
            }
        }
        if (categoryName == null || isBlank(categoryName) || categoryName.equals("null")) {
            errors.add("Category name is required.");
        }


        return errors;
    }

    // ── Category validation ───────────────────────────────────────────────────

    public static List<String> validateCategory(String name) {
        List<String> errors = new ArrayList<>();
        if (isBlank(name))
            errors.add("Category name is required.");
        else if (name.length() > 100)
            errors.add("Category name must be 100 characters or fewer.");
        return errors;
    }

    // ── Review validation ─────────────────────────────────────────────────────

    public static List<String> validateReview(String customerName, String ratingText,
                                              String comment) {
        List<String> errors = new ArrayList<>();
        if (isBlank(customerName))
            errors.add("Customer name is required.");

        if (isBlank(ratingText)) {
            errors.add("Rating is required.");
        } else {
            try {
                int rating = Integer.parseInt(ratingText.trim());
                if (rating < 1 || rating > 5)
                    errors.add("Rating must be between 1 and 5.");
            } catch (NumberFormatException e) {
                errors.add("Rating must be a whole number between 1 and 5.");
            }
        }

        if (isBlank(comment))
            errors.add("Comment is required.");

        return errors;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String joinErrors(List<String> errors) {
        return String.join("\n", errors);
    }
}
