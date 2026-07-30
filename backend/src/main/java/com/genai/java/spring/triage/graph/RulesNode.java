package com.genai.java.spring.triage.graph;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import com.genai.java.spring.shared.advisor.TicketRoutingRules;
import org.springframework.stereotype.Component;

/**
 * Deterministic application rules (no LLM call). Combines Agent 1's
 * criticality with Agent 3's review outcome to decide a routing
 * priority. Never overrides needsHumanReview - that stays governed by
 * HumanReviewPolicy (existing, unmodified) inside M4/M5 themselves.
 */
@Component
public class RulesNode {

    private final TicketRoutingRules routingRules;

    public RulesNode(TicketRoutingRules routingRules) {
        this.routingRules = routingRules;
    }

    public TriageGraphState apply(TriageGraphState state) {
        Long ticketId = state.getCurrentTicketId();
        if (ticketId == null || state.getCurrentStageError() != null) {
            return state;
        }

        TriageClassification classification = state.getClassifications().get(ticketId);
        Object reviewResultObj = state.getCurrentReviewResult();

        AiReviewStatus reviewStatus = (reviewResultObj instanceof RagReviewApiResponse ragResponse)
                ? ragResponse.getStatus()
                : AiReviewStatus.FAILED;

        TicketRoutingRules.RoutingDecision decision =
                routingRules.decide(classification.getCriticality(), reviewStatus);

        state.setCurrentRoutingDecision(decision);
        return state;
    }
}