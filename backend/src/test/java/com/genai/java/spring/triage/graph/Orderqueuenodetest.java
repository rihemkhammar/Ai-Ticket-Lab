package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderQueueNodeTest {

    private final OrderQueueNode node = new OrderQueueNode();

    private void classify(TriageGraphState state, Long ticketId, TicketCriticality criticality) {
        state.getClassifications().put(ticketId,
                new TriageClassification(ticketId, criticality, "rationale"));
    }

    @Test
    @DisplayName("apply orders tickets CRITICAL -> HIGH -> MEDIUM -> LOW")
    void apply_ordersByDescendingCriticality() {
        TriageGraphState state = new TriageGraphState();
        classify(state, 1L, TicketCriticality.LOW);
        classify(state, 2L, TicketCriticality.CRITICAL);
        classify(state, 3L, TicketCriticality.MEDIUM);
        classify(state, 4L, TicketCriticality.HIGH);

        TriageGraphState result = node.apply(state);

        assertThat(result.getOrderedQueue()).containsExactly(2L, 4L, 3L, 1L);
    }

    @Test
    @DisplayName("apply breaks ties within the same criticality by ascending ticket id")
    void apply_tieBreaksByAscendingTicketId() {
        TriageGraphState state = new TriageGraphState();
        classify(state, 5L, TicketCriticality.HIGH);
        classify(state, 3L, TicketCriticality.HIGH);
        classify(state, 4L, TicketCriticality.HIGH);

        TriageGraphState result = node.apply(state);

        assertThat(result.getOrderedQueue()).containsExactly(3L, 4L, 5L);
    }

    @Test
    @DisplayName("apply on no classifications results in an empty ordered queue")
    void apply_noClassifications_emptyOrderedQueue() {
        TriageGraphState state = new TriageGraphState();

        TriageGraphState result = node.apply(state);

        assertThat(result.getOrderedQueue()).isEmpty();
    }
}