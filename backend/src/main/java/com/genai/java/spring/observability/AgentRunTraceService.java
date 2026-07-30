package com.genai.java.spring.observability;

import com.genai.java.spring.agent.AgentRun;
import com.genai.java.spring.agent.AgentRunRepository;
import com.genai.java.spring.agent.AgentToolCall;
import com.genai.java.spring.agent.AgentToolCallRepository;
import com.genai.java.spring.hitl.AgentReviewCheckpoint;
import com.genai.java.spring.hitl.AgentReviewCheckpointRepository;
import com.genai.java.spring.observability.dto.AgentCheckpointTraceResponse;
import com.genai.java.spring.observability.dto.AgentRunTraceResponse;
import com.genai.java.spring.observability.dto.AgentToolCallTraceResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Assembles the full trace of an agent/HITL run: run metadata, tool-call
 * trace, and checkpoint/human-decision trace .
 *
 * Read-only — never mutates agent_run, agent_tool_call, or
 * agent_review_checkpoint. Failed runs keep whatever trace data was
 * persisted before the failure .
 */
@Service
public class AgentRunTraceService {

    private final AgentRunRepository agentRunRepository;
    private final AgentToolCallRepository agentToolCallRepository;
    private final AgentReviewCheckpointRepository checkpointRepository;

    public AgentRunTraceService(AgentRunRepository agentRunRepository,
                                AgentToolCallRepository agentToolCallRepository,
                                AgentReviewCheckpointRepository checkpointRepository) {
        this.agentRunRepository = agentRunRepository;
        this.agentToolCallRepository = agentToolCallRepository;
        this.checkpointRepository = checkpointRepository;
    }

    public Optional<AgentRunTraceResponse> getTraceByRunId(Long runId) {
        return agentRunRepository.findById(runId).map(this::buildTrace);
    }

    public Optional<AgentRunTraceResponse> getTraceByTraceId(String traceId) {
        return agentRunRepository.findFirstByTraceId(traceId).map(this::buildTrace);
    }

    private AgentRunTraceResponse buildTrace(AgentRun run) {
        List<AgentToolCallTraceResponse> toolCalls = agentToolCallRepository
                .findByAgentRunIdOrderByStartedAtAsc(run.getId())
                .stream()
                .map(this::toToolCallTrace)
                .collect(Collectors.toList());

        List<AgentReviewCheckpoint> checkpointEntities =
                checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(run.getId());

        List<AgentCheckpointTraceResponse> checkpoints = checkpointEntities.stream()
                .map(this::toCheckpointTrace)
                .collect(Collectors.toList());

        List<AgentCheckpointTraceResponse> humanDecisions = checkpointEntities.stream()
                .filter(cp -> cp.getHumanDecision() != null)
                .map(this::toCheckpointTrace)
                .collect(Collectors.toList());

        AgentRunTraceResponse response = new AgentRunTraceResponse();
        response.setTraceId(run.getTraceId());
        response.setRunId(run.getId());
        response.setTicketId(run.getTicketId());
        response.setRunType(run.getRunType() != null ? run.getRunType().name() : null);
        response.setStatus(run.getStatus() != null ? run.getStatus().name() : null);
        response.setPromptVersion(run.getPromptVersion());
        response.setModelName(run.getModelName());
        response.setStartedAt(run.getCreatedAt());
        response.setCompletedAt(run.getCompletedAt());
        response.setDurationMs(run.getDurationMs());
        // Mandatory safety invariants (S5 §2.5 / S6 §2.9) — never anything else.
        response.setOfficialActionExecuted(false);
        response.setTicketStatusChanged(false);
        response.setToolCalls(toolCalls);
        response.setCheckpoints(checkpoints);
        response.setHumanDecisions(humanDecisions);
        response.setErrorMessage(run.getErrorMessage());
        return response;
    }

    private AgentToolCallTraceResponse toToolCallTrace(AgentToolCall call) {
        AgentToolCallTraceResponse r = new AgentToolCallTraceResponse();
        r.setToolName(call.getToolName());
        r.setStatus(call.getStatus() != null ? call.getStatus().name() : null);
        r.setStartedAt(call.getStartedAt());
        r.setCompletedAt(call.getCompletedAt());
        r.setDurationMs(call.getDurationMs() != null
                ? call.getDurationMs()
                : computeDurationMs(call.getStartedAt(), call.getCompletedAt()));
        r.setErrorMessage(call.getErrorMessage());
        return r;
    }

    private AgentCheckpointTraceResponse toCheckpointTrace(AgentReviewCheckpoint cp) {
        AgentCheckpointTraceResponse r = new AgentCheckpointTraceResponse();
        r.setCheckpointId(cp.getId());
        r.setCheckpointNumber(cp.getCheckpointNumber());
        r.setStatus(cp.getStatus() != null ? cp.getStatus().name() : null);
        r.setHumanDecision(cp.getHumanDecision() != null ? cp.getHumanDecision().name() : null);
        r.setHumanComment(cp.getHumanComment());
        r.setCreatedAt(cp.getCreatedAt());
        r.setCompletedAt(cp.getCompletedAt());
        return r;
    }

    private Long computeDurationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }
}