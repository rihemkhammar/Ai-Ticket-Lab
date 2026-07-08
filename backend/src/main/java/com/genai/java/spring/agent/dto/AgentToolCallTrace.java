package com.genai.java.spring.agent.dto;

import java.time.LocalDateTime;

/**
 * Operational tool-call trace shown to the frontend (S4-G07).
 *
 * Deliberately shallow: tool name, status, error, and started/completed
 * timestamps only. This is NOT a chain-of-thought transcript — the raw
 * input/output JSON of each tool call is persisted separately in
 * AgentToolCall and is intentionally NOT surfaced here, to avoid exposing
 * internal reasoning payloads to the UI.
 */
public class AgentToolCallTrace {

    private String toolName;
    private String status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public AgentToolCallTrace() {}

    /** @deprecated kept for backward compatibility; prefer the timestamped constructor. */
    @Deprecated
    public AgentToolCallTrace(String toolName, String status, String errorMessage) {
        this(toolName, status, errorMessage, null, null);
    }

    public AgentToolCallTrace(String toolName, String status, String errorMessage,
                              LocalDateTime startedAt, LocalDateTime completedAt) {
        this.toolName = toolName;
        this.status = status;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getToolName()             { return toolName; }
    public void setToolName(String v)       { this.toolName = v; }

    public String getStatus()                { return status; }
    public void setStatus(String v)          { this.status = v; }

    public String getErrorMessage()          { return errorMessage; }
    public void setErrorMessage(String v)    { this.errorMessage = v; }

    public LocalDateTime getStartedAt()      { return startedAt; }
    public void setStartedAt(LocalDateTime v){ this.startedAt = v; }

    public LocalDateTime getCompletedAt()       { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
}
