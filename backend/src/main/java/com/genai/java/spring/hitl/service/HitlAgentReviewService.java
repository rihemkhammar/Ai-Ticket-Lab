package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.genai.java.spring.agent.*;
import com.genai.java.spring.agent.dto.AgentToolCallTrace;
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
import com.genai.java.spring.hitl.dto.CheckpointSnapshot;
import com.genai.java.spring.hitl.dto.HitlDraft;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.prompt.AgentPromptStateSerializer;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.shared.advisor.PromptInjectionGuard;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Phase 1-2 orchestration of the HITL flow :
 *
 *  1. Create agent_run RUNNING
 *  2. Run the same read-only M4 tool chain (lookup, evidence, previous
 *     reviews, boundaries) reused as-is from the agent package
 *  3. GPT drafts a recommendation (needsHumanReview always true)
 *  4. Validate draft (reuses AgentOutputValidator)
 *  5. Persist checkpoint #1 as PENDING (draft + serialized prompt/state +
 *     tool trace snapshot)
 *  6. Move agent_run to WAITING_FOR_HUMAN
 *
 * Unlike TicketAgentInvestigationService, this service NEVER finalizes the
 * run itself — it always stops at the pause point. Finalization only
 * happens later, in HumanReviewDecisionService, after a human decision.
 */
@Slf4j
@Service
public class HitlAgentReviewService {

    private static final String MODEL_NAME = "openai/gpt-oss-20b";

    private final ChatClient chatClient;
    private final TicketService ticketService;
    private final TicketLookupTool ticketLookupTool;
    private final TicketEvidenceTool ticketEvidenceTool;
    private final PreviousAiReviewTool previousAiReviewTool;
    private final TicketRecommendationBoundaryTool boundaryTool;
    private final TicketAgentPromptBuilder promptBuilder;
    private final PromptInjectionGuard promptInjectionGuard;
    private final AgentOutputValidator validator;
    private final AgentRunRepository agentRunRepository;
    private final AgentToolCallRepository agentToolCallRepository;
    private final AgentReviewCheckpointService checkpointService;
    private final AgentPromptStateSerializer promptStateSerializer;
    private final ObjectMapper objectMapper;

    public HitlAgentReviewService(@Qualifier("openAIChatClient") ChatClient chatClient,
                                  TicketService ticketService,
                                  TicketLookupTool ticketLookupTool,
                                  TicketEvidenceTool ticketEvidenceTool,
                                  PreviousAiReviewTool previousAiReviewTool,
                                  TicketRecommendationBoundaryTool boundaryTool,
                                  TicketAgentPromptBuilder promptBuilder,
                                  PromptInjectionGuard promptInjectionGuard,
                                  AgentOutputValidator validator,
                                  AgentRunRepository agentRunRepository,
                                  AgentToolCallRepository agentToolCallRepository,
                                  AgentReviewCheckpointService checkpointService,
                                  AgentPromptStateSerializer promptStateSerializer,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ticketService = ticketService;
        this.ticketLookupTool = ticketLookupTool;
        this.ticketEvidenceTool = ticketEvidenceTool;
        this.previousAiReviewTool = previousAiReviewTool;
        this.boundaryTool = boundaryTool;
        this.promptBuilder = promptBuilder;
        this.promptInjectionGuard = promptInjectionGuard;
        this.validator = validator;
        this.agentRunRepository = agentRunRepository;
        this.agentToolCallRepository = agentToolCallRepository;
        this.checkpointService = checkpointService;
        this.promptStateSerializer = promptStateSerializer;
        // Tool-call traces carry LocalDateTime fields; make a defensive copy that
        // is guaranteed to support Java 8 date/time types, rather than relying on
        // the injected ObjectMapper already having jackson-datatype-jsr310
        // registered (it normally does via Spring Boot autoconfiguration, but we
        // don't want a silent serialization failure here to corrupt a checkpoint).
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public HitlReviewResponse startReview(Long ticketId, HitlReviewRequest request) {

        if (!ticketExists(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        AgentRun run = new AgentRun();
        run.setTicketId(ticketId);
        run.setPromptVersion(promptBuilder.version());
        run.setModelName(MODEL_NAME);
        run.setStatus(AgentRunStatus.RUNNING);
        run.setCreatedAt(LocalDateTime.now());
        run = agentRunRepository.save(run);

        List<AgentToolCallTrace> traceForResponse = new ArrayList<>();

        try {
            TicketLookupResult ticketResult = runTool(run.getId(), TicketLookupTool.NAME,
                    ticketId, () -> ticketLookupTool.lookup(ticketId), traceForResponse);

            Ticket ticket = ticketService.findById(ticketId);

            List<String> injectionFlags = promptInjectionGuard.scan(ticket.getTitle(), ticket.getDescription());
            if (!injectionFlags.isEmpty()) {
                log.warn("[HitlAgentReviewService] SUSPICIOUS ticketId={} patterns={}", ticketId, injectionFlags);
            }

            TicketEvidenceResult evidenceResult = runTool(run.getId(), TicketEvidenceTool.NAME,
                    ticketId, () -> ticketEvidenceTool.retrieve(ticket, request.getTopK()), traceForResponse);
            List<EvidenceChunkResponse> evidence = evidenceResult.getEvidence();

            PreviousAiReviewResult previousReviews;
            if (request.getIncludePreviousReviews()) {
                previousReviews = runTool(run.getId(), PreviousAiReviewTool.NAME,
                        ticketId, () -> previousAiReviewTool.loadRecent(ticketId, 3), traceForResponse);
            } else {
                previousReviews = PreviousAiReviewResult.of(ticketId, Collections.emptyList());
            }

            RecommendationBoundaryResult boundaries = runTool(run.getId(),
                    TicketRecommendationBoundaryTool.NAME, ticketId, boundaryTool::load, traceForResponse);

            String systemPrompt = promptBuilder.buildSystemPrompt();
            String taskPrompt = promptBuilder.buildTaskPrompt(
                    ticketId, request.getUserGoal(), ticketResult, evidence, previousReviews, boundaries);

            HitlDraft draft;
            try {
                draft = chatClient.prompt()
                        .system(systemPrompt)
                        .user(taskPrompt)
                        .call()
                        .entity(HitlDraft.class);
                log.info("HITL draft parsed for ticketId={} -> {}", ticketId, draft);
            } catch (Exception e) {
                log.error("HITL GPT draft generation failed for ticketId={}", ticketId, e);
                return failRun(run, "AI provider failed or returned invalid output.", traceForResponse);
            }

            try {
                validator.validate(toSynthesis(draft), evidence);
            } catch (AgentValidationException e) {
                log.warn("HITL draft rejected for ticketId={} reason={}", ticketId, e.getMessage());
                return failRun(run, e.getMessage(), traceForResponse);
            }

            String serializedPromptJson = promptStateSerializer.serialize(
                    request.getUserGoal(), evidence, previousReviews, boundaries, MODEL_NAME);
            String toolTraceJson = safeWrite(traceForResponse);
            String draftJson = safeWrite(draft);

            CheckpointSnapshot checkpoint = checkpointService.createInitialCheckpoint(
                    run.getId(), ticketId, draftJson, serializedPromptJson, toolTraceJson);

            AgentRun waitingRun = new AgentRun(run);
            waitingRun.setStatus(AgentRunStatus.WAITING_FOR_HUMAN);
            agentRunRepository.save(waitingRun);

            return toPendingResponse(waitingRun, checkpoint, draft, traceForResponse);

        } catch (AgentToolException e) {
            log.warn("HITL agent tool failure for ticketId={} reason={}", ticketId, e.getMessage());
            return failRun(run, e.getMessage(), traceForResponse);
        } catch (Exception e) {
            log.error("Unexpected HITL failure for ticketId={}", ticketId, e);
            return failRun(run, "HITL agent review failed unexpectedly.", traceForResponse);
        }
    }

    /** Reloads the latest checkpoint of a run without re-running anything (used on page refresh). */
    public Optional<HitlReviewResponse> reloadPendingReview(Long runId) {
        AgentRun run = agentRunRepository.findById(runId).orElse(null);
        if (run == null) {
            return Optional.empty();
        }
        return checkpointService.findLatestCheckpoint(runId)
                .map(checkpoint -> toPendingResponse(run, checkpoint, readDraft(checkpoint.getDraftJson()), List.of()));
    }

    /**
     * Reloads the latest HITL run for a ticket (by latest checkpoint), so the
     * frontend can redisplay a pending/finalized/rejected review after
     * navigating away and back, or after a page refresh (S5 §2.7). Returns
     * empty if the ticket's latest agent_run has no HITL checkpoint (e.g. it
     * was a plain M4 investigation run).
     */
    public Optional<HitlReviewResponse> getLatestReview(Long ticketId) {
        return agentRunRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId)
                .flatMap(run -> checkpointService.findLatestCheckpoint(run.getId())
                        .map(checkpoint -> toPendingResponse(run, checkpoint, readDraft(checkpoint.getDraftJson()), List.of())));
    }

    private HitlReviewResponse toPendingResponse(AgentRun run, CheckpointSnapshot checkpoint, HitlDraft draft,
                                                 List<AgentToolCallTrace> traceForResponse) {
        HitlReviewResponse response = new HitlReviewResponse();
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setCheckpointId(checkpoint.getCheckpointId());
        response.setCheckpointNumber(checkpoint.getCheckpointNumber());
        response.setStatus(run.getStatus());
        response.setCheckpointStatus(checkpoint.getStatus());
        response.setToolCalls(traceForResponse);
        response.setCreatedAt(run.getCreatedAt());
        if (draft != null) {
            response.setInvestigationSummary(draft.getInvestigationSummary());
            response.setEvidenceRefs(draft.getEvidenceRefs());
            response.setPreviousReviewSummary(draft.getPreviousReviewSummary());
            response.setRecommendedNextSteps(draft.getRecommendedNextSteps());
            response.setDraftTechnicianResponse(draft.getDraftTechnicianResponse());
            response.setConfidence(draft.getConfidence());
            response.setLimitations(draft.getLimitations());
            response.setNeedsHumanReview(draft.getNeedsHumanReview());
        }
        response.setHumanDecision(checkpoint.getHumanDecision());
        response.setHumanComment(checkpoint.getHumanComment());
        if (checkpoint.getFinalReviewedResultJson() != null) {
            response.setFinalReviewedResult(readFinalResult(checkpoint.getFinalReviewedResultJson()));
        }
        return response;
    }

    private com.genai.java.spring.hitl.dto.FinalReviewedResult readFinalResult(String json) {
        try {
            return objectMapper.readValue(json, com.genai.java.spring.hitl.dto.FinalReviewedResult.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize stored HITL final reviewed result", e);
            return null;
        }
    }

    private HitlReviewResponse failRun(AgentRun run, String errorMessage, List<AgentToolCallTrace> trace) {
        // Save a distinct copy rather than mutating the already-saved RUNNING
        // instance in place, so the RUNNING snapshot persisted earlier stays
        // genuinely intact (and so ArgumentCaptor-based assertions on the
        // sequence of saved statuses see two different objects, not the same
        // mutated reference twice).
        AgentRun failedRun = new AgentRun(run);
        failedRun.setStatus(AgentRunStatus.FAILED);
        failedRun.setErrorMessage(errorMessage);
        failedRun.setCompletedAt(LocalDateTime.now());
        agentRunRepository.save(failedRun);

        HitlReviewResponse response = new HitlReviewResponse();
        response.setRunId(failedRun.getId());
        response.setTicketId(failedRun.getTicketId());
        response.setStatus(AgentRunStatus.FAILED);
        response.setErrorMessage(errorMessage);
        response.setToolCalls(trace);
        response.setCreatedAt(failedRun.getCreatedAt());
        response.setNeedsHumanReview(true);
        return response;
    }

    private boolean ticketExists(Long ticketId) {
        try {
            ticketService.findById(ticketId);
            return true;
        } catch (TicketNotFoundException e) {
            return false;
        }
    }

    private <T> T runTool(Long runId, String toolName, Long ticketId,
                          ToolCall<T> call, List<AgentToolCallTrace> traceForResponse) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            T output = call.execute();
            LocalDateTime completedAt = LocalDateTime.now();
            persistToolCall(runId, toolName, ticketId, output, AgentToolCallStatus.SUCCESS, null, startedAt);
            traceForResponse.add(new AgentToolCallTrace(toolName, AgentToolCallStatus.SUCCESS.name(), null,
                    startedAt, completedAt));
            return output;
        } catch (AgentToolException e) {
            LocalDateTime completedAt = LocalDateTime.now();
            persistToolCall(runId, toolName, ticketId, null, AgentToolCallStatus.FAILED, e.getMessage(), startedAt);
            traceForResponse.add(new AgentToolCallTrace(toolName, AgentToolCallStatus.FAILED.name(), e.getMessage(),
                    startedAt, completedAt));
            throw e;
        }
    }

    private void persistToolCall(Long runId, String toolName, Long ticketId, Object output,
                                 AgentToolCallStatus status, String errorMessage, LocalDateTime startedAt) {
        AgentToolCall call = new AgentToolCall();
        call.setAgentRunId(runId);
        call.setToolName(toolName);
        call.setInputJson(safeWrite(Collections.singletonMap("ticketId", ticketId)));
        call.setOutputJson(output != null ? safeWrite(output) : null);
        call.setStatus(status);
        call.setErrorMessage(errorMessage);
        call.setStartedAt(startedAt);
        call.setCompletedAt(LocalDateTime.now());
        agentToolCallRepository.save(call);
    }

    private String safeWrite(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize HITL payload", e);
            return null;
        }
    }

    private HitlDraft readDraft(String draftJson) {
        try {
            return objectMapper.readValue(draftJson, HitlDraft.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize stored HITL draft", e);
            return null;
        }
    }

    /** Bridges HitlDraft to the M4 synthesis shape so AgentOutputValidator can be reused unchanged. */
    private com.genai.java.spring.agent.dto.TicketAgentSynthesisResult toSynthesis(HitlDraft draft) {
        com.genai.java.spring.agent.dto.TicketAgentSynthesisResult synthesis =
                new com.genai.java.spring.agent.dto.TicketAgentSynthesisResult();
        synthesis.setInvestigationSummary(draft.getInvestigationSummary());
        synthesis.setEvidenceRefs(draft.getEvidenceRefs());
        synthesis.setPreviousReviewSummary(draft.getPreviousReviewSummary());
        synthesis.setRecommendedNextSteps(draft.getRecommendedNextSteps());
        synthesis.setDraftTechnicianResponse(draft.getDraftTechnicianResponse());
        synthesis.setConfidence(draft.getConfidence());
        synthesis.setLimitations(draft.getLimitations());
        synthesis.setNeedsHumanReview(draft.getNeedsHumanReview());
        return synthesis;
    }

    @FunctionalInterface
    private interface ToolCall<T> {
        T execute();
    }
}