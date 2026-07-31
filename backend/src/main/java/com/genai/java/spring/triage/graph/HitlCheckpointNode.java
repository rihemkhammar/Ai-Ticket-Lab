package com.genai.java.spring.triage.graph;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.service.HitlAgentReviewService;
import com.genai.java.spring.triage.TriageOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Final pipeline stage. Thin adapter: calls the EXISTING
 * HitlAgentReviewService.startReview(...), the same code path used by
 * POST /api/tickets/{ticketId}/agent/hitl-review, to create a real,
 * traceable human-review checkpoint for this ticket.
 *
 * Regardless of success or failure at any earlier stage (Investigation,
 * Review, Rules), this node ALWAYS records one treated entry and
 * persists it via TriageOrchestratorService.recordTreated (Rule 2.7:
 * a single ticket failure never aborts the batch).
 */
@Slf4j
@Component
public class HitlCheckpointNode {

    private final HitlAgentReviewService hitlAgentReviewService;
    private final TriageOrchestratorService triageOrchestratorService;

    public HitlCheckpointNode(HitlAgentReviewService hitlAgentReviewService,
                              TriageOrchestratorService triageOrchestratorService) {
        this.hitlAgentReviewService = hitlAgentReviewService;
        this.triageOrchestratorService = triageOrchestratorService;
    }

    public TriageGraphState apply(TriageGraphState state) {
        Long ticketId = state.getCurrentTicketId();
        if (ticketId == null) {
            return state;
        }

        TriageClassification classification = state.getClassifications().get(ticketId);
        TriageTreatedItem item;

        if (state.getCurrentStageError() != null) {
            item = TriageTreatedItem.failure(
                    ticketId, classification.getCriticality(),
                    state.getCurrentStageError(), LocalDateTime.now());
        } else {
            item = createCheckpointAndBuildItem(ticketId, classification, state.getCurrentRoutingDecision());
        }

        state.getTreated().add(item);
        triageOrchestratorService.recordTreated(state.getTriageRunId(), ticketId, item);

        return state;
    }

    private TriageTreatedItem createCheckpointAndBuildItem(Long ticketId,
                                                           TriageClassification classification,
                                                           Object routingDecision) {
        try {
            HitlReviewResponse response =
                    hitlAgentReviewService.startReview(ticketId, new HitlReviewRequest());

            if (response.getStatus() == AgentRunStatus.FAILED) {
                return TriageTreatedItem.failure(
                        ticketId, classification.getCriticality(),
                        response.getErrorMessage() != null
                                ? response.getErrorMessage()
                                : "HITL checkpoint creation failed.",
                        LocalDateTime.now());
            }

            return TriageTreatedItem.success(
                    ticketId, classification.getCriticality(),
                    response.getRunId(), LocalDateTime.now(),
                    // Set by RulesNode just before this node runs (null only if
                    // Rules itself never ran, which currentStageError already
                    // covers in apply() above).
                    routingDecision);

        } catch (Exception e) {
            log.warn("HITL checkpoint creation failed for ticket {}: {}", ticketId, e.getMessage());
            return TriageTreatedItem.failure(
                    ticketId, classification.getCriticality(),
                    "HITL checkpoint creation failed: " + e.getMessage(),
                    LocalDateTime.now());
        }
    }
}