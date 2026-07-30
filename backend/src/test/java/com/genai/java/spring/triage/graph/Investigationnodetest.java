package com.genai.java.spring.triage.graph;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.agent.TicketAgentInvestigationService;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationRequest;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationResponse;
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
class InvestigationNodeTest {

    @Mock private TicketAgentInvestigationService investigationService;

    private InvestigationNode node;

    @BeforeEach
    void setUp() {
        node = new InvestigationNode(investigationService);
    }

    @Test
    @DisplayName("apply stores the investigation result when M4 succeeds")
    void apply_success_storesInvestigationResult() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setStatus(AgentRunStatus.SUCCESS);
        when(investigationService.investigate(eq(1L), any(TicketAgentInvestigationRequest.class)))
                .thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentInvestigationResult()).isEqualTo(response);
        assertThat(result.getCurrentStageError()).isNull();
    }

    @Test
    @DisplayName("apply records currentStageError when M4 returns a FAILED status")
    void apply_m4Failed_recordsStageError() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setStatus(AgentRunStatus.FAILED);
        response.setErrorMessage("Investigation could not complete.");
        when(investigationService.investigate(eq(1L), any(TicketAgentInvestigationRequest.class)))
                .thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("Investigation could not complete.");
        assertThat(result.getCurrentInvestigationResult()).isNull();
    }

    @Test
    @DisplayName("apply falls back to a generic message when a FAILED response has no error message")
    void apply_m4FailedNoMessage_usesGenericMessage() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setStatus(AgentRunStatus.FAILED);
        when(investigationService.investigate(eq(1L), any(TicketAgentInvestigationRequest.class)))
                .thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("M4 investigation failed.");
    }

    @Test
    @DisplayName("apply records currentStageError when the investigation service throws")
    void apply_exception_recordsStageError() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        when(investigationService.investigate(eq(1L), any(TicketAgentInvestigationRequest.class)))
                .thenThrow(new RuntimeException("connection reset"));

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).contains("M4 investigation failed");
        assertThat(result.getCurrentStageError()).contains("connection reset");
    }

    @Test
    @DisplayName("apply does nothing when currentTicketId is null")
    void apply_nullTicketId_doesNothing() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(null);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentInvestigationResult()).isNull();
        verify(investigationService, never()).investigate(any(), any());
    }

    @Test
    @DisplayName("apply skips dispatch when an earlier stage already recorded an error")
    void apply_earlierStageError_skipsDispatch() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.setCurrentStageError("previous failure");

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("previous failure");
        verify(investigationService, never()).investigate(any(), any());
    }
}