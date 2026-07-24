package com.genai.java.spring.hitl.service;

import com.genai.java.spring.hitl.AgentReviewCheckpoint;
import com.genai.java.spring.hitl.AgentReviewCheckpointRepository;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Owns all reads/writes of {@link AgentReviewCheckpoint} rows .
 *
 * Responsibilities:
 *  - create the first PENDING checkpoint of a HITL run
 *  - create a revised PENDING checkpoint (superseding the previous one)
 *  - find the current pending checkpoint of a run
 *  - apply a human decision (finalize / reject / mark superseded)
 *
 * This service never talks to the LLM and never touches AgentRun directly
 * — it is a thin, focused persistence layer so HitlAgentReviewService /
 * HumanReviewDecisionService / HitlRevisionService can stay orchestration-only.
 */
@Service
public class AgentReviewCheckpointService {

    private final AgentReviewCheckpointRepository repository;

    public AgentReviewCheckpointService(AgentReviewCheckpointRepository repository) {
        this.repository = repository;
    }

    /** Creates the first checkpoint (checkpointNumber = 1) of a new HITL run. */
    public CheckpointSnapshot createInitialCheckpoint(Long agentRunId, Long ticketId, String traceId,
                                                      String draftJson, String serializedPromptJson,
                                                      String toolTraceSnapshotJson) {
        return createCheckpoint(agentRunId, ticketId, traceId, 1, draftJson, serializedPromptJson, toolTraceSnapshotJson);
    }

    /** Creates a revised checkpoint after REQUEST_REVISION, incrementing the checkpoint number. */
    public CheckpointSnapshot createRevisedCheckpoint(Long agentRunId, Long ticketId, String traceId,
                                                      int nextCheckpointNumber, String draftJson,
                                                      String serializedPromptJson, String toolTraceSnapshotJson) {
        return createCheckpoint(agentRunId, ticketId, traceId, nextCheckpointNumber, draftJson,
                serializedPromptJson, toolTraceSnapshotJson);
    }

    private CheckpointSnapshot createCheckpoint(Long agentRunId, Long ticketId, String traceId, int checkpointNumber,
                                                String draftJson, String serializedPromptJson,
                                                String toolTraceSnapshotJson) {
        AgentReviewCheckpoint checkpoint = new AgentReviewCheckpoint();
        checkpoint.setAgentRunId(agentRunId);
        checkpoint.setTicketId(ticketId);
        checkpoint.setTraceId(traceId);
        checkpoint.setCheckpointNumber(checkpointNumber);
        checkpoint.setStatus(ReviewCheckpointStatus.PENDING);
        checkpoint.setDraftJson(draftJson);
        checkpoint.setSerializedPromptJson(serializedPromptJson);
        checkpoint.setToolTraceSnapshotJson(toolTraceSnapshotJson);
        checkpoint.setCreatedAt(LocalDateTime.now());
        checkpoint.setUpdatedAt(LocalDateTime.now());
        checkpoint = repository.save(checkpoint);
        return CheckpointSnapshot.from(checkpoint);
    }

    public Optional<CheckpointSnapshot> findPendingCheckpoint(Long agentRunId) {
        return repository.findFirstByAgentRunIdAndStatus(agentRunId, ReviewCheckpointStatus.PENDING)
                .map(CheckpointSnapshot::from);
    }

    public Optional<CheckpointSnapshot> findLatestCheckpoint(Long agentRunId) {
        return repository.findFirstByAgentRunIdOrderByCheckpointNumberDesc(agentRunId)
                .map(CheckpointSnapshot::from);
    }

    /**
     * Latest checkpoint for a ticket across all of its agent_runs (ordered by
     * checkpoint creation time), used to reload the HITL review on page
     * refresh without depending on the ticket's latest agent_run actually
     * being a HITL run .
     */
    public Optional<CheckpointSnapshot> findLatestCheckpointForTicket(Long ticketId) {
        return repository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId)
                .map(CheckpointSnapshot::from);
    }

    public List<CheckpointSnapshot> findAllCheckpoints(Long agentRunId) {
        return repository.findByAgentRunIdOrderByCheckpointNumberAsc(agentRunId)
                .stream().map(CheckpointSnapshot::from).collect(Collectors.toList());
    }

    /** APPROVE: stores the human decision + comment and finalizes the checkpoint. */
    public CheckpointSnapshot finalizeCheckpoint(Long checkpointId, String comment, String finalReviewedResultJson) {
        AgentReviewCheckpoint checkpoint = load(checkpointId);
        checkpoint.setHumanDecision(HumanReviewDecision.APPROVE);
        checkpoint.setHumanComment(comment);
        checkpoint.setFinalReviewedResultJson(finalReviewedResultJson);
        checkpoint.setStatus(ReviewCheckpointStatus.FINALIZED);
        checkpoint.setUpdatedAt(LocalDateTime.now());
        checkpoint.setCompletedAt(LocalDateTime.now());
        return CheckpointSnapshot.from(repository.save(checkpoint));
    }

    /** REJECT: stores the human decision + required comment and marks the checkpoint REJECTED. */
    public CheckpointSnapshot rejectCheckpoint(Long checkpointId, String comment) {
        AgentReviewCheckpoint checkpoint = load(checkpointId);
        checkpoint.setHumanDecision(HumanReviewDecision.REJECT);
        checkpoint.setHumanComment(comment);
        checkpoint.setStatus(ReviewCheckpointStatus.REJECTED);
        checkpoint.setUpdatedAt(LocalDateTime.now());
        checkpoint.setCompletedAt(LocalDateTime.now());
        return CheckpointSnapshot.from(repository.save(checkpoint));
    }

    /** REQUEST_REVISION: stores the human decision + required comment, marks checkpoint SUPERSEDED. */
    public CheckpointSnapshot supersedeCheckpoint(Long checkpointId, String comment) {
        AgentReviewCheckpoint checkpoint = load(checkpointId);
        checkpoint.setHumanDecision(HumanReviewDecision.REQUEST_REVISION);
        checkpoint.setHumanComment(comment);
        checkpoint.setStatus(ReviewCheckpointStatus.SUPERSEDED);
        checkpoint.setUpdatedAt(LocalDateTime.now());
        checkpoint.setCompletedAt(LocalDateTime.now());
        return CheckpointSnapshot.from(repository.save(checkpoint));
    }

    private AgentReviewCheckpoint load(Long checkpointId) {
        return repository.findById(checkpointId)
                .orElseThrow(() -> new IllegalStateException("Checkpoint not found: " + checkpointId));
    }
}