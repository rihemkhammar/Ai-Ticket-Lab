package com.genai.java.spring.triage;

import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.ClassifyTicketsNode;
import com.genai.java.spring.triage.graph.DispatchNextTicketNode;
import com.genai.java.spring.triage.graph.HitlCheckpointNode;
import com.genai.java.spring.triage.graph.InvestigationNode;
import com.genai.java.spring.triage.graph.OrderQueueNode;
import com.genai.java.spring.triage.graph.ReviewNode;
import com.genai.java.spring.triage.graph.RulesNode;
import com.genai.java.spring.triage.graph.TriageClassification;
import com.genai.java.spring.triage.graph.TriageGraphState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the orchestration added to fix the previously-broken
 * TriagePipelineService skeleton: Agent 1 (classify) -> Order -> Dispatch
 * loop -> Agent 2 (investigation, only for CRITICAL/HIGH) -> Agent 3
 * (review) -> Rules -> HITL, one ticket at a time, until the ordered
 * queue is empty, then the final run is reloaded and returned.
 *
 * Each node is mocked as a black box (already covered by its own
 * dedicated *NodeTest); this test only asserts the WIRING: which nodes
 * run, in what order, and under which condition Agent 2 is skipped.
 */
@ExtendWith(MockitoExtension.class)
class TriagePipelineServiceTest {

    @Mock private TriageOrchestratorService orchestratorService;
    @Mock private ClassifyTicketsNode classifyTicketsNode;
    @Mock private OrderQueueNode orderQueueNode;
    @Mock private DispatchNextTicketNode dispatchNextTicketNode;
    @Mock private InvestigationNode investigationNode;
    @Mock private ReviewNode reviewNode;
    @Mock private RulesNode rulesNode;
    @Mock private HitlCheckpointNode hitlCheckpointNode;

    private TriagePipelineService pipelineService;

    @BeforeEach
    void setUp() {
        pipelineService = new TriagePipelineService(
                orchestratorService, classifyTicketsNode, orderQueueNode, dispatchNextTicketNode,
                investigationNode, reviewNode, rulesNode, hitlCheckpointNode);
    }

    @Test
    @DisplayName("startAndRun sends a CRITICAL ticket through Agent 2, then Agent 3, Rules, and HITL")
    void startAndRun_criticalTicket_runsInvestigation() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L));

        TriageRunResponse created = new TriageRunResponse();
        created.setRunId(10L);
        created.setTicketQueue(List.of(1L));
        when(orchestratorService.startBatch(request)).thenReturn(created);

        // classify: sets classification for ticket 1 as CRITICAL
        stubApply(classifyTicketsNode, state ->
                state.getClassifications().put(1L,
                        new TriageClassification(1L, TicketCriticality.CRITICAL, "Service down")));

        stubApply(orderQueueNode, state -> state.setOrderedQueue(new LinkedList<>(List.of(1L))));

        // dispatch is called twice: pops ticket 1, then finds the queue empty
        Queue<Long> dispatchQueue = new LinkedList<>(List.of(1L));
        stubApply(dispatchNextTicketNode, state -> {
            Long next = dispatchQueue.poll();
            state.setCurrentTicketId(next);
        });

        stubApply(investigationNode, state -> { });
        stubApply(reviewNode, state -> { });
        stubApply(rulesNode, state -> { });
        stubApply(hitlCheckpointNode, state -> { });

        TriageRunResponse finalRun = new TriageRunResponse();
        finalRun.setRunId(10L);
        finalRun.setStatus(TriageRunStatus.COMPLETED);
        when(orchestratorService.getRun(10L)).thenReturn(Optional.of(finalRun));

        TriageRunResponse result = pipelineService.startAndRun(request, "alice");

        assertThat(result).isEqualTo(finalRun);
        verify(orchestratorService).markRunning(10L);
        verify(investigationNode).apply(any(TriageGraphState.class));

        org.mockito.ArgumentCaptor<TriageGraphState> stateCaptor =
                org.mockito.ArgumentCaptor.forClass(TriageGraphState.class);
        verify(reviewNode).apply(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getRequesterUsername()).isEqualTo("alice");

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
    @DisplayName("startAndRun skips Agent 2 for a LOW criticality ticket but still runs Agent 3, Rules and HITL")
    void startAndRun_lowCriticalityTicket_skipsInvestigation() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(2L));

        TriageRunResponse created = new TriageRunResponse();
        created.setRunId(11L);
        created.setTicketQueue(List.of(2L));
        when(orchestratorService.startBatch(request)).thenReturn(created);

        stubApply(classifyTicketsNode, state ->
                state.getClassifications().put(2L,
                        new TriageClassification(2L, TicketCriticality.LOW, "Minor cosmetic issue")));
        stubApply(orderQueueNode, state -> state.setOrderedQueue(new LinkedList<>(List.of(2L))));

        Queue<Long> dispatchQueue = new LinkedList<>(List.of(2L));
        stubApply(dispatchNextTicketNode, state -> state.setCurrentTicketId(dispatchQueue.poll()));

        stubApply(reviewNode, state -> { });
        stubApply(rulesNode, state -> { });
        stubApply(hitlCheckpointNode, state -> { });

        when(orchestratorService.getRun(11L)).thenReturn(Optional.of(new TriageRunResponse()));

        pipelineService.startAndRun(request, "alice");

        verify(investigationNode, never()).apply(any());
        verify(reviewNode).apply(any());
        verify(rulesNode).apply(any());
        verify(hitlCheckpointNode).apply(any());
    }

    @Test
    @DisplayName("startAndRun throws when the run cannot be reloaded after processing")
    void startAndRun_runMissingAfterProcessing_throws() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(3L));

        TriageRunResponse created = new TriageRunResponse();
        created.setRunId(12L);
        created.setTicketQueue(List.of());
        when(orchestratorService.startBatch(request)).thenReturn(created);

        stubApply(classifyTicketsNode, state -> { });
        stubApply(orderQueueNode, state -> state.setOrderedQueue(new LinkedList<>()));
        stubApply(dispatchNextTicketNode, state -> state.setCurrentTicketId(null));

        when(orchestratorService.getRun(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.startAndRun(request, "alice"))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("12");
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