package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.TriageDispatchOutcome;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One entry of a triage_run's treated list: the record of a single
 * ticket having been dispatched to the existing M4 agent (or having
 * failed to be dispatched — Rule 2.7, failure isolation).
 */
@Getter
@Setter
@NoArgsConstructor
public class TriageTreatedItem implements java.io.Serializable {

    private Long ticketId;
    private TicketCriticality criticality;

    /**
     * Id of the agent_run row created by the existing M4
     * TicketAgentInvestigationService for this ticket.
     * Null when the dispatch failed (outcome = FAILED).
     */
    private Long agentRunId;

    private TriageDispatchOutcome outcome;

    /**
     * Populated only when outcome = FAILED.
     */
    private String errorMessage;

    private LocalDateTime processedAt;

    /**
     * Result of the deterministic Rules stage (RulesNode) for this ticket —
     * a com.genai.java.spring.shared.advisor.TicketRoutingRules.RoutingDecision
     * (ESCALATE_TO_HUMAN_PRIORITY / STANDARD_HUMAN_REVIEW), carried here as
     * Object to mirror TriageGraphState#currentRoutingDecision and avoid
     * this DTO depending on the advisor package. Null when an earlier stage
     * failed before Rules ran (outcome = FAILED).
     */
    private Object routingDecision;

    public static TriageTreatedItem success(Long ticketId, TicketCriticality criticality,
                                            Long agentRunId, LocalDateTime processedAt,
                                            Object routingDecision) {
        TriageTreatedItem item = new TriageTreatedItem();
        item.ticketId = ticketId;
        item.criticality = criticality;
        item.agentRunId = agentRunId;
        item.outcome = TriageDispatchOutcome.SUCCESS;
        item.processedAt = processedAt;
        item.routingDecision = routingDecision;
        return item;
    }

    public static TriageTreatedItem failure(Long ticketId, TicketCriticality criticality,
                                            String errorMessage, LocalDateTime processedAt) {
        TriageTreatedItem item = new TriageTreatedItem();
        item.ticketId = ticketId;
        item.criticality = criticality;
        item.agentRunId = null;
        item.outcome = TriageDispatchOutcome.FAILED;
        item.errorMessage = errorMessage;
        item.processedAt = processedAt;
        return item;
    }
}