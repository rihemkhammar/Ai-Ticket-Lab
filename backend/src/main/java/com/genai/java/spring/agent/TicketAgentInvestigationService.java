package com.genai.java.spring.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.dto.AgentToolCallTrace;
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
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.shared.advisor.PromptInjectionGuard;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TicketAgentInvestigationService .
 *
 *  1. Create agent_run RUNNING
 *  2. Load ticket        (TicketLookupTool)
 *  2b. Scan ticket text for prompt-injection patterns (non-blocking, audit only)
 *  3. Retrieve evidence  (TicketEvidenceTool)
 *  4. Load previous reviews (PreviousAiReviewTool)
 *  5. Load boundaries    (TicketRecommendationBoundaryTool)
 *  6. GPT final synthesis
 *  7. Validate output
 *  8. Update agent_run SUCCESS/FAILED, persist tool-call trace
 *
 * The agent is strictly read-only: it never mutates the ticket.
 */
@Slf4j
@Service
public class TicketAgentInvestigationService {

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
    private final ObjectMapper objectMapper;

    public TicketAgentInvestigationService(@Qualifier("openAIChatClient") ChatClient chatClient,
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
        this.objectMapper = objectMapper;
    }

    public TicketAgentInvestigationResponse investigate(Long ticketId, TicketAgentInvestigationRequest request) {

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

            // Step 2b — anti prompt-injection audit (non-blocking, mêmes patterns
            // que côté aireview via PromptInjectionGuard).
            List<String> injectionFlags = promptInjectionGuard.scan(ticket.getTitle(), ticket.getDescription());
            if (!injectionFlags.isEmpty()) {
                log.warn("[TicketAgentInvestigationService] SUSPICIOUS ticketId={} patterns={}",
                        ticketId, injectionFlags);
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

            TicketAgentSynthesisResult synthesis;
            try {
                synthesis = chatClient.prompt()
                        .system(systemPrompt)
                        .user(taskPrompt)
                        .call()
                        .entity(TicketAgentSynthesisResult.class);
                log.info("Agent synthesis parsed for ticketId={} -> {}", ticketId, synthesis);
            } catch (Exception e) {
                log.error("Agent GPT synthesis failed for ticketId={}", ticketId, e);
                return failRun(run, "AI provider failed or returned invalid output.", traceForResponse);
            }

            try {
                validator.validate(synthesis, evidence);
            } catch (AgentValidationException e) {
                log.warn("Agent output rejected for ticketId={} reason={}", ticketId, e.getMessage());
                return failRun(run, e.getMessage(), traceForResponse);
            }

            return succeedRun(run, synthesis, traceForResponse);

        } catch (AgentToolException e) {
            log.warn("Agent tool failure for ticketId={} reason={}", ticketId, e.getMessage());
            return failRun(run, e.getMessage(), traceForResponse);
        } catch (Exception e) {
            log.error("Unexpected agent failure for ticketId={}", ticketId, e);
            return failRun(run, "Agent investigation failed unexpectedly.", traceForResponse);
        }
    }

    /**
     * returns the last persisted agent run for this ticket
     * without re-running the investigation (no new tool calls, no new
     * LLM call). Used so the frontend can redisplay old results after
     * navigating away and back.
     */
    public Optional<TicketAgentInvestigationResponse> getLatestRun(Long ticketId) {
        return agentRunRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId)
                .map(this::toResponseFromStored);
    }

    private TicketAgentInvestigationResponse toResponseFromStored(AgentRun run) {
        List<AgentToolCallTrace> trace = agentToolCallRepository
                .findByAgentRunIdOrderByStartedAtAsc(run.getId())
                .stream()
                .map(tc -> new AgentToolCallTrace(tc.getToolName(), tc.getStatus().name(),
                        tc.getErrorMessage(), tc.getStartedAt(), tc.getCompletedAt()))
                .collect(Collectors.toList());

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setStatus(run.getStatus());
        response.setPromptVersion(run.getPromptVersion());
        response.setModelName(run.getModelName());
        response.setErrorMessage(run.getErrorMessage());
        response.setToolCalls(trace);
        response.setCreatedAt(run.getCreatedAt());

        if (run.getStatus() == AgentRunStatus.SUCCESS && run.getResultJson() != null) {
            try {
                TicketAgentSynthesisResult synthesis =
                        objectMapper.readValue(run.getResultJson(), TicketAgentSynthesisResult.class);
                response.setInvestigationSummary(synthesis.getInvestigationSummary());
                response.setEvidenceRefs(synthesis.getEvidenceRefs());
                response.setPreviousReviewSummary(synthesis.getPreviousReviewSummary());
                response.setRecommendedNextSteps(synthesis.getRecommendedNextSteps());
                response.setDraftTechnicianResponse(synthesis.getDraftTechnicianResponse());
                response.setConfidence(synthesis.getConfidence());
                response.setLimitations(synthesis.getLimitations());
                response.setNeedsHumanReview(synthesis.getNeedsHumanReview());
            } catch (Exception e) {
                log.warn("Failed to deserialize stored agent run resultJson for runId={}", run.getId(), e);
            }
        } else {
            response.setNeedsHumanReview(true);
        }
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
            log.warn("Failed to serialize tool call payload", e);
            return null;
        }
    }

    private TicketAgentInvestigationResponse failRun(AgentRun run, String errorMessage,
                                                     List<AgentToolCallTrace> traceForResponse) {
        run.setStatus(AgentRunStatus.FAILED);
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(LocalDateTime.now());
        agentRunRepository.save(run);

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setStatus(AgentRunStatus.FAILED);
        response.setPromptVersion(run.getPromptVersion());
        response.setModelName(run.getModelName());
        response.setErrorMessage(errorMessage);
        response.setToolCalls(traceForResponse);
        response.setCreatedAt(run.getCreatedAt());
        response.setNeedsHumanReview(true);
        return response;
    }

    @Transactional
    protected TicketAgentInvestigationResponse succeedRun(AgentRun run, TicketAgentSynthesisResult synthesis,
                                                          List<AgentToolCallTrace> traceForResponse) {
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setCompletedAt(LocalDateTime.now());
        run.setResultJson(safeWrite(synthesis));
        agentRunRepository.save(run);

        TicketAgentInvestigationResponse response = new TicketAgentInvestigationResponse();
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setStatus(AgentRunStatus.SUCCESS);
        response.setPromptVersion(run.getPromptVersion());
        response.setModelName(run.getModelName());
        response.setInvestigationSummary(synthesis.getInvestigationSummary());
        response.setEvidenceRefs(synthesis.getEvidenceRefs());
        response.setPreviousReviewSummary(synthesis.getPreviousReviewSummary());
        response.setRecommendedNextSteps(synthesis.getRecommendedNextSteps());
        response.setDraftTechnicianResponse(synthesis.getDraftTechnicianResponse());
        response.setConfidence(synthesis.getConfidence());
        response.setLimitations(synthesis.getLimitations());
        response.setNeedsHumanReview(synthesis.getNeedsHumanReview());
        response.setToolCalls(traceForResponse);
        response.setCreatedAt(run.getCreatedAt());
        return response;
    }

    @FunctionalInterface
    private interface ToolCall<T> {
        T execute();
    }
}