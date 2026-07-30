package com.genai.java.spring.observability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response of GET /api/agent-runs/{runId}/trace (and /api/ai-traces/{traceId}).
 *
 * Exposes only safe, non-hidden metadata : trace/run identifiers,
 * model/prompt metadata, timestamps, tool-call trace, checkpoint/human
 * decision trace, and the M5 safety flags. Never exposes hidden
 * chain-of-thought or raw model reasoning.
 */
public class AgentRunTraceResponse {

    private String traceId;
    private Long runId;
    private Long ticketId;
    private String runType;
    private String status;
    private String promptVersion;
    private String modelName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;

    // Mandatory safety invariants preserved from M5 — always false.
    private boolean officialActionExecuted;
    private boolean ticketStatusChanged;

    private List<AgentToolCallTraceResponse> toolCalls;
    private List<AgentCheckpointTraceResponse> checkpoints;
    private List<AgentCheckpointTraceResponse> humanDecisions;

    private String errorMessage;

    public String getTraceId()                    { return traceId; }
    public void setTraceId(String v)              { this.traceId = v; }

    public Long getRunId()                         { return runId; }
    public void setRunId(Long v)                   { this.runId = v; }

    public Long getTicketId()                      { return ticketId; }
    public void setTicketId(Long v)                { this.ticketId = v; }

    public String getRunType()                     { return runType; }
    public void setRunType(String v)               { this.runType = v; }

    public String getStatus()                      { return status; }
    public void setStatus(String v)                { this.status = v; }

    public String getPromptVersion()               { return promptVersion; }
    public void setPromptVersion(String v)         { this.promptVersion = v; }

    public String getModelName()                   { return modelName; }
    public void setModelName(String v)             { this.modelName = v; }

    public LocalDateTime getStartedAt()            { return startedAt; }
    public void setStartedAt(LocalDateTime v)      { this.startedAt = v; }

    public LocalDateTime getCompletedAt()          { return completedAt; }
    public void setCompletedAt(LocalDateTime v)    { this.completedAt = v; }

    public Long getDurationMs()                     { return durationMs; }
    public void setDurationMs(Long v)               { this.durationMs = v; }

    public boolean isOfficialActionExecuted()       { return officialActionExecuted; }
    public void setOfficialActionExecuted(boolean v){ this.officialActionExecuted = v; }

    public boolean isTicketStatusChanged()          { return ticketStatusChanged; }
    public void setTicketStatusChanged(boolean v)   { this.ticketStatusChanged = v; }

    public List<AgentToolCallTraceResponse> getToolCalls()               { return toolCalls; }
    public void setToolCalls(List<AgentToolCallTraceResponse> v)         { this.toolCalls = v; }

    public List<AgentCheckpointTraceResponse> getCheckpoints()           { return checkpoints; }
    public void setCheckpoints(List<AgentCheckpointTraceResponse> v)     { this.checkpoints = v; }

    public List<AgentCheckpointTraceResponse> getHumanDecisions()        { return humanDecisions; }
    public void setHumanDecisions(List<AgentCheckpointTraceResponse> v)  { this.humanDecisions = v; }

    public String getErrorMessage()                { return errorMessage; }
    public void setErrorMessage(String v)          { this.errorMessage = v; }
}