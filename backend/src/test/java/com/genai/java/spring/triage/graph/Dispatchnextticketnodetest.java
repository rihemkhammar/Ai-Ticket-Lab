package com.genai.java.spring.triage.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchNextTicketNodeTest {

    private final DispatchNextTicketNode node = new DispatchNextTicketNode();

    @Test
    @DisplayName("apply pops the first ticket off orderedQueue into currentTicketId")
    void apply_popsFirstTicket() {
        TriageGraphState state = new TriageGraphState();
        state.setOrderedQueue(new ArrayList<>(List.of(1L, 2L, 3L)));

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentTicketId()).isEqualTo(1L);
        assertThat(result.getOrderedQueue()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("apply resets per-ticket intermediate fields for the newly dispatched ticket")
    void apply_resetsIntermediateFields() {
        TriageGraphState state = new TriageGraphState();
        state.setOrderedQueue(new ArrayList<>(List.of(1L)));
        state.setCurrentInvestigationResult("stale investigation");
        state.setCurrentReviewResult("stale review");
        state.setCurrentRoutingDecision("stale routing");
        state.setCurrentStageError("stale error");

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentInvestigationResult()).isNull();
        assertThat(result.getCurrentReviewResult()).isNull();
        assertThat(result.getCurrentRoutingDecision()).isNull();
        assertThat(result.getCurrentStageError()).isNull();
    }

    @Test
    @DisplayName("apply on an empty orderedQueue sets currentTicketId to null and does nothing else")
    void apply_emptyQueue_setsCurrentTicketIdNull() {
        TriageGraphState state = new TriageGraphState();
        state.setOrderedQueue(new ArrayList<>());
        state.setCurrentTicketId(99L);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentTicketId()).isNull();
        assertThat(result.getOrderedQueue()).isEmpty();
    }
}