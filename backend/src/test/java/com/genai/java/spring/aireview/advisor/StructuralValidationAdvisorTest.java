package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StructuralValidationAdvisorTest {

    private final StructuralValidationAdvisor advisor = new StructuralValidationAdvisor();

    private AiReviewContext context;

    @BeforeEach
    void setUp() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Pump vibration detected");
        ticket.setDescription("The pump vibrates strongly during normal operation.");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ReflectionTestUtils.setField(ticket, "id", 1L);

        context = new AiReviewContext(ticket, UUID.randomUUID(), "demo_technician");
    }

    @Test
    void stageIsPostCall() {
        assertThat(advisor.getStage()).isEqualTo(AiReviewAdvisor.Stage.POST_CALL);
    }

    @Test
    void isValid_whenAllRequiredFieldsArePresent() {
        context.setAiResponse(fullyValidResponse());

        advisor.advise(context);

        assertThat(context.isValid()).isTrue();
        assertThat(context.getValidationErrors()).isEmpty();
    }

    @Test
    void invalidates_whenAiResponseIsNull() {
        context.setAiResponse(null);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.firstError()).contains("could not be parsed");
    }

    @Test
    void invalidates_whenSummaryIsBlank() {
        TicketAiReviewResponse response = fullyValidResponse();
        response.setSummary("   ");
        context.setAiResponse(response);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getValidationErrors()).anyMatch(e -> e.contains("Summary"));
    }

    @Test
    void invalidates_whenPossibleCausesIsEmpty() {
        TicketAiReviewResponse response = fullyValidResponse();
        response.setPossibleCauses(List.of());
        context.setAiResponse(response);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getValidationErrors()).anyMatch(e -> e.contains("Possible causes"));
    }

    @Test
    void invalidates_whenRecommendedChecksIsEmpty() {
        TicketAiReviewResponse response = fullyValidResponse();
        response.setRecommendedChecks(null);
        context.setAiResponse(response);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getValidationErrors()).anyMatch(e -> e.contains("Recommended checks"));
    }

    @Test
    void invalidates_whenDraftResponseIsBlank() {
        TicketAiReviewResponse response = fullyValidResponse();
        response.setDraftResponse("");
        context.setAiResponse(response);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getValidationErrors()).anyMatch(e -> e.contains("Draft response"));
    }

    @Test
    void invalidates_whenConfidenceIsMissing() {
        TicketAiReviewResponse response = fullyValidResponse();
        response.setConfidence(null);
        context.setAiResponse(response);

        advisor.advise(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getValidationErrors()).anyMatch(e -> e.contains("Confidence"));
    }

    private TicketAiReviewResponse fullyValidResponse() {
        TicketAiReviewResponse response = new TicketAiReviewResponse();
        response.setSummary("The pump shows abnormal vibration during operation.");
        response.setPossibleCauses(List.of("Misalignment", "Worn bearing"));
        response.setRecommendedChecks(List.of("Inspect alignment", "Check bearing condition"));
        response.setDraftResponse("Please inspect alignment and bearing condition.");
        response.setConfidence(Confidence.MEDIUM);
        response.setLimitations(List.of("Based only on ticket description."));
        response.setNeedsHumanReview(true);
        return response;
    }
}