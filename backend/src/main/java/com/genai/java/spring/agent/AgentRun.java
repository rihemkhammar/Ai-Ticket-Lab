package com.genai.java.spring.agent;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_run")
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRunStatus status;

    @Column(name = "result_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // observability / trace fields ──────────────────────────────
    @Column(name = "trace_id")
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type")
    private AgentRunType runType;

    @Column(name = "duration_ms")
    private Long durationMs;

    public AgentRun() {}

    /** Copies all fields, including the generated id, so a status transition can be
     * persisted as a distinct object rather than mutating an already-saved instance
     * in place (keeps the RUNNING snapshot that was saved earlier genuinely intact). */
    public AgentRun(AgentRun other) {
        this.id = other.id;
        this.ticketId = other.ticketId;
        this.promptVersion = other.promptVersion;
        this.modelName = other.modelName;
        this.status = other.status;
        this.resultJson = other.resultJson;
        this.errorMessage = other.errorMessage;
        this.createdAt = other.createdAt;
        this.completedAt = other.completedAt;
        this.traceId = other.traceId;
        this.runType = other.runType;
        this.durationMs = other.durationMs;
    }

    public Long getId()                          { return id; }

    public Long getTicketId()                    { return ticketId; }
    public void setTicketId(Long v)              { this.ticketId = v; }

    public String getPromptVersion()             { return promptVersion; }
    public void setPromptVersion(String v)       { this.promptVersion = v; }

    public String getModelName()                 { return modelName; }
    public void setModelName(String v)           { this.modelName = v; }

    public AgentRunStatus getStatus()            { return status; }
    public void setStatus(AgentRunStatus v)      { this.status = v; }

    public String getResultJson()                { return resultJson; }
    public void setResultJson(String v)          { this.resultJson = v; }

    public String getErrorMessage()              { return errorMessage; }
    public void setErrorMessage(String v)        { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }

    public LocalDateTime getCompletedAt()        { return completedAt; }
    public void setCompletedAt(LocalDateTime v)  { this.completedAt = v; }

    public String getTraceId()                   { return traceId; }
    public void setTraceId(String v)             { this.traceId = v; }

    public AgentRunType getRunType()              { return runType; }
    public void setRunType(AgentRunType v)        { this.runType = v; }

    public Long getDurationMs()                   { return durationMs; }
    public void setDurationMs(Long v)             { this.durationMs = v; }
}