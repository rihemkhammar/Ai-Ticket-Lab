package com.genai.java.spring.triage.graph;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.service.HitlAgentReviewService;
import com.genai.java.spring.shared.advisor.TicketRoutingRules;
import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.TriageDispatchOutcome;
import com.genai.java.spring.triage.TriageOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HitlCheckpointNodeTest {

    @Mock private HitlAgentReviewService hitlAgentReviewService;
    @Mock private TriageOrchestratorService triageOrchestratorService;

    private HitlCheckpointNode node;

    @BeforeEach
    void setUp() {
        node = new HitlCheckpointNode(hitlAgentReviewService, triageOrchestratorService);
    }

    private TriageGraphState stateFor(Long ticketId, TicketCriticality criticality) {
        TriageGraphState state = new TriageGraphState();
        state.setTriageRunId(50L);
        state.setCurrentTicketId(ticketId);
        state.getClassifications().put(ticketId,
                new TriageClassification(ticketId, criticality, "rationale"));
        return state;
    }

    @Test
    @DisplayName("apply appends a SUCCESS treated item and persists it when the checkpoint is created")
    void apply_checkpointCreated_recordsSuccess() {
        TriageGraphState state = stateFor(1L, TicketCriticality.HIGH);
        state.setCurrentRoutingDecision(TicketRoutingRules.RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY);

        HitlReviewResponse response = new HitlReviewResponse();
        response.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        response.setRunId(200L);
        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class))).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getTreated()).hasSize(1);
        TriageTreatedItem item = result.getTreated().get(0);
        assertThat(item.getTicketId()).isEqualTo(1L);
        assertThat(item.getOutcome()).isEqualTo(TriageDispatchOutcome.SUCCESS);
        assertThat(item.getAgentRunId()).isEqualTo(200L);
        assertThat(item.getRoutingDecision())
                .isEqualTo(TicketRoutingRules.RoutingDecision.ESCALATE_TO_HUMAN_PRIORITY);

        verify(triageOrchestratorService).recordTreated(eq(50L), eq(1L), any(TriageTreatedItem.class));
    }

    @Test
    @DisplayName("apply leaves routingDecision null on success when Rules never set one")
    void apply_checkpointCreated_noRoutingDecisionSet_leavesItNull() {
        TriageGraphState state = stateFor(1L, TicketCriticality.LOW);

        HitlReviewResponse response = new HitlReviewResponse();
        response.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        response.setRunId(201L);
        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class))).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getTreated().get(0).getRoutingDecision()).isNull();
    }

    @Test
    @DisplayName("apply records FAILED when the HITL checkpoint response itself is FAILED")
    void apply_checkpointFailedStatus_recordsFailure() {
        TriageGraphState state = stateFor(1L, TicketCriticality.MEDIUM);

        HitlReviewResponse response = new HitlReviewResponse();
        response.setStatus(AgentRunStatus.FAILED);
        response.setErrorMessage("Checkpoint persistence error.");
        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class))).thenReturn(response);

        TriageGraphState result = node.apply(state);

        TriageTreatedItem item = result.getTreated().get(0);
        assertThat(item.getOutcome()).isEqualTo(TriageDispatchOutcome.FAILED);
        assertThat(item.getErrorMessage()).isEqualTo("Checkpoint persistence error.");
    }

    @Test
    @DisplayName("apply records FAILED with a generic message when a FAILED response has no error message")
    void apply_checkpointFailedNoMessage_usesGenericMessage() {
        TriageGraphState state = stateFor(1L, TicketCriticality.MEDIUM);

        HitlReviewResponse response = new HitlReviewResponse();
        response.setStatus(AgentRunStatus.FAILED);
        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class))).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getTreated().get(0).getErrorMessage())
                .isEqualTo("HITL checkpoint creation failed.");
    }

    @Test
    @DisplayName("apply records FAILED when the HITL service throws")
    void apply_serviceThrows_recordsFailure() {
        TriageGraphState state = stateFor(1L, TicketCriticality.LOW);

        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class)))
                .thenThrow(new RuntimeException("checkpoint table locked"));

        TriageGraphState result = node.apply(state);

        TriageTreatedItem item = result.getTreated().get(0);
        assertThat(item.getOutcome()).isEqualTo(TriageDispatchOutcome.FAILED);
        assertThat(item.getErrorMessage()).contains("checkpoint table locked");
    }

    @Test
    @DisplayName("apply skips the HITL call and records a failure when an earlier stage already errored")
    void apply_earlierStageError_recordsFailureWithoutCallingHitl() {
        TriageGraphState state = stateFor(1L, TicketCriticality.CRITICAL);
        state.setCurrentStageError("investigation failed earlier");

        TriageGraphState result = node.apply(state);

        TriageTreatedItem item = result.getTreated().get(0);
        assertThat(item.getOutcome()).isEqualTo(TriageDispatchOutcome.FAILED);
        assertThat(item.getErrorMessage()).isEqualTo("investigation failed earlier");
        verify(hitlAgentReviewService, never()).startReview(any(), any());
        verify(triageOrchestratorService).recordTreated(eq(50L), eq(1L), any(TriageTreatedItem.class));
    }

    @Test
    @DisplayName("apply does nothing when currentTicketId is null")
    void apply_nullTicketId_doesNothing() {
        TriageGraphState state = new TriageGraphState();

        TriageGraphState result = node.apply(state);

        assertThat(result.getTreated()).isEmpty();
        verify(hitlAgentReviewService, never()).startReview(any(), any());
        verify(triageOrchestratorService, never()).recordTreated(any(), any(), any());
    }

    @Test
    @DisplayName("apply always calls recordTreated exactly once, regardless of outcome (Rule 2.7)")
    void apply_alwaysRecordsTreated() {
        TriageGraphState state = stateFor(1L, TicketCriticality.HIGH);
        HitlReviewResponse response = new HitlReviewResponse();
        response.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        response.setRunId(300L);
        when(hitlAgentReviewService.startReview(eq(1L), any(HitlReviewRequest.class))).thenReturn(response);

        node.apply(state);

        verify(triageOrchestratorService, org.mockito.Mockito.times(1))
                .recordTreated(any(), any(), any());
    }
}