package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.*;
import com.genai.java.spring.agent.prompt.TicketAgentPromptBuilder;
import com.genai.java.spring.agent.tool.PreviousAiReviewTool;
import com.genai.java.spring.agent.tool.TicketEvidenceTool;
import com.genai.java.spring.agent.tool.TicketLookupTool;
import com.genai.java.spring.agent.tool.TicketRecommendationBoundaryTool;
import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult;
import com.genai.java.spring.agent.tool.dto.RecommendationBoundaryResult;
import com.genai.java.spring.agent.tool.dto.TicketEvidenceResult;
import com.genai.java.spring.agent.tool.dto.TicketLookupResult;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import com.genai.java.spring.hitl.dto.HitlDraft;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.dto.HumanReviewDecisionRequest;
import com.genai.java.spring.hitl.prompt.AgentPromptStateSerializer;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.shared.advisor.PromptInjectionGuard;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Official State Safety Rule: the HITL flow must never mutate the
 * ticket, at any point in the flow (run creation, approve, reject, request
 * revision). This is the single most important safety guardrail of M5.
 *
 * HumanReviewDecisionService doesn't even hold a TicketService reference,
 * so mutation is structurally impossible on the decision side; these tests
 * additionally verify the read-only-ness of the run-creation side
 * (HitlAgentReviewService), and drive both services together to confirm the
 * shared TicketService mock is only ever read, never written to, across a
 * full run -> approve / reject / request-revision cycle.
 */
@ExtendWith(MockitoExtension.class)
class HitlNoMutationTest {

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
    @Mock private HitlRevisionService revisionService;

    private HitlAgentReviewService hitlAgentReviewService;
    private HumanReviewDecisionService humanReviewDecisionService;

    private static final Long TICKET_ID = 1L;
    private static final Long RUN_ID = 100L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        hitlAgentReviewService = new HitlAgentReviewService(
                chatClient, ticketService, ticketLookupTool, ticketEvidenceTool,
                previousAiReviewTool, boundaryTool, promptBuilder,
                new PromptInjectionGuard(), validator,
                agentRunRepository, agentToolCallRepository,
                checkpointService, promptStateSerializer, objectMapper
        );
        humanReviewDecisionService = new HumanReviewDecisionService(
                agentRunRepository, checkpointService, revisionService, objectMapper
        );

        Ticket ticket = mock(Ticket.class);
        lenient().when(ticket.getId()).thenReturn(TICKET_ID);
        lenient().when(ticket.getTitle()).thenReturn("Conveyor motor overheating");
        lenient().when(ticket.getDescription()).thenReturn("Motor temperature increases after 20 minutes.");
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        lenient().when(ticketService.findById(TICKET_ID)).thenReturn(ticket);

        lenient().when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(inv -> {
            AgentRun run = inv.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", RUN_ID);
            }
            return run;
        });
        lenient().when(agentToolCallRepository.save(any(AgentToolCall.class))).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(ticketLookupTool.lookup(TICKET_ID)).thenReturn(
                TicketLookupResult.of(TICKET_ID, "Conveyor motor overheating",
                        "Motor temperature increases after 20 minutes.", "OPEN"));
        lenient().when(ticketEvidenceTool.retrieve(any(Ticket.class), anyInt()))
                .thenReturn(TicketEvidenceResult.of(TICKET_ID, List.of(
                        EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9))));
        lenient().when(previousAiReviewTool.loadRecent(eq(TICKET_ID), anyInt()))
                .thenReturn(PreviousAiReviewResult.of(TICKET_ID, List.of()));
        lenient().when(boundaryTool.load()).thenReturn(
                RecommendationBoundaryResult.of(List.of("inspect equipment"), List.of("close ticket")));
        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        lenient().when(promptBuilder.buildTaskPrompt(any(), anyString(), any(), any(), any(), any())).thenReturn("task");
        lenient().when(promptBuilder.version()).thenReturn("ticket-agent-investigation-v1");
        lenient().when(promptStateSerializer.serialize(any(), any(), any(), any(), any())).thenReturn("{}");

        lenient().when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(HitlDraft.class))
                .thenReturn(validDraft());
        lenient().doNothing().when(validator).validate(any(), any());

        lenient().when(checkpointService.createInitialCheckpoint(anyLong(), eq(TICKET_ID), anyString(), anyString(), anyString()))
                .thenReturn(pendingSnapshot());
    }

    private HitlDraft validDraft() {
        HitlDraft d = new HitlDraft();
        d.setInvestigationSummary("Motor overheating likely caused by ventilation blockage.");
        d.setEvidenceRefs(List.of());
        d.setRecommendedNextSteps(List.of("inspect equipment"));
        d.setDraftTechnicianResponse("Please inspect the cooling vent.");
        d.setConfidence(Confidence.MEDIUM);
        d.setLimitations(List.of());
        d.setNeedsHumanReview(true);
        return d;
    }

    private CheckpointSnapshot pendingSnapshot() {
        com.genai.java.spring.hitl.AgentReviewCheckpoint entity = new com.genai.java.spring.hitl.AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setAgentRunId(RUN_ID);
        entity.setTicketId(TICKET_ID);
        entity.setCheckpointNumber(1);
        entity.setStatus(ReviewCheckpointStatus.PENDING);
        entity.setDraftJson("{\"needsHumanReview\":true}");
        return CheckpointSnapshot.from(entity);
    }

    private AgentRun waitingRun() {
        AgentRun run = new AgentRun();
        ReflectionTestUtils.setField(run, "id", RUN_ID);
        run.setTicketId(TICKET_ID);
        run.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        run.setCreatedAt(LocalDateTime.now());
        return run;
    }

    @Test
    @DisplayName("ticket status is unchanged after HITL run creation")
    void hitlRunCreation_doesNotMutateTicket() {
        hitlAgentReviewService.startReview(TICKET_ID, new HitlReviewRequest());

        verify(ticketService, atLeastOnce()).findById(TICKET_ID);
        verifyNoMoreInteractions(ticketService);
    }

    @Test
    @DisplayName("ticket status is unchanged after human approval")
    void approve_doesNotMutateTicket() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(waitingRun()));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot()));
        when(checkpointService.finalizeCheckpoint(eq(500L), any(), anyString())).thenReturn(pendingSnapshot());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        humanReviewDecisionService.applyDecision(RUN_ID, request);

        // HumanReviewDecisionService has no TicketService dependency at all —
        // mutation is structurally impossible, not just untested.
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("ticket status is unchanged after human rejection")
    void reject_doesNotMutateTicket() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(waitingRun()));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot()));
        when(checkpointService.rejectCheckpoint(eq(500L), anyString())).thenReturn(pendingSnapshot());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REJECT);
        request.setComment("Not accurate enough.");

        humanReviewDecisionService.applyDecision(RUN_ID, request);

        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("ticket status is unchanged after request-revision")
    void requestRevision_doesNotMutateTicket() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(waitingRun()));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot()));
        when(checkpointService.supersedeCheckpoint(eq(500L), anyString())).thenReturn(pendingSnapshot());
        when(checkpointService.createRevisedCheckpoint(eq(RUN_ID), eq(TICKET_ID), eq(2), anyString(), any(), any()))
                .thenReturn(pendingSnapshot());
        when(revisionService.generateRevisedDraft(eq(TICKET_ID), anyString(), anyString()))
                .thenReturn(validDraft());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REQUEST_REVISION);
        request.setComment("Please add more detail.");

        humanReviewDecisionService.applyDecision(RUN_ID, request);

        verifyNoInteractions(ticketService);
    }

    @ParameterizedTest(name = "no ticket mutation for decision {0}")
    @EnumSource(HumanReviewDecision.class)
    @DisplayName("no decision path ever calls TicketService.updateStatus or delete")
    void noDecisionPath_everCallsUpdateOrDelete(HumanReviewDecision decision) {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(waitingRun()));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot()));
        lenient().when(checkpointService.finalizeCheckpoint(anyLong(), any(), anyString())).thenReturn(pendingSnapshot());
        lenient().when(checkpointService.rejectCheckpoint(anyLong(), anyString())).thenReturn(pendingSnapshot());
        lenient().when(checkpointService.supersedeCheckpoint(anyLong(), anyString())).thenReturn(pendingSnapshot());
        lenient().when(checkpointService.createRevisedCheckpoint(anyLong(), anyLong(), anyInt(), anyString(), any(), any()))
                .thenReturn(pendingSnapshot());
        lenient().when(revisionService.generateRevisedDraft(anyLong(), anyString(), anyString())).thenReturn(validDraft());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(decision);
        request.setComment(decision == HumanReviewDecision.APPROVE ? null : "required comment");

        humanReviewDecisionService.applyDecision(RUN_ID, request);

        verifyNoInteractions(ticketService);
    }
}
