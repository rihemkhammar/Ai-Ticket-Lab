package com.genai.java.spring.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationRequest;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationResponse;
import com.genai.java.spring.agent.dto.TicketAgentSynthesisResult;
import com.genai.java.spring.agent.prompt.TicketAgentPromptBuilder;
import com.genai.java.spring.agent.tool.AgentToolException;
import com.genai.java.spring.agent.tool.PreviousAiReviewTool;
import com.genai.java.spring.agent.tool.TicketEvidenceTool;
import com.genai.java.spring.agent.tool.TicketLookupTool;
import com.genai.java.spring.agent.tool.TicketRecommendationBoundaryTool;
import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult;
import com.genai.java.spring.agent.tool.dto.RecommendationBoundaryResult;
import com.genai.java.spring.agent.tool.dto.TicketEvidenceResult;
import com.genai.java.spring.agent.tool.dto.TicketLookupResult;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.observability.AiTraceIdGenerator;
import com.genai.java.spring.observability.AiWorkflowLogger;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.genai.java.spring.shared.advisor.PromptInjectionGuard;


@ExtendWith(MockitoExtension.class)
class TicketAgentInvestigationServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock private TicketService ticketService;
    @Mock private TicketLookupTool ticketLookupTool;
    @Mock private TicketEvidenceTool ticketEvidenceTool;
    @Mock private PreviousAiReviewTool previousAiReviewTool;
    @Mock private TicketRecommendationBoundaryTool boundaryTool;
    @Mock private TicketAgentPromptBuilder promptBuilder;
    @Mock private AgentOutputValidator validator;
    @Mock private AgentRunRepository agentRunRepository;
    @Mock private AgentToolCallRepository agentToolCallRepository;

    private TicketAgentInvestigationService service;

    private static final Long TICKET_ID = 1L;

    private AgentRun savedRun;

    @BeforeEach
    void setUp() {
        service = new TicketAgentInvestigationService(
                chatClient, ticketService, ticketLookupTool, ticketEvidenceTool,
                previousAiReviewTool, boundaryTool, promptBuilder,
                new PromptInjectionGuard(), validator,
                agentRunRepository, agentToolCallRepository, new ObjectMapper(),
                new AiTraceIdGenerator(), new AiWorkflowLogger()
        );
        Ticket ticket = mock(Ticket.class);
        lenient().when(ticket.getId()).thenReturn(TICKET_ID);
        lenient().when(ticket.getTitle()).thenReturn("Conveyor motor overheating");
        lenient().when(ticket.getDescription()).thenReturn("Motor temperature increases after 20 minutes.");
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        lenient().when(ticketService.findById(TICKET_ID)).thenReturn(ticket);

        // agentRunRepository.save() always returns a spy over a *real* AgentRun so that
        // setStatus()/setErrorMessage()/setResultJson() calls from the service actually
        // mutate state (a plain mock would silently drop them and getStatus() etc. would
        // keep returning whatever was stubbed, not what the service set) — while still
        // letting the tests use verify(savedRun)... below.
        savedRun = spy(new AgentRun());
        org.springframework.test.util.ReflectionTestUtils.setField(savedRun, "id", 100L);
        savedRun.setTicketId(TICKET_ID);
        savedRun.setPromptVersion("ticket-agent-investigation-v1");
        savedRun.setModelName("openai/gpt-oss-20b");
        savedRun.setCreatedAt(LocalDateTime.now());
        savedRun.setTraceId("trace-1");
        savedRun.setRunType(AgentRunType.AGENT_INVESTIGATION);
        savedRun.setStatus(AgentRunStatus.RUNNING);
        lenient().when(agentRunRepository.save(any(AgentRun.class))).thenReturn(savedRun);

        lenient().when(agentToolCallRepository.save(any(AgentToolCall.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(ticketLookupTool.lookup(TICKET_ID)).thenReturn(
                TicketLookupResult.of(TICKET_ID, "Conveyor motor overheating",
                        "Motor temperature increases after 20 minutes.", "OPEN"));
        lenient().when(ticketEvidenceTool.retrieve(any(Ticket.class), anyInt()))
                .thenReturn(TicketEvidenceResult.of(TICKET_ID, List.of(evidence())));
        lenient().when(previousAiReviewTool.loadRecent(eq(TICKET_ID), anyInt()))
                .thenReturn(PreviousAiReviewResult.of(TICKET_ID, List.of()));
        lenient().when(boundaryTool.load()).thenReturn(
                RecommendationBoundaryResult.of(List.of("inspect equipment"), List.of("close ticket")));

        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        lenient().when(promptBuilder.buildTaskPrompt(any(), anyString(), any(), any(), any(), any()))
                .thenReturn("task");
        lenient().when(promptBuilder.version()).thenReturn("ticket-agent-investigation-v1");
    }

    private EvidenceChunkResponse evidence() {
        return EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9);
    }

    private TicketAgentSynthesisResult validSynthesis() {
        TicketAgentSynthesisResult r = new TicketAgentSynthesisResult();
        r.setInvestigationSummary("Motor overheating likely caused by ventilation blockage.");
        r.setEvidenceRefs(List.of());
        r.setPreviousReviewSummary("No previous AI reviews exist for this ticket.");
        r.setRecommendedNextSteps(List.of("inspect equipment", "verify symptoms"));
        r.setDraftTechnicianResponse("Please inspect the cooling vent.");
        r.setConfidence(Confidence.MEDIUM);
        r.setLimitations(List.of("Only top-3 evidence chunks were considered."));
        r.setNeedsHumanReview(true);
        return r;
    }

    private void stubChatClientEntity(TicketAgentSynthesisResult toReturn) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketAgentSynthesisResult.class)).thenReturn(toReturn);
    }

    private void stubChatClientThrows(RuntimeException ex) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketAgentSynthesisResult.class)).thenThrow(ex);
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("agent service creates a RUNNING run before any tool executes")
    void investigate_createsRunningRun_beforeToolExecution() {
        stubChatClientEntity(validSynthesis());
        doNothing().when(validator).validate(any(), any());

        service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, atLeastOnce()).save(captor.capture());

        AgentRun firstSaved = captor.getAllValues().get(0);
        assertThat(firstSaved.getStatus()).isEqualTo(AgentRunStatus.RUNNING);
        assertThat(firstSaved.getTicketId()).isEqualTo(TICKET_ID);
    }

    @Test
    @DisplayName("agent service updates run to SUCCESS for a valid result")
    void investigate_success_updatesRunToSuccess() {
        stubChatClientEntity(validSynthesis());
        doNothing().when(validator).validate(any(), any());

        TicketAgentInvestigationResponse response = service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.SUCCESS);
        assertThat(response.getNeedsHumanReview()).isTrue();
        assertThat(response.getRecommendedNextSteps()).contains("inspect equipment");

        verify(savedRun).setStatus(AgentRunStatus.SUCCESS);
        verify(savedRun).setResultJson(anyString());
    }

    @Test
    @DisplayName("tool-call trace is recorded for every tool executed")
    void investigate_toolCallTrace_isRecorded() {
        stubChatClientEntity(validSynthesis());
        doNothing().when(validator).validate(any(), any());

        TicketAgentInvestigationResponse response = service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        assertThat(response.getToolCalls()).extracting("toolName").containsExactly(
                TicketLookupTool.NAME, TicketEvidenceTool.NAME,
                PreviousAiReviewTool.NAME, TicketRecommendationBoundaryTool.NAME);
        assertThat(response.getToolCalls()).allSatisfy(tc ->
                assertThat(tc.getStatus()).isEqualTo("SUCCESS"));

        verify(agentToolCallRepository, times(4)).save(any(AgentToolCall.class));
    }

    // ── failure paths ────────────────────────────────────────────────────────

    @Test
    @DisplayName("agent service updates run to FAILED when the AI provider call fails")
    void investigate_providerFailure_updatesRunToFailed() {
        stubChatClientThrows(new RuntimeException("OpenRouter timeout"));

        TicketAgentInvestigationResponse response = service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).isNotBlank();

        verify(savedRun).setStatus(AgentRunStatus.FAILED);
        verify(savedRun).setErrorMessage(anyString());
    }

    @Test
    @DisplayName("agent service updates run to FAILED for invalid model output")
    void investigate_invalidOutput_updatesRunToFailed() {
        stubChatClientEntity(validSynthesis());
        doThrow(new AgentValidationException("Agent output contains a forbidden claim: \"approved\"."))
                .when(validator).validate(any(), any());

        TicketAgentInvestigationResponse response = service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("forbidden claim");

        verify(savedRun).setStatus(AgentRunStatus.FAILED);
    }

    @Test
    @DisplayName("a failing tool short-circuits the chain and fails the run cleanly")
    void investigate_toolFailure_updatesRunToFailed() {
        when(ticketEvidenceTool.retrieve(any(Ticket.class), anyInt()))
                .thenThrow(new AgentToolException("Evidence retrieval unavailable."));

        TicketAgentInvestigationResponse response = service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("Evidence retrieval unavailable");

        // Lookup tool ran and succeeded, evidence tool ran and failed; the chain
        // stopped there, so the review/boundary tools never ran.
        verify(agentToolCallRepository, times(2)).save(any(AgentToolCall.class));
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("investigating a missing ticket throws and never creates an agent run")
    void investigate_ticketNotFound_throwsAndCreatesNoRun() {
        when(ticketService.findById(TICKET_ID)).thenThrow(new TicketNotFoundException(TICKET_ID));

        assertThatThrownBy(() -> service.investigate(TICKET_ID, new TicketAgentInvestigationRequest()))
                .isInstanceOf(TicketNotFoundException.class);

        verifyNoInteractions(agentRunRepository);
    }

    // ── safety guardrail ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ticket status is unchanged after a successful agent run (agent stays read-only)")
    void investigate_doesNotMutateTicket() {
        stubChatClientEntity(validSynthesis());
        doNothing().when(validator).validate(any(), any());

        service.investigate(TICKET_ID, new TicketAgentInvestigationRequest());

        // The only interaction the service has with TicketService is read-only lookups.
        verify(ticketService, atLeastOnce()).findById(TICKET_ID);
        verifyNoMoreInteractions(ticketService);
    }
}