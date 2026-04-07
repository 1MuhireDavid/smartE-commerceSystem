package org.example.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SlugValidator implements ConstraintValidator<ValidSlug, String> {

    /**
     * Valid slug pattern:
     * - starts with [a-z0-9]
     * - followed by zero or more groups of a single hyphen then one or more [a-z0-9]
     * - OR is a single [a-z0-9] character
     *
     * This rejects:  -foo, foo-, foo--bar, FOO, foo_bar
     * This accepts:  foo, foo-bar, foo-bar-baz, a1b2c3
     */
    private static final Pattern SLUG_PATTERN =
            Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handled by @NotNull / @NotBlank
        }
        return SLUG_PATTERN.matcher(value).matches();
    }
}
