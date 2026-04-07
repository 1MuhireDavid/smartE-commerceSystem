package org.example.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Class-level constraint for product pricing.
 * When both {@code basePrice} and {@code discountPrice} are present,
 * {@code discountPrice} must be strictly less than {@code basePrice}.
 *
 * <p>Applied to {@link org.example.api.dto.request.ProductRequest}.
 */
@Documented
@Constraint(validatedBy = DiscountValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDiscount {

    String message() default "Discount price must be less than the base price";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
