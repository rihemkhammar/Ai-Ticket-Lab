package com.genai.java.spring.shared.advisor;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.triage.TicketCriticality;
import org.springframework.stereotype.Component;

/**
 * Deterministic, non-AI routing rules applied after Agent 3 (Review).
 * Pure Java logic — no model call — sitting alongside HumanReviewPolicy
 * (existing, unmodified) rather than replacing it.
 *
 * This is intentionally simple: it decides whether a ticket needs
 * escalated (priority) human attention, but it never changes
 * needsHumanReview itself — HumanReviewPolicy's existing rule
 * (always true) still applies untouched.
 */
@Component
public class TicketRoutingRules {

    public enum RoutingDecision {
        ESCALATE_TO_HUMAN_PRIORITY,
        STANDARD_HUMAN_REVIEW
    }

    /**
     * @param criticality   result of Agent 1 (Triage classification)
     * @param reviewStatus  result of Agent 3 (Review/grounding check)
     */
    public RoutingDecision decide(TicketCriticality criticality, AiReviewStatus reviewStatus) {
        boolean lowConfidenceReview = reviewStatus == AiReviewStatus.FAILED;
        boolean highPriority = criticality == TicketCriticality.CRITICAL
                || criticality == TicketCriticality.HIGH;

        if (highPriority || lowConfidenceReview) {
            return RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY;
        }
        return RoutingDecision.STANDARD_HUMAN_REVIEW;
    }
}