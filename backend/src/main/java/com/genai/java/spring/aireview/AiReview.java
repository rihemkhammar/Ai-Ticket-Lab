package com.genai.java.spring.aireview;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "ai_reviews")
public class AiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "triggered_by", nullable = false)
    private UUID triggeredBy;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiReviewStatus status;

    @Column(name = "result", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId()                              { return id; }

    public Long getTicketId()                        { return ticketId; }
    public void setTicketId(Long v)                  { this.ticketId = v; }

    public UUID getTriggeredBy()                     { return triggeredBy; }
    public void setTriggeredBy(UUID v)               { this.triggeredBy = v; }

    public String getPromptVersion()                 { return promptVersion; }
    public void setPromptVersion(String v)           { this.promptVersion = v; }

    public String getModelName()                     { return modelName; }
    public void setModelName(String v)               { this.modelName = v; }

    public AiReviewStatus getStatus()                { return status; }
    public void setStatus(AiReviewStatus v)          { this.status = v; }

    public String getResultJson()                    { return resultJson; }
    public void setResultJson(String v)              { this.resultJson = v; }

    public String getErrorMessage()                  { return errorMessage; }
    public void setErrorMessage(String v)            { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }
}