package com.genai.java.spring.triage.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * TriageGraphConfig does not expose a compiled @Bean yet (Phase 4 TODO):
 * the real langgraph4j StateGraph wiring is still pending. This test only
 * locks in the current, intentional placeholder behavior described in the
 * class , so that it fails loudly (instead of silently) once the
 * real wiring is added and this test needs to be rewritten.
 */
class TriageGraphConfigTest {

    @Test
    @DisplayName("buildCompiledGraphOnceWiringIsFinished throws until the real langgraph4j wiring is added")
    void buildCompiledGraph_notYetWired_throwsUnsupportedOperationException() {
        TriageGraphConfig config = new TriageGraphConfig(
                mock(ClassifyTicketsNode.class),
                mock(OrderQueueNode.class),
                mock(DispatchNextTicketNode.class),
                mock(InvestigationNode.class),
                mock(ReviewNode.class),
                mock(RulesNode.class),
                mock(HitlCheckpointNode.class)
        );

        assertThatThrownBy(config::buildCompiledGraphOnceWiringIsFinished)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("wire the real langgraph4j StateGraph");
    }
}