package org.example.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.api.dto.request.ProductRequest;

public class DiscountValidator implements ConstraintValidator<ValidDiscount, ProductRequest> {

    @Override
    public boolean isValid(ProductRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        var base     = request.getBasePrice();
        var discount = request.getDiscountPrice();

        // Only validate when both values are present
        if (base == null || discount == null) {
            return true;
        }

        if (discount.compareTo(base) < 0) {
            return true;
        }

        // Attach the violation to discountPrice so the error maps to the right field
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "Discount price (" + discount + ") must be less than base price (" + base + ")"
        ).addPropertyNode("discountPrice").addConstraintViolation();

        return false;
    }
}
