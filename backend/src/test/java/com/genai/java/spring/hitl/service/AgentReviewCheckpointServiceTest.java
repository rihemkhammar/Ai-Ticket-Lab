package com.genai.java.spring.hitl.service;

import com.genai.java.spring.hitl.AgentReviewCheckpoint;
import com.genai.java.spring.hitl.AgentReviewCheckpointRepository;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AgentReviewCheckpointService: checkpoint creation, lookup, and
 * human-decision application (finalize / reject / supersede).
 */
@ExtendWith(MockitoExtension.class)
class AgentReviewCheckpointServiceTest {

    @Mock private AgentReviewCheckpointRepository repository;

    private AgentReviewCheckpointService service;

    private static final Long RUN_ID = 10L;
    private static final Long TICKET_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new AgentReviewCheckpointService(repository);

        // repository.save() returns the same entity it was given, with an
        // id assigned (simulates what Hibernate would do on persist).
        lenient().when(repository.save(any(AgentReviewCheckpoint.class))).thenAnswer(inv -> {
            AgentReviewCheckpoint entity = inv.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", 500L);
            }
            return entity;
        });
    }

    @Test
    @DisplayName("createInitialCheckpoint creates checkpoint #1 as PENDING")
    void createInitialCheckpoint_createsPendingCheckpointNumberOne() {
        CheckpointSnapshot snapshot = service.createInitialCheckpoint(
                RUN_ID, TICKET_ID, "trace-1", "{\"draft\":true}", "{\"prompt\":true}", "[{\"tool\":\"lookup\"}]");

        assertThat(snapshot.getCheckpointNumber()).isEqualTo(1);
        assertThat(snapshot.getStatus()).isEqualTo(ReviewCheckpointStatus.PENDING);
        assertThat(snapshot.getAgentRunId()).isEqualTo(RUN_ID);
        assertThat(snapshot.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(snapshot.getDraftJson()).isEqualTo("{\"draft\":true}");

        ArgumentCaptor<AgentReviewCheckpoint> captor = ArgumentCaptor.forClass(AgentReviewCheckpoint.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewCheckpointStatus.PENDING);
        assertThat(captor.getValue().getCheckpointNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("createRevisedCheckpoint creates a new checkpoint at the given (incremented) number")
    void createRevisedCheckpoint_createsCheckpointAtGivenNumber() {
        CheckpointSnapshot snapshot = service.createRevisedCheckpoint(
                RUN_ID, TICKET_ID, "trace-1", 2, "{\"draft\":\"revised\"}", "{\"prompt\":true}", "[]");

        assertThat(snapshot.getCheckpointNumber()).isEqualTo(2);
        assertThat(snapshot.getStatus()).isEqualTo(ReviewCheckpointStatus.PENDING);
    }

    @Test
    @DisplayName("findPendingCheckpoint delegates to repository with PENDING status")
    void findPendingCheckpoint_delegatesToRepository() {
        AgentReviewCheckpoint entity = pendingEntity();
        when(repository.findFirstByAgentRunIdAndStatus(RUN_ID, ReviewCheckpointStatus.PENDING))
                .thenReturn(Optional.of(entity));

        Optional<CheckpointSnapshot> result = service.findPendingCheckpoint(RUN_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ReviewCheckpointStatus.PENDING);
    }

    @Test
    @DisplayName("findPendingCheckpoint returns empty when no pending checkpoint exists")
    void findPendingCheckpoint_returnsEmptyWhenNone() {
        when(repository.findFirstByAgentRunIdAndStatus(RUN_ID, ReviewCheckpointStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThat(service.findPendingCheckpoint(RUN_ID)).isEmpty();
    }

    @Test
    @DisplayName("finalizeCheckpoint stores APPROVE decision, comment, final result, and marks FINALIZED")
    void finalizeCheckpoint_storesDecisionAndFinalizesStatus() {
        AgentReviewCheckpoint entity = pendingEntity();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        CheckpointSnapshot result = service.finalizeCheckpoint(
                entity.getId(), "Approved, looks good.", "{\"humanReviewed\":true}");

        assertThat(result.getStatus()).isEqualTo(ReviewCheckpointStatus.FINALIZED);
        assertThat(result.getHumanDecision()).isEqualTo(HumanReviewDecision.APPROVE);
        assertThat(result.getHumanComment()).isEqualTo("Approved, looks good.");
        assertThat(result.getFinalReviewedResultJson()).isEqualTo("{\"humanReviewed\":true}");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejectCheckpoint stores REJECT decision and required comment, marks REJECTED")
    void rejectCheckpoint_storesDecisionAndRejectsStatus() {
        AgentReviewCheckpoint entity = pendingEntity();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        CheckpointSnapshot result = service.rejectCheckpoint(entity.getId(), "Not accurate enough.");

        assertThat(result.getStatus()).isEqualTo(ReviewCheckpointStatus.REJECTED);
        assertThat(result.getHumanDecision()).isEqualTo(HumanReviewDecision.REJECT);
        assertThat(result.getHumanComment()).isEqualTo("Not accurate enough.");
    }

    @Test
    @DisplayName("supersedeCheckpoint stores REQUEST_REVISION decision and marks SUPERSEDED")
    void supersedeCheckpoint_storesDecisionAndSupersedesStatus() {
        AgentReviewCheckpoint entity = pendingEntity();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        CheckpointSnapshot result = service.supersedeCheckpoint(entity.getId(), "Please add more detail.");

        assertThat(result.getStatus()).isEqualTo(ReviewCheckpointStatus.SUPERSEDED);
        assertThat(result.getHumanDecision()).isEqualTo(HumanReviewDecision.REQUEST_REVISION);
    }

    private AgentReviewCheckpoint pendingEntity() {
        AgentReviewCheckpoint entity = new AgentReviewCheckpoint();
        ReflectionTestUtils.setField(entity, "id", 500L);
        entity.setAgentRunId(RUN_ID);
        entity.setTicketId(TICKET_ID);
        entity.setCheckpointNumber(1);
        entity.setStatus(ReviewCheckpointStatus.PENDING);
        entity.setDraftJson("{\"draft\":true}");
        return entity;
    }
}