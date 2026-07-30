package com.genai.java.spring.triage.graph;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.review.TicketRagReviewService;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 3 - Recommendation Review. Thin adapter: calls the EXISTING
 * TicketRagReviewService.runRagReview(...), the same code path used by
 * POST /api/tickets/{ticketId}/ai-review/rag. No grounding/validation
 * logic is duplicated here.
 *
 * Prefers the authenticated technician who launched the batch
 * (state.getRequesterUsername(), set by TriageController from
 * Authentication.getName()) so the stored ai_review row is attributed to
 * a real person, same as the single-ticket review flow. Falls back to a
 * fixed system identifier only when no requester is present on the state
 * (e.g. a state built without going through the controller, as in unit
 * tests) — this fallback does NOT require a "triage-batch-orchestrator"
 * row to exist in the users table unless it is actually reached.
 */
@Slf4j
@Component
public class ReviewNode {

    private static final String TRIAGE_SYSTEM_REQUESTER = "triage-batch-orchestrator";

    private final TicketRagReviewService ragReviewService;

    public ReviewNode(TicketRagReviewService ragReviewService) {
        this.ragReviewService = ragReviewService;
    }

    public TriageGraphState apply(TriageGraphState state) {
        Long ticketId = state.getCurrentTicketId();
        if (ticketId == null || state.getCurrentStageError() != null) {
            return state;
        }

        try {
            String requester = (state.getRequesterUsername() != null && !state.getRequesterUsername().isBlank())
                    ? state.getRequesterUsername()
                    : TRIAGE_SYSTEM_REQUESTER;

            RagReviewApiResponse response =
                    ragReviewService.runRagReview(ticketId, requester);

            if (response.getStatus() == AiReviewStatus.FAILED) {
                state.setCurrentStageError(
                        response.getErrorMessage() != null
                                ? response.getErrorMessage()
                                : "Recommendation review failed.");
            } else {
                state.setCurrentReviewResult(response);
            }

        } catch (Exception e) {
            log.warn("Review failed for ticket {}: {}", ticketId, e.getMessage());
            state.setCurrentStageError("Recommendation review failed: " + e.getMessage());
        }

        return state;
    }
}