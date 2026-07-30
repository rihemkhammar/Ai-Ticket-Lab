package com.genai.java.spring.triage.graph;

import org.springframework.stereotype.Component;

/**
 * Pops the next ticket id off state.orderedQueue into
 * state.currentTicketId, and resets the per-ticket intermediate
 * fields (investigation/review/routing results, stage error) so the
 * following nodes (Investigation, Review, Rules, HITL) start clean
 * for this ticket.
 *
 * If orderedQueue is empty, does nothing: the conditional edge routes
 * to END in that case, per the graph wiring in TriageGraphConfig.
 */
@Component
public class DispatchNextTicketNode {

    public TriageGraphState apply(TriageGraphState state) {
        if (!state.hasRemainingTickets()) {
            state.setCurrentTicketId(null);
            return state;
        }

        Long nextTicketId = state.getOrderedQueue().remove(0);

        state.setCurrentTicketId(nextTicketId);
        state.setCurrentInvestigationResult(null);
        state.setCurrentReviewResult(null);
        state.setCurrentRoutingDecision(null);
        state.setCurrentStageError(null);

        return state;
    }
}