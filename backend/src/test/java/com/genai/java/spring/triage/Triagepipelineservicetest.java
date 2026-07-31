package com.genai.java.spring.triage;

import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.TriageAgentState;
import com.genai.java.spring.triage.graph.TriageGraphState;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TriagePipelineService no longer contains any orchestration logic
 * itself (that now lives in the compiled langgraph4j graph, covered by
 * TriageGraphConfigTest). This test only asserts the thin wiring left
 * here: run creation, markRunning, delegating to CompiledGraph.invoke,
 * and reloading the persisted run afterwards.
 */
@ExtendWith(MockitoExtension.class)
class TriagePipelineServiceTest {

    @Mock private TriageOrchestratorService orchestratorService;
    @Mock private CompiledGraph<TriageAgentState> triageGraph;

    private TriagePipelineService pipelineService;

    @BeforeEach
    void setUp() {
        pipelineService = new TriagePipelineService(orchestratorService, triageGraph);
    }

    @Test
    @DisplayName("startAndRun creates the run, marks it running, invokes the graph, then reloads the run")
    void startAndRun_delegatesToCompiledGraph() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L));

        TriageRunResponse created = new TriageRunResponse();
        created.setRunId(10L);
        created.setTicketQueue(List.of(1L));
        when(orchestratorService.startBatch(request)).thenReturn(created);

        TriageGraphState finalGraphState = new TriageGraphState(10L, List.of(1L));
        when(triageGraph.invoke(any())).thenReturn(Optional.of(
                new TriageAgentState(java.util.Map.of(TriageAgentState.STATE_KEY, finalGraphState))));

        TriageRunResponse finalRun = new TriageRunResponse();
        finalRun.setRunId(10L);
        finalRun.setStatus(TriageRunStatus.COMPLETED);
        when(orchestratorService.getRun(10L)).thenReturn(Optional.of(finalRun));

        TriageRunResponse result = pipelineService.startAndRun(request, "alice");

        assertThat(result).isEqualTo(finalRun);
        verify(orchestratorService).markRunning(10L);
        verify(triageGraph).invoke(any());
        verify(orchestratorService).recordClassifications(10L, finalGraphState.getClassifications());
        verify(orchestratorService).markCompleted(10L);
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

        when(triageGraph.invoke(any())).thenReturn(Optional.empty());
        when(orchestratorService.getRun(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.startAndRun(request, "alice"))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("12");
    }
}