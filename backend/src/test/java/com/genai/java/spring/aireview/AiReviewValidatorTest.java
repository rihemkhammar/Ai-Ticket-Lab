package com.genai.java.spring.aireview;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire
 */
class AiReviewValidatorTest {

    private final AiReviewValidator validator = new AiReviewValidator();

    @Test
    void validResponse_passesValidation() {
        AiReviewValidator.ValidationResult result = validator.validate(validResponse());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void blankSummary_failsValidation() {
        TicketAiReviewResponse response = validResponse();
        response.setSummary("   ");

        AiReviewValidator.ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Summary");
    }

    @Test
    void emptyPossibleCauses_failsValidation() {
        TicketAiReviewResponse response = validResponse();
        response.setPossibleCauses(List.of());

        AiReviewValidator.ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Possible causes");
    }

    @Test
    void emptyRecommendedChecks_failsValidation() {
        TicketAiReviewResponse response = validResponse();
        response.setRecommendedChecks(List.of());

        AiReviewValidator.ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Recommended checks");
    }

    @Test
    void blankDraftResponse_failsValidation() {
        TicketAiReviewResponse response = validResponse();
        response.setDraftResponse("");

        AiReviewValidator.ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Draft response");
    }

    @Test
    void nullConfidence_failsValidation() {
        TicketAiReviewResponse response = validResponse();
        response.setConfidence(null);

        AiReviewValidator.ValidationResult result = validator.validate(response);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Confidence");
    }

    @Test
    void nullResponse_failsValidation() {
        AiReviewValidator.ValidationResult result = validator.validate(null);

        assertThat(result.isValid()).isFalse();
    }

    private TicketAiReviewResponse validResponse() {
        TicketAiReviewResponse response = new TicketAiReviewResponse();
        response.setSummary("The conveyor motor overheats after running for 20 minutes.");
        response.setPossibleCauses(List.of("Insufficient cooling", "Motor overload"));
        response.setRecommendedChecks(List.of("Check ventilation", "Measure motor current"));
        response.setDraftResponse("Please inspect the motor cooling and bearing condition.");
        response.setConfidence(Confidence.MEDIUM);
        return response;
    }
}