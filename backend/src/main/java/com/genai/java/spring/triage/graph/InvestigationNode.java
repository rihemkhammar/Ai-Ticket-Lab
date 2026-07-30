package com.genai.java.spring.triage.graph;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.agent.TicketAgentInvestigationService;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationRequest;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 2 - Investigation. Thin adapter: calls the EXISTING M4
 * TicketAgentInvestigationService.investigate(...) - the exact same
 * code path used by POST /api/tickets/{ticketId}/agent/investigate.
 * No tool/prompt logic is duplicated here (Rule 2.4).
 *
 * Failure isolation (Rule 2.7): any exception, or a FAILED
 * AgentRunStatus, is recorded in state.currentStageError instead of
 * propagating, so DispatchNextTicketNode's loop can continue with the
 * next ticket.
 */
@Slf4j
@Component
public class InvestigationNode {

    private static final String TRIAGE_DISPATCH_USER_GOAL =
            "Investigate this ticket and recommend next checks (triage batch dispatch).";

    private final TicketAgentInvestigationService investigationService;

    public InvestigationNode(TicketAgentInvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    public TriageGraphState apply(TriageGraphState state) {
        Long ticketId = state.getCurrentTicketId();
        if (ticketId == null || state.getCurrentStageError() != null) {
            return state; // nothing to dispatch, or an earlier stage already failed
        }

        try {
            TicketAgentInvestigationRequest request = new TicketAgentInvestigationRequest();
            request.setUserGoal(TRIAGE_DISPATCH_USER_GOAL);

            TicketAgentInvestigationResponse response =
                    investigationService.investigate(ticketId, request);

            if (response.getStatus() == AgentRunStatus.FAILED) {
                state.setCurrentStageError(
                        response.getErrorMessage() != null
                                ? response.getErrorMessage()
                                : "M4 investigation failed.");
            } else {
                state.setCurrentInvestigationResult(response);
            }

        } catch (Exception e) {
            log.warn("Investigation dispatch failed for ticket {}: {}", ticketId, e.getMessage());
            state.setCurrentStageError("M4 investigation failed: " + e.getMessage());
        }

        return state;
    }
}