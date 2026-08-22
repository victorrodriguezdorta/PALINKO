package com.kawser.cleanspringbootproject.user.infrastructure.adapter.in.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates password strength (min 8 chars, at least one letter and one digit) and,
 * on failure, rebuilds the constraint violation message as a comma-joined
 * list of stable rule codes (e.g. "TOO_SHORT,MISSING_DIGIT") instead of a
 * prose sentence — UserRestExceptionHandler forwards that string verbatim
 * as the WEAK_PASSWORD error's "rules" arg so the frontend can translate
 * each violated rule through its own i18n keys (see errors.passwordRules.*).
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final Pattern LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        List<String> violatedRules = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            violatedRules.add("TOO_SHORT");
        }
        if (!LETTER.matcher(password).find()) {
            violatedRules.add("MISSING_LETTER");
        }
        if (!DIGIT.matcher(password).find()) {
            violatedRules.add("MISSING_DIGIT");
        }

        if (violatedRules.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(String.join(",", violatedRules))
                .addConstraintViolation();
        return false;
    }
}
