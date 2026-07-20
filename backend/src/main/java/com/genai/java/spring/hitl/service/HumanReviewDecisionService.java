package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.AgentRun;
import com.genai.java.spring.agent.AgentRunRepository;
import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.HitlValidationException;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.hitl.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 *  orchestration: applies a human decision to the pending
 * checkpoint of a HITL run .
 *
 * One unified entry point (applyDecision) branches into approve / reject /
 * request-revision, each enforcing its own comment and state-transition
 * rules. The ticket itself is never touched — see the mandatory
 * "no mutation" guardrail: TicketService is not even injected here.
 */
@Slf4j
@Service
public class HumanReviewDecisionService {

    /**  only one revision cycle is supported in this milestone. */
    private static final int MAX_REVISION_CYCLES = 1;

    private final AgentRunRepository agentRunRepository;
    private final AgentReviewCheckpointService checkpointService;
    private final HitlRevisionService revisionService;
    private final ObjectMapper objectMapper;

    public HumanReviewDecisionService(AgentRunRepository agentRunRepository,
                                      AgentReviewCheckpointService checkpointService,
                                      HitlRevisionService revisionService,
                                      ObjectMapper objectMapper) {
        this.agentRunRepository = agentRunRepository;
        this.checkpointService = checkpointService;
        this.revisionService = revisionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public HumanReviewDecisionResponse applyDecision(Long runId, HumanReviewDecisionRequest request) {
        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new HitlValidationException("Agent run not found: " + runId));

        if (run.getStatus() != AgentRunStatus.WAITING_FOR_HUMAN) {
            throw new HitlValidationException(
                    "Decision rejected: agent run " + runId + " is not WAITING_FOR_HUMAN (current status: "
                            + run.getStatus() + ").");
        }

        CheckpointSnapshot pending = checkpointService.findPendingCheckpoint(runId)
                .orElseThrow(() -> new HitlValidationException(
                        "Decision rejected: no pending checkpoint for agent run " + runId + "."));

        HumanReviewDecision decision = request.getDecision();
        if (decision == null) {
            throw new HitlValidationException("decision is required and must be one of APPROVE, REJECT, REQUEST_REVISION.");
        }

        return switch (decision) {
            case APPROVE -> approve(run, pending, request.getComment());
            case REJECT -> reject(run, pending, requireComment(request.getComment(), "reject"));
            case REQUEST_REVISION -> requestRevision(run, pending, requireComment(request.getComment(), "request revision"));
        };
    }

    // ---- APPROVE  --------------------------------------------------

    private HumanReviewDecisionResponse approve(AgentRun run, CheckpointSnapshot pending, String comment) {
        HitlDraft draft = readDraft(pending.getDraftJson());

        FinalReviewedResult result = new FinalReviewedResult();
        result.setInvestigationSummary(draft.getInvestigationSummary());
        result.setEvidenceRefs(draft.getEvidenceRefs());
        result.setRecommendedNextSteps(draft.getRecommendedNextSteps());
        result.setDraftTechnicianResponse(draft.getDraftTechnicianResponse());
        result.setConfidence(draft.getConfidence());
        result.setLimitations(draft.getLimitations());
        result.setHumanReviewed(true);
        result.setHumanDecision(HumanReviewDecision.APPROVE);
        // Mandatory safety invariants (S5 §2.4 / §2.6) — never true in this milestone.
        result.setOfficialActionExecuted(false);
        result.setTicketStatusChanged(false);

        String finalJson = safeWrite(result);
        checkpointService.finalizeCheckpoint(pending.getCheckpointId(), comment, finalJson);

        run.setStatus(AgentRunStatus.FINALIZED);
        run.setCompletedAt(LocalDateTime.now());
        agentRunRepository.save(run);

        HumanReviewDecisionResponse response = baseResponse(run, pending.getCheckpointId(),
                HumanReviewDecision.APPROVE, comment);
        response.setFinalStatus(AgentRunStatus.FINALIZED);
        response.setFinalReviewedResult(result);
        response.setHumanReviewed(true);
        response.setOfficialActionExecuted(false);
        response.setTicketStatusChanged(false);
        response.setFinalizedAt(LocalDateTime.now());
        return response;
    }

    // ---- REJECT ---------------------------------------------------

    private HumanReviewDecisionResponse reject(AgentRun run, CheckpointSnapshot pending, String comment) {
        checkpointService.rejectCheckpoint(pending.getCheckpointId(), comment);

        run.setStatus(AgentRunStatus.REJECTED);
        run.setCompletedAt(LocalDateTime.now());
        agentRunRepository.save(run);

        HumanReviewDecisionResponse response = baseResponse(run, pending.getCheckpointId(),
                HumanReviewDecision.REJECT, comment);
        response.setFinalStatus(AgentRunStatus.REJECTED);
        response.setHumanReviewed(true);
        response.setOfficialActionExecuted(false);
        response.setTicketStatusChanged(false);
        response.setFinalizedAt(LocalDateTime.now());
        return response;
    }

    // ---- REQUEST_REVISION ---------------------------

    private HumanReviewDecisionResponse requestRevision(AgentRun run, CheckpointSnapshot pending, String comment) {
        if (pending.getCheckpointNumber() != null && pending.getCheckpointNumber() > MAX_REVISION_CYCLES) {
            throw new HitlValidationException(
                    "Only one revision cycle is supported in this training milestone.");
        }

        run.setStatus(AgentRunStatus.REVISING);
        agentRunRepository.save(run);

        // Persist the human REQUEST_REVISION decision + comment FIRST, before attempting
        // GPT revision generation. This guarantees the human decision and checkpoint
        // history survive even if the revised draft generation fails below (S5-G02).
        checkpointService.supersedeCheckpoint(pending.getCheckpointId(), comment);

        HitlDraft revisedDraft;
        try {
            revisedDraft = revisionService.generateRevisedDraft(
                    run.getTicketId(), comment, pending.getDraftJson());
        } catch (HitlRevisionService.RevisionFailedException e) {
            log.warn("HITL revision failed for runId={} after retry: {}", run.getId(), e.getMessage());
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(LocalDateTime.now());
            agentRunRepository.save(run);
            // The REQUEST_REVISION decision/comment is already persisted on the
            // superseded checkpoint above — checkpoint history is preserved.
            throw new HitlValidationException(
                    "Revision could not be generated: " + e.getMessage());
        }

        int nextCheckpointNumber = pending.getCheckpointNumber() + 1;
        CheckpointSnapshot revisedCheckpoint = checkpointService.createRevisedCheckpoint(
                run.getId(), run.getTicketId(), nextCheckpointNumber,
                safeWrite(revisedDraft), pending.getSerializedPromptJson(), pending.getToolTraceSnapshotJson());

        run.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        agentRunRepository.save(run);

        HitlReviewResponse revisedReview = new HitlReviewResponse();
        revisedReview.setRunId(run.getId());
        revisedReview.setTicketId(run.getTicketId());
        revisedReview.setCheckpointId(revisedCheckpoint.getCheckpointId());
        revisedReview.setCheckpointNumber(revisedCheckpoint.getCheckpointNumber());
        revisedReview.setStatus(run.getStatus());
        revisedReview.setCheckpointStatus(revisedCheckpoint.getStatus());
        revisedReview.setInvestigationSummary(revisedDraft.getInvestigationSummary());
        revisedReview.setEvidenceRefs(revisedDraft.getEvidenceRefs());
        revisedReview.setPreviousReviewSummary(revisedDraft.getPreviousReviewSummary());
        revisedReview.setRecommendedNextSteps(revisedDraft.getRecommendedNextSteps());
        revisedReview.setDraftTechnicianResponse(revisedDraft.getDraftTechnicianResponse());
        revisedReview.setConfidence(revisedDraft.getConfidence());
        revisedReview.setLimitations(revisedDraft.getLimitations());
        revisedReview.setNeedsHumanReview(revisedDraft.getNeedsHumanReview());
        revisedReview.setCreatedAt(LocalDateTime.now());

        HumanReviewDecisionResponse response = baseResponse(run, revisedCheckpoint.getCheckpointId(),
                HumanReviewDecision.REQUEST_REVISION, comment);
        response.setFinalStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        response.setRevisedReview(revisedReview);
        response.setHumanReviewed(false);
        response.setOfficialActionExecuted(false);
        response.setTicketStatusChanged(false);
        return response;
    }

    // ---- helpers --------------------------------------------------------------

    private HumanReviewDecisionResponse baseResponse(AgentRun run, Long checkpointId,
                                                     HumanReviewDecision decision, String comment) {
        HumanReviewDecisionResponse response = new HumanReviewDecisionResponse();
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setCheckpointId(checkpointId);
        response.setHumanDecision(decision);
        response.setHumanComment(comment);
        return response;
    }

    private String requireComment(String comment, String decisionLabel) {
        if (comment == null || comment.isBlank()) {
            throw new HitlValidationException("comment is required for " + decisionLabel + ".");
        }
        return comment;
    }

    private HitlDraft readDraft(String draftJson) {
        try {
            return objectMapper.readValue(draftJson, HitlDraft.class);
        } catch (Exception e) {
            throw new HitlValidationException("Stored checkpoint draft could not be read.");
        }
    }

    private String safeWrite(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize HITL decision payload", e);
            return null;
        }
    }
}