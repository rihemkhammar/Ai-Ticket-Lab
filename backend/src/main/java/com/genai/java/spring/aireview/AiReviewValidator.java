package com.genai.java.spring.aireview;

import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class AiReviewValidator {



    public ValidationResult validate(TicketAiReviewResponse response) {
        if (response == null) {
            return ValidationResult.invalid("AI response could not be parsed.");
        }
        if (isBlank(response.getSummary())) {
            return ValidationResult.invalid("Summary must not be blank.");
        }
        if (isEmpty(response.getPossibleCauses())) {
            return ValidationResult.invalid("Possible causes must not be empty.");
        }
        if (isEmpty(response.getRecommendedChecks())) {
            return ValidationResult.invalid("Recommended checks must not be empty.");
        }
        if (isBlank(response.getDraftResponse())) {
            return ValidationResult.invalid("Draft response must not be blank.");
        }
        if (response.getConfidence() == null) {
            return ValidationResult.invalid("Confidence must be LOW, MEDIUM, or HIGH.");
        }
        return ValidationResult.valid();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid()           { return valid; }
        public String getErrorMessage()    { return errorMessage; }
    }
}