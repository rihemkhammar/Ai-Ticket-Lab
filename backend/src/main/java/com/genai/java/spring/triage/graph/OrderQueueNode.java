package com.genai.java.spring.triage.graph;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic rule (no LLM call): orders classified tickets
 * CRITICAL -> HIGH -> MEDIUM -> LOW, with a stable tie-break by
 * ascending ticket id for tickets sharing the same criticality
 * (Rule 2.6).
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

        state.setOrderedQueue(ordered);
        return state;
    }
}