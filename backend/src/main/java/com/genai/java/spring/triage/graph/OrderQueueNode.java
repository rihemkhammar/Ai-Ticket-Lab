package com.genai.java.spring.triage.graph;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic rule (no LLM call): orders classified tickets
 * CRITICAL -> HIGH -> MEDIUM -> LOW, with a stable tie-break by
 * ascending ticket id for tickets sharing the same criticality
 * (Rule 2.6).
 *
 * Only the single most critical ticket is queued for the full
 * pipeline (dispatch -> investigate -> review -> rules -> hitl).
 * All other tickets stay in state.classifications (so their ranking
 * is still visible/returned to the caller) but are never dispatched,
 * which keeps the graph well under LangGraph4j's default max
 * iterations regardless of batch size.
 */
@Component
public class OrderQueueNode {

    public TriageGraphState apply(TriageGraphState state) {
        List<Long> ordered = state.getClassifications().keySet().stream()
                .sorted(Comparator
                        .comparingInt((Long ticketId) ->
                                state.getClassifications().get(ticketId).getCriticality().ordinal())
                        .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());

        List<Long> topOnly = ordered.isEmpty()
                ? ordered
                : ordered.subList(0, 1);

        state.setOrderedQueue(new ArrayList<>(topOnly));
        return state;
    }
}