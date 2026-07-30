package com.genai.java.spring.triage.dto;

import com.genai.java.spring.triage.TriageRunStatus;
import com.genai.java.spring.triage.graph.TriageTreatedItem;

import java.time.LocalDateTime;
import java.util.List;

public class TriageRunResponse {

    private Long runId;
    private TriageRunStatus status;
    private String promptVersion;
    private List<Long> ticketQueue;
    private List<TriageTreatedItem> treated;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public TriageRunResponse() {}

    public Long getRunId()                               { return runId; }
    public void setRunId(Long v)                         { this.runId = v; }

    public TriageRunStatus getStatus()                   { return status; }
    public void setStatus(TriageRunStatus v)             { this.status = v; }

    public String getPromptVersion()                     { return promptVersion; }
    public void setPromptVersion(String v)               { this.promptVersion = v; }

    public List<Long> getTicketQueue()                   { return ticketQueue; }
    public void setTicketQueue(List<Long> v)             { this.ticketQueue = v; }

    public List<TriageTreatedItem> getTreated()          { return treated; }
    public void setTreated(List<TriageTreatedItem> v)    { this.treated = v; }

    public String getErrorMessage()                      { return errorMessage; }
    public void setErrorMessage(String v)                { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime v)            { this.createdAt = v; }

    public LocalDateTime getCompletedAt()                { return completedAt; }
    public void setCompletedAt(LocalDateTime v)          { this.completedAt = v; }
}