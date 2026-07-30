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