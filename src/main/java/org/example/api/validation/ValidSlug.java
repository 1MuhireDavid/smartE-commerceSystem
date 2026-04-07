package org.example.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string is a well-formed URL slug:
 * <ul>
 *   <li>Only lowercase letters (a–z), digits (0–9), and hyphens</li>
 *   <li>Must start and end with a letter or digit (no leading/trailing hyphens)</li>
 *   <li>No consecutive hyphens (--)</li>
 *   <li>At least one character</li>
 * </ul>
 * Null values are considered valid — combine with {@code @NotNull} / {@code @NotBlank} if needed.
 */
@Documented
@Constraint(validatedBy = SlugValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSlug {

    String message() default "Slug must contain only lowercase letters, digits, and single hyphens, " +
                             "and must not start or end with a hyphen";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
