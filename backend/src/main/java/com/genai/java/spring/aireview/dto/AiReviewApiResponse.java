package com.genai.java.spring.aireview.dto;

import com.genai.java.spring.aireview.AiReviewStatus;
import java.time.LocalDateTime;

public class AiReviewApiResponse {

    private Long reviewId;
    private Long ticketId;
    private String promptVersion;
    private String modelName;
    private AiReviewStatus status;
    private TicketAiReviewResponse result;
    private String errorMessage;
    private LocalDateTime createdAt;

    public Long getReviewId()                          { return reviewId; }
    public void setReviewId(Long v)                    { this.reviewId = v; }

    public Long getTicketId()                          { return ticketId; }
    public void setTicketId(Long v)                    { this.ticketId = v; }

    public String getPromptVersion()                   { return promptVersion; }
    public void setPromptVersion(String v)             { this.promptVersion = v; }

    public String getModelName()                       { return modelName; }
    public void setModelName(String v)                 { this.modelName = v; }

    public AiReviewStatus getStatus()                   { return status; }
    public void setStatus(AiReviewStatus v)             { this.status = v; }

    public TicketAiReviewResponse getResult()           { return result; }
    public void setResult(TicketAiReviewResponse v)     { this.result = v; }

    public String getErrorMessage()                     { return errorMessage; }
    public void setErrorMessage(String v)               { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime v)            { this.createdAt = v; }
}