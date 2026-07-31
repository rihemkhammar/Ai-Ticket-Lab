package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the wiring compiled by TriageGraphConfig#triageGraph(): Agent 1
 * (classify) -> Order -> Dispatch loop -> Agent 2 (investigation, only
 * for CRITICAL/HIGH) -> Agent 3 (review) -> Rules -> HITL, one ticket at
 * a time, until the ordered queue is empty.
 *
 * Each node is mocked as a black box (already covered by its own
 * dedicated *NodeTest); this test only asserts the GRAPH WIRING: which
 * nodes run, in what order, and under which condition Agent 2 is
 * skipped - i.e. exactly what TriagePipelineServiceTest used to assert
 * against the old hand-written while-loop.
 */
@ExtendWith(MockitoExtension.class)
class TriageGraphConfigTest {

    @Mock private ClassifyTicketsNode classifyTicketsNode;
    @Mock private OrderQueueNode orderQueueNode;
    @Mock private DispatchNextTicketNode dispatchNextTicketNode;
    @Mock private InvestigationNode investigationNode;
    @Mock private ReviewNode reviewNode;
    @Mock private RulesNode rulesNode;
    @Mock private HitlCheckpointNode hitlCheckpointNode;

    private CompiledGraph<TriageAgentState> graph;

    @BeforeEach
    void setUp() throws Exception {
        TriageGraphConfig config = new TriageGraphConfig(
                classifyTicketsNode, orderQueueNode, dispatchNextTicketNode,
                investigationNode, reviewNode, rulesNode, hitlCheckpointNode);
        graph = config.triageGraph();
    }

    @Test
    @DisplayName("graph sends a CRITICAL ticket through Investigation, then Review, Rules, and HITL")
    void criticalTicket_runsInvestigation() {
        stubApply(classifyTicketsNode, state ->
                state.getClassifications().put(1L,
                        new TriageClassification(1L, TicketCriticality.CRITICAL, "Service down")));
        stubApply(orderQueueNode, state -> state.setOrderedQueue(new LinkedList<>(List.of(1L))));

        Queue<Long> dispatchQueue = new LinkedList<>(List.of(1L));
        stubApply(dispatchNextTicketNode, state -> state.setCurrentTicketId(dispatchQueue.poll()));

        stubApply(investigationNode, state -> { });
        stubApply(reviewNode, state -> { });
        stubApply(rulesNode, state -> { });
        stubApply(hitlCheckpointNode, state -> { });

        TriageGraphState initialState = new TriageGraphState(10L, List.of(1L));
        initialState.setRequesterUsername("alice");

        Optional<TriageAgentState> result =
                graph.invoke(Map.of(TriageAgentState.STATE_KEY, initialState));

        assertThat(result).isPresent();
        verify(investigationNode).apply(any(TriageGraphState.class));

        InOrder order = inOrder(classifyTicketsNode, orderQueueNode, dispatchNextTicketNode,
                investigationNode, reviewNode, rulesNode, hitlCheckpointNode);
        order.verify(classifyTicketsNode).apply(any());
        order.verify(orderQueueNode).apply(any());
        order.verify(dispatchNextTicketNode).apply(any());
        order.verify(investigationNode).apply(any());
        order.verify(reviewNode).apply(any());
        order.verify(rulesNode).apply(any());
        order.verify(hitlCheckpointNode).apply(any());
        order.verify(dispatchNextTicketNode).apply(any());
    }

    @Test
    @DisplayName("graph skips Investigation for a LOW criticality ticket but still runs Review, Rules and HITL")
    void lowCriticalityTicket_skipsInvestigation() {
        stubApply(classifyTicketsNode, state ->
                state.getClassifications().put(2L,
                        new TriageClassification(2L, TicketCriticality.LOW, "Minor cosmetic issue")));
        stubApply(orderQueueNode, state -> state.setOrderedQueue(new LinkedList<>(List.of(2L))));

        Queue<Long> dispatchQueue = new LinkedList<>(List.of(2L));
        stubApply(dispatchNextTicketNode, state -> state.setCurrentTicketId(dispatchQueue.poll()));

        stubApply(reviewNode, state -> { });
        stubApply(rulesNode, state -> { });
        stubApply(hitlCheckpointNode, state -> { });

        TriageGraphState initialState = new TriageGraphState(11L, List.of(2L));

        graph.invoke(Map.of(TriageAgentState.STATE_KEY, initialState));

        verify(investigationNode, never()).apply(any());
        verify(reviewNode).apply(any());
        verify(rulesNode).apply(any());
        verify(hitlCheckpointNode).apply(any());
    }

    // -- helpers --------------------------------------------------------

    private interface StateMutation {
        void mutate(TriageGraphState state);
    }

    private void stubApply(ClassifyTicketsNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(OrderQueueNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(DispatchNextTicketNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(InvestigationNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(ReviewNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(RulesNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private void stubApply(HitlCheckpointNode node, StateMutation mutation) {
        when(node.apply(any())).thenAnswer(inv -> apply(inv.getArgument(0), mutation));
    }

    private TriageGraphState apply(TriageGraphState state, StateMutation mutation) {
        mutation.mutate(state);
        return state;
    }
}
