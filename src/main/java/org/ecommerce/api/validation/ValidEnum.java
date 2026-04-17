package org.ecommerce.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string value is one of a fixed set of allowed values.
 *
 * <pre>
 * {@code @ValidEnum(allowed = {"customer", "seller", "admin"})}
 * private String role;
 * </pre>
 *
 * Null values pass — combine with {@code @NotNull} / {@code @NotBlank} when the field is mandatory.
 */
@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    /** The permitted string values. */
    String[] allowed();

    String message() default "Value must be one of the allowed options";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
