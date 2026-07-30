package com.genai.java.spring.triage;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "triage_run")
public class TriageRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriageRunStatus status;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    // JSON array of ticket ids still waiting to be dispatched,
    // ordered by criticality once OrderQueueNode has run.
    @Column(name = "ticket_queue", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String ticketQueue;

    // JSON map/array of per-ticket classification results.
    @Column(name = "classifications_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String classificationsJson;

    // JSON array of TriageTreatedItem, appended one by one as tickets
    // move through the pipeline.
    @Column(name = "treated_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String treatedJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public TriageRun() {}

    public Long getId()                              { return id; }

    public TriageRunStatus getStatus()               { return status; }
    public void setStatus(TriageRunStatus v)         { this.status = v; }

    public String getPromptVersion()                 { return promptVersion; }
    public void setPromptVersion(String v)           { this.promptVersion = v; }

    public String getModelName()                     { return modelName; }
    public void setModelName(String v)               { this.modelName = v; }

    public String getTicketQueue()                   { return ticketQueue; }
    public void setTicketQueue(String v)             { this.ticketQueue = v; }

    public String getClassificationsJson()           { return classificationsJson; }
    public void setClassificationsJson(String v)     { this.classificationsJson = v; }

    public String getTreatedJson()                   { return treatedJson; }
    public void setTreatedJson(String v)             { this.treatedJson = v; }

    public String getErrorMessage()                  { return errorMessage; }
    public void setErrorMessage(String v)            { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)        { this.updatedAt = v; }

    public LocalDateTime getCompletedAt()            { return completedAt; }
    public void setCompletedAt(LocalDateTime v)      { this.completedAt = v; }
}