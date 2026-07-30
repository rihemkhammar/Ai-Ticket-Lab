package com.genai.java.spring.triage.graph;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import com.genai.java.spring.shared.advisor.TicketRoutingRules;
import com.genai.java.spring.triage.TicketCriticality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RulesNodeTest {

    // Real TicketRoutingRules on purpose: it is pure, deterministic logic
    // with no external dependency, so exercising the real implementation
    // gives more signal than mocking it away.
    private final RulesNode node = new RulesNode(new TicketRoutingRules());

    @Test
    @DisplayName("apply escalates a CRITICAL ticket with a successful review")
    void apply_criticalTicket_escalates() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.getClassifications().put(1L,
                new TriageClassification(1L, TicketCriticality.CRITICAL, "Safety hazard."));

        RagReviewApiResponse review = new RagReviewApiResponse();
        review.setStatus(AiReviewStatus.SUCCESS);
        state.setCurrentReviewResult(review);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision())
                .isEqualTo(TicketRoutingRules.RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY);
    }

    @Test
    @DisplayName("apply escalates a LOW-criticality ticket when the review failed")
    void apply_lowCriticalityFailedReview_escalates() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.getClassifications().put(1L,
                new TriageClassification(1L, TicketCriticality.LOW, "Minor issue."));

        RagReviewApiResponse review = new RagReviewApiResponse();
        review.setStatus(AiReviewStatus.FAILED);
        state.setCurrentReviewResult(review);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision())
                .isEqualTo(TicketRoutingRules.RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY);
    }

    @Test
    @DisplayName("apply treats a review result of an unexpected type as FAILED for routing purposes")
    void apply_unexpectedReviewResultType_treatedAsFailed() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.getClassifications().put(1L,
                new TriageClassification(1L, TicketCriticality.LOW, "Minor issue."));
        state.setCurrentReviewResult("not a RagReviewApiResponse");

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision())
                .isEqualTo(TicketRoutingRules.RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY);
    }

    @Test
    @DisplayName("apply routes a MEDIUM/LOW ticket with a successful review to standard review")
    void apply_mediumCriticalitySuccessfulReview_standardReview() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.getClassifications().put(1L,
                new TriageClassification(1L, TicketCriticality.MEDIUM, "Routine wear."));

        RagReviewApiResponse review = new RagReviewApiResponse();
        review.setStatus(AiReviewStatus.SUCCESS);
        state.setCurrentReviewResult(review);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision())
                .isEqualTo(TicketRoutingRules.RoutingDecision.STANDARD_HUMAN_REVIEW);
    }

    @Test
    @DisplayName("apply does nothing when currentTicketId is null")
    void apply_nullTicketId_doesNothing() {
        TriageGraphState state = new TriageGraphState();

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision()).isNull();
    }

    @Test
    @DisplayName("apply skips the decision when an earlier stage already recorded an error")
    void apply_earlierStageError_skipsDecision() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.setCurrentStageError("review failed earlier");

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentRoutingDecision()).isNull();
    }
}