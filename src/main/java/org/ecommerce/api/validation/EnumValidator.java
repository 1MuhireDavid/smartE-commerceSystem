package org.ecommerce.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> allowed;
    private String      allowedList;

    @Override
    public void initialize(ValidEnum annotation) {
        allowed     = Arrays.stream(annotation.allowed()).collect(Collectors.toUnmodifiableSet());
        allowedList = String.join(", ", annotation.allowed());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handled by @NotNull / @NotBlank
        }

        if (allowed.contains(value)) {
            return true;
        }

        // Replace the default message with one listing the valid options
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "must be one of: " + allowedList
        ).addConstraintViolation();

        return false;
    }
}
