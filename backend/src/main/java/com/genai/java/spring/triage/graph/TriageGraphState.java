package com.genai.java.spring.triage.graph;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain, serializable state object carried through the LangGraph4j
 * triage StateGraph: START -> ClassifyTicketsNode -> OrderQueueNode
 * -> DispatchNextTicketNode -> (loop | END).
 *
 * Each node receives this state, produces an updated copy/mutation,
 * and passes it to the next node. This class holds no persistence
 * or business logic itself — TriageOrchestratorService is responsible
 * for syncing relevant parts of this state into the triage_run table.
 */
@Getter
@Setter
@NoArgsConstructor
public class TriageGraphState {

    /**
     * Id of the triage_run row this graph execution belongs to.
     */
    private Long triageRunId;

    /**
     * Ticket ids still waiting to be classified. Consumed by
     * ClassifyTicketsNode; the original input batch (max 5 ids,
     * Rule 2.11).
     */
    private List<Long> ticketQueue = new ArrayList<>();

    /**
     * Classification result per ticket id, filled in by
     * ClassifyTicketsNode.
     */
    private Map<Long, TriageClassification> classifications = new HashMap<>();

    /**
     * Ticket ids ordered by criticality (CRITICAL -> HIGH -> MEDIUM ->
     * LOW, stable tie-break), filled in by OrderQueueNode and consumed
     * one at a time by DispatchNextTicketNode. This list shrinks as
     * DispatchNextTicketNode pops tickets off the front.
     */
    private List<Long> orderedQueue = new ArrayList<>();

    /**
     * Tickets already dispatched to the M4 agent, in the order they
     * were processed, whether the dispatch succeeded or failed.
     */
    private List<TriageTreatedItem> treated = new ArrayList<>();

    // -- extended pipeline fields (Triage -> Investigation -> Review ->
    // Rules -> HITL). Popped by DispatchNextTicketNode, carried through
    // the following nodes for the single ticket currently in flight. --

    /**
     * Ticket id currently flowing through Investigation/Review/Rules/HITL,
     * or null when no ticket is being processed (start, or between loops).
     */
    private Long currentTicketId;

    /**
     * Result of Agent 2 (Investigation) for currentTicketId, set by
     * InvestigationNode and read by ReviewNode.
     */
    private Object currentInvestigationResult;

    /**
     * Result of Agent 3 (Review/grounding) for currentTicketId, set by
     * ReviewNode and read by RulesNode.
     */
    private Object currentReviewResult;

    /**
     * Deterministic routing decision for currentTicketId, set by
     * RulesNode and read by HitlCheckpointNode.
     */
    private Object currentRoutingDecision;

    /**
     * Error captured by any pipeline stage for currentTicketId, used by
     * each node to record a FAILED treated entry without aborting the
     * batch (Rule 2.7).
     */
    private String currentStageError;

    /**
     * Username of the technician who launched this triage batch (from
     * Authentication.getName() on the controller). Used by ReviewNode as
     * the "requester" for TicketRagReviewService.runRagReview, instead of
     * a fictitious system account that would need to exist in the users
     * table. Left null in tests / any caller that builds a state without
     * it — ReviewNode falls back to its own fixed system identifier in
     * that case (see ReviewNode.TRIAGE_SYSTEM_REQUESTER).
     */
    private String requesterUsername;

    public TriageGraphState(Long triageRunId, List<Long> ticketQueue) {
        this.triageRunId = triageRunId;
        this.ticketQueue = new ArrayList<>(ticketQueue);
    }

    /**
     * Convenience check used by the conditional edge after
     * DispatchNextTicketNode: loop back while tickets remain,
     * otherwise route to END.
     */
    public boolean hasRemainingTickets() {
        return orderedQueue != null && !orderedQueue.isEmpty();
    }
}