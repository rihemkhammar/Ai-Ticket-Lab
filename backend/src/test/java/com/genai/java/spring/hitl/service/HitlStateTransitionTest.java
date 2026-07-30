package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.AgentRun;
import com.genai.java.spring.agent.AgentRunRepository;
import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.HitlValidationException;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import com.genai.java.spring.hitl.dto.HumanReviewDecisionRequest;
import com.genai.java.spring.observability.AiWorkflowLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * HITL state-machine transition rules.
 *
 * Only WAITING_FOR_HUMAN -> {FINALIZED, REJECTED, REVISING} are reachable
 * through a human decision. Every other starting status must be rejected
 * with a 422-mapped HitlValidationException, and no checkpoint/run mutation
 * must occur when that happens.
 */
@ExtendWith(MockitoExtension.class)
class HitlStateTransitionTest {

    @Mock private AgentRunRepository agentRunRepository;
    @Mock private AgentReviewCheckpointService checkpointService;
    @Mock private HitlRevisionService revisionService;

    private HumanReviewDecisionService service;

    private static final Long RUN_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new HumanReviewDecisionService(agentRunRepository, checkpointService, revisionService, new ObjectMapper(), new AiWorkflowLogger());
    }

    private AgentRun runWithStatus(AgentRunStatus status) {
        AgentRun run = new AgentRun();
        ReflectionTestUtils.setField(run, "id", RUN_ID);
        run.setTicketId(1L);
        run.setStatus(status);
        run.setCreatedAt(java.time.LocalDateTime.now().minusMinutes(5));
        return run;
    }

    @ParameterizedTest(name = "decision rejected when run status is {0}")
    @EnumSource(value = AgentRunStatus.class, names = {"RUNNING", "REVISING", "FINALIZED", "REJECTED", "FAILED"})
    @DisplayName("a decision is rejected whenever the run is not WAITING_FOR_HUMAN")
    void applyDecision_rejectedForAnyNonWaitingStatus(AgentRunStatus status) {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(runWithStatus(status)));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class)
                .hasMessageContaining("not WAITING_FOR_HUMAN");

        verifyNoInteractions(checkpointService);
        verifyNoInteractions(revisionService);
    }

    @Test
    @DisplayName("a decision on an unknown run id is rejected")
    void applyDecision_unknownRun_throws() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.empty());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class);

        verifyNoInteractions(checkpointService);
    }

    @Test
    @DisplayName("a decision when WAITING_FOR_HUMAN but with no pending checkpoint is rejected")
    void applyDecision_noPendingCheckpoint_throws() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(runWithStatus(AgentRunStatus.WAITING_FOR_HUMAN)));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.empty());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class)
                .hasMessageContaining("no pending checkpoint");
    }

    @Test
    @DisplayName("a null decision is rejected")
    void applyDecision_nullDecision_throws() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(runWithStatus(AgentRunStatus.WAITING_FOR_HUMAN)));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot(1)));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(null);

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class)
                .hasMessageContaining("decision is required");
    }

    @Test
    @DisplayName("APPROVE from WAITING_FOR_HUMAN transitions the run to FINALIZED")
    void applyDecision_approve_transitionsToFinalized() {
        AgentRun run = runWithStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(run));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot(1)));
        when(checkpointService.finalizeCheckpoint(eq(500L), any(), anyString()))
                .thenReturn(finalizedSnapshot());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        service.applyDecision(RUN_ID, request);

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.FINALIZED);
    }

    @Test
    @DisplayName("REJECT from WAITING_FOR_HUMAN transitions the run to REJECTED")
    void applyDecision_reject_transitionsToRejected() {
        AgentRun run = runWithStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(run));
        when(checkpointService.findPendingCheckpoint(RUN_ID)).thenReturn(Optional.of(pendingSnapshot(1)));
        when(checkpointService.rejectCheckpoint(eq(500L), anyString())).thenReturn(rejectedSnapshot());

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REJECT);
        request.setComment("Not accurate enough.");

        service.applyDecision(RUN_ID, request);

        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.REJECTED);
    }

    @Test
    @DisplayName("FINALIZED -> WAITING_FOR_HUMAN is not reachable through a decision")
    void applyDecision_finalizedRunCannotBeReDecided() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(runWithStatus(AgentRunStatus.FINALIZED)));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REQUEST_REVISION);
        request.setComment("One more pass.");

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class);
    }

    @Test
    @DisplayName("FAILED -> FINALIZED is not reachable through a decision")
    void applyDecision_failedRunCannotBeApproved() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(runWithStatus(AgentRunStatus.FAILED)));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        assertThatThrownBy(() -> service.applyDecision(RUN_ID, request))
                .isInstanceOf(HitlValidationException.class);
    }

    private CheckpointSnapshot pendingSnapshot(int checkpointNumber) {
        com.genai.java.spring.hitl.AgentReviewCheckpoint entity = new com.genai.java.spring.hitl.AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setAgentRunId(RUN_ID);
        entity.setTicketId(1L);
        entity.setCheckpointNumber(checkpointNumber);
        entity.setStatus(ReviewCheckpointStatus.PENDING);
        entity.setDraftJson("{\"investigationSummary\":\"x\",\"needsHumanReview\":true}");
        return CheckpointSnapshot.from(entity);
    }

    private CheckpointSnapshot finalizedSnapshot() {
        com.genai.java.spring.hitl.AgentReviewCheckpoint entity = new com.genai.java.spring.hitl.AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setStatus(ReviewCheckpointStatus.FINALIZED);
        entity.setHumanDecision(HumanReviewDecision.APPROVE);
        return CheckpointSnapshot.from(entity);
    }

    private CheckpointSnapshot rejectedSnapshot() {
        com.genai.java.spring.hitl.AgentReviewCheckpoint entity = new com.genai.java.spring.hitl.AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setStatus(ReviewCheckpointStatus.REJECTED);
        entity.setHumanDecision(HumanReviewDecision.REJECT);
        return CheckpointSnapshot.from(entity);
    }
}