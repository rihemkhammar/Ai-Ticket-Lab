package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.*;
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
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import com.genai.java.spring.hitl.dto.HitlDraft;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.prompt.AgentPromptStateSerializer;
import com.genai.java.spring.observability.AiTraceIdGenerator;
import com.genai.java.spring.observability.AiWorkflowLogger;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.shared.advisor.PromptInjectionGuard;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *  HitlAgentReviewService: creates a RUNNING run,
 * read-only tool chain, drafts a recommendation, and pauses at a persisted
 * PENDING checkpoint (WAITING_FOR_HUMAN). Never finalizes by itself.
 */
@ExtendWith(MockitoExtension.class)
class HitlAgentReviewServiceTest {

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
    @Mock private AgentReviewCheckpointService checkpointService;
    @Mock private AgentPromptStateSerializer promptStateSerializer;

    private HitlAgentReviewService service;

    private static final Long TICKET_ID = 1L;

    private AgentRun savedRun;

    private ListAppender<ILoggingEvent> workflowAppender;
    private Logger workflowTargetLogger;
    private ListAppender<ILoggingEvent> serviceAppender;
    private Logger serviceTargetLogger;

    @BeforeEach
    void setUp() {
        workflowTargetLogger = (Logger) LoggerFactory.getLogger(AiWorkflowLogger.class);
        workflowAppender = new ListAppender<>();
        workflowAppender.start();
        workflowTargetLogger.addAppender(workflowAppender);

        serviceTargetLogger = (Logger) LoggerFactory.getLogger(HitlAgentReviewService.class);
        serviceAppender = new ListAppender<>();
        serviceAppender.start();
        serviceTargetLogger.addAppender(serviceAppender);

        service = new HitlAgentReviewService(
                chatClient, ticketService, ticketLookupTool, ticketEvidenceTool,
                previousAiReviewTool, boundaryTool, promptBuilder,
                new PromptInjectionGuard(), validator,
                agentRunRepository, agentToolCallRepository,
                checkpointService, promptStateSerializer, new ObjectMapper(),
                new AiTraceIdGenerator(), new AiWorkflowLogger()
        );

        Ticket ticket = mock(Ticket.class);
        lenient().when(ticket.getId()).thenReturn(TICKET_ID);
        lenient().when(ticket.getTitle()).thenReturn("Conveyor motor overheating");
        lenient().when(ticket.getDescription()).thenReturn("Motor temperature increases after 20 minutes.");
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        lenient().when(ticketService.findById(TICKET_ID)).thenReturn(ticket);

        savedRun = new AgentRun();
        savedRun.setTicketId(TICKET_ID);
        savedRun.setCreatedAt(LocalDateTime.now());
        lenient().when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(inv -> {
            AgentRun run = inv.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 100L);
            }
            return run;
        });

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

        lenient().when(promptStateSerializer.serialize(any(), any(), any(), any(), any()))
                .thenReturn("{\"userGoal\":\"...\"}");

        lenient().when(checkpointService.createInitialCheckpoint(anyLong(), eq(TICKET_ID), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(pendingSnapshot());
    }

    @AfterEach
    void tearDown() {
        workflowTargetLogger.detachAppender(workflowAppender);
        serviceTargetLogger.detachAppender(serviceAppender);
    }

    private List<String> workflowLogLines() {
        return workflowAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<String> serviceLogLines() {
        return serviceAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private EvidenceChunkResponse evidence() {
        return EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9);
    }

    private CheckpointSnapshot pendingSnapshot() {
        return CheckpointSnapshot.from(pendingEntity());
    }

    private com.genai.java.spring.hitl.AgentReviewCheckpoint pendingEntity() {
        com.genai.java.spring.hitl.AgentReviewCheckpoint entity = new com.genai.java.spring.hitl.AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setAgentRunId(100L);
        entity.setTicketId(TICKET_ID);
        entity.setCheckpointNumber(1);
        entity.setStatus(ReviewCheckpointStatus.PENDING);
        entity.setDraftJson("{\"needsHumanReview\":true}");
        return entity;
    }

    private HitlDraft validDraft() {
        HitlDraft d = new HitlDraft();
        d.setInvestigationSummary("Motor overheating likely caused by ventilation blockage.");
        d.setEvidenceRefs(List.of());
        d.setPreviousReviewSummary("No previous AI reviews exist for this ticket.");
        d.setRecommendedNextSteps(List.of("inspect equipment", "verify symptoms"));
        d.setDraftTechnicianResponse("Please inspect the cooling vent.");
        d.setConfidence(Confidence.MEDIUM);
        d.setLimitations(List.of("Only top-3 evidence chunks were considered."));
        d.setNeedsHumanReview(true);
        return d;
    }

    private void stubChatClientEntity(HitlDraft toReturn) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(HitlDraft.class)).thenReturn(toReturn);
    }

    private void stubChatClientThrows(RuntimeException ex) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(HitlDraft.class)).thenThrow(ex);
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("HITL run creates RUNNING then WAITING_FOR_HUMAN")
    void startReview_createsRunningThenWaitingForHuman() {
        stubChatClientEntity(validDraft());
        doNothing().when(validator).validate(any(), any());

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.WAITING_FOR_HUMAN);

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(AgentRunStatus.RUNNING);
        assertThat(captor.getAllValues().get(captor.getAllValues().size() - 1).getStatus())
                .isEqualTo(AgentRunStatus.WAITING_FOR_HUMAN);
    }

    @Test
    @DisplayName("HITL run creates a pending checkpoint via AgentReviewCheckpointService")
    void startReview_createsPendingCheckpoint() {
        stubChatClientEntity(validDraft());
        doNothing().when(validator).validate(any(), any());

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getCheckpointId()).isEqualTo(500L);
        assertThat(response.getCheckpointNumber()).isEqualTo(1);
        assertThat(response.getCheckpointStatus()).isEqualTo(ReviewCheckpointStatus.PENDING);
        assertThat(response.getNeedsHumanReview()).isTrue();

        verify(checkpointService).createInitialCheckpoint(eq(100L), eq(TICKET_ID), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("the single traceId generated for the run propagates unchanged to every persisted tool call and to the checkpoint")
    void startReview_traceIdPropagatesToToolCallsAndCheckpoint() {
        stubChatClientEntity(validDraft());
        doNothing().when(validator).validate(any(), any());

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        // Capture the traceId actually generated and stamped on the RUNNING agent_run —
        // not a value re-typed by the test — so this test would fail if a future change
        // broke propagation (e.g. by generating a fresh id per tool call, or passing null).
        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, atLeastOnce()).save(runCaptor.capture());
        String generatedTraceId = runCaptor.getAllValues().get(0).getTraceId();
        assertThat(generatedTraceId).isNotBlank();

        ArgumentCaptor<AgentToolCall> toolCallCaptor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallRepository, times(4)).save(toolCallCaptor.capture());
        assertThat(toolCallCaptor.getAllValues())
                .extracting(AgentToolCall::getTraceId)
                .containsOnly(generatedTraceId);

        ArgumentCaptor<String> checkpointTraceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(checkpointService).createInitialCheckpoint(eq(100L), eq(TICKET_ID),
                checkpointTraceIdCaptor.capture(), anyString(), anyString(), anyString());
        assertThat(checkpointTraceIdCaptor.getValue()).isEqualTo(generatedTraceId);

        assertThat(response.getRunId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("the generated traceId still propagates to the tool calls persisted before a failed run stops")
    void startReview_failedRun_traceIdStillPropagatesToPersistedToolCalls() {
        when(ticketEvidenceTool.retrieve(any(Ticket.class), anyInt()))
                .thenThrow(new AgentToolException("Evidence retrieval unavailable."));

        service.startReview(TICKET_ID, new HitlReviewRequest());

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, atLeastOnce()).save(runCaptor.capture());
        String generatedTraceId = runCaptor.getAllValues().get(0).getTraceId();
        assertThat(generatedTraceId).isNotBlank();

        // Lookup tool ran (SUCCESS) then evidence tool ran (FAILED); both tool-call
        // records persisted before the run failed must still carry the same traceId.
        ArgumentCaptor<AgentToolCall> toolCallCaptor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallRepository, times(2)).save(toolCallCaptor.capture());
        assertThat(toolCallCaptor.getAllValues())
                .extracting(AgentToolCall::getTraceId)
                .containsOnly(generatedTraceId);
    }

    @Test
    @DisplayName("HITL draft always requests human review even if the model omits the flag")
    void startReview_draftIncludesNeedsHumanReview() {
        HitlDraft draft = validDraft();
        stubChatClientEntity(draft);
        doNothing().when(validator).validate(any(), any());

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getNeedsHumanReview()).isTrue();
    }

    // ── failure paths ────────────────────────────────────────────────────────

    @Test
    @DisplayName("a failing evidence tool short-circuits the chain and fails the run cleanly")
    void startReview_toolFailure_updatesRunToFailed() {
        when(ticketEvidenceTool.retrieve(any(Ticket.class), anyInt()))
                .thenThrow(new AgentToolException("Evidence retrieval unavailable."));

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("Evidence retrieval unavailable");
        verifyNoInteractions(checkpointService);
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("HITL run fails cleanly when the AI provider call fails")
    void startReview_providerFailure_updatesRunToFailed() {
        stubChatClientThrows(new RuntimeException("OpenRouter timeout"));

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).isNotBlank();
        verifyNoInteractions(checkpointService);
    }

    @Test
    @DisplayName("HITL run fails cleanly for invalid model output and never creates a checkpoint")
    void startReview_invalidOutput_updatesRunToFailed() {
        stubChatClientEntity(validDraft());
        doThrow(new AgentValidationException("Agent output contains a forbidden claim: \"approved\"."))
                .when(validator).validate(any(), any());

        HitlReviewResponse response = service.startReview(TICKET_ID, new HitlReviewRequest());

        assertThat(response.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("forbidden claim");
        verifyNoInteractions(checkpointService);
    }

    @Test
    @DisplayName("investigating a missing ticket throws and never creates an agent run")
    void startReview_ticketNotFound_throwsAndCreatesNoRun() {
        when(ticketService.findById(TICKET_ID)).thenThrow(new TicketNotFoundException(TICKET_ID));

        assertThatThrownBy(() -> service.startReview(TICKET_ID, new HitlReviewRequest()))
                .isInstanceOf(TicketNotFoundException.class);

        verifyNoInteractions(agentRunRepository);
    }

    // ── safety guardrail ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ticket status is unchanged after a HITL run is created (agent stays read-only)")
    void startReview_doesNotMutateTicket() {
        stubChatClientEntity(validDraft());
        doNothing().when(validator).validate(any(), any());

        service.startReview(TICKET_ID, new HitlReviewRequest());

        verify(ticketService, atLeastOnce()).findById(TICKET_ID);
        verifyNoMoreInteractions(ticketService);
    }

    // ── structured logging / observability ──────────────────────────────────

    @Test
    @DisplayName("every tool call emits a TOOL_CALL_STARTED and matching TOOL_CALL_COMPLETED event with the run's traceId")
    void startReview_emitsToolCallStartedAndCompletedEvents() {
        stubChatClientEntity(validDraft());
        doNothing().when(validator).validate(any(), any());

        service.startReview(TICKET_ID, new HitlReviewRequest());

        List<String> lines = workflowLogLines();
        long startedCount = lines.stream().filter(l -> l.contains("event=TOOL_CALL_STARTED")).count();
        long completedCount = lines.stream().filter(l -> l.contains("event=TOOL_CALL_COMPLETED")).count();

        assertThat(startedCount).isEqualTo(4);
        assertThat(completedCount).isEqualTo(4);
        assertThat(lines.stream().filter(l -> l.contains("event=TOOL_CALL_COMPLETED")))
                .allSatisfy(l -> assertThat(l).contains("status=SUCCESS"));
    }

    @Test
    @DisplayName("draft/synthesis content never appears in the AiWorkflowLogger or service log output")
    void startReview_neverLogsDraftContent() {
        HitlDraft draft = validDraft();
        String distinctiveMarker = "XyZ-DISTINCTIVE-DRAFT-TEXT-42";
        draft.setDraftTechnicianResponse(distinctiveMarker);
        stubChatClientEntity(draft);
        doNothing().when(validator).validate(any(), any());

        service.startReview(TICKET_ID, new HitlReviewRequest());

        for (String line : workflowLogLines()) {
            assertThat(line).doesNotContain(distinctiveMarker);
        }
        for (String line : serviceLogLines()) {
            assertThat(line).doesNotContain(distinctiveMarker);
        }
    }
}