package com.genai.java.spring.observability.dto;

import java.time.LocalDateTime;

/**
 * Tool-call entry within an AgentRunTraceResponse .
 * Deliberately shallow — no input/output payloads, no chain-of-thought.
 */
public class AgentToolCallTraceResponse {

    private String toolName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String errorMessage;

    public String getToolName()               { return toolName; }
    public void setToolName(String v)         { this.toolName = v; }

    public String getStatus()                  { return status; }
    public void setStatus(String v)            { this.status = v; }

    public LocalDateTime getStartedAt()        { return startedAt; }
    public void setStartedAt(LocalDateTime v)  { this.startedAt = v; }

    public LocalDateTime getCompletedAt()      { return completedAt; }
    public void setCompletedAt(LocalDateTime v){ this.completedAt = v; }

    public Long getDurationMs()                 { return durationMs; }
    public void setDurationMs(Long v)           { this.durationMs = v; }

    public String getErrorMessage()            { return errorMessage; }
    public void setErrorMessage(String v)      { this.errorMessage = v; }
}