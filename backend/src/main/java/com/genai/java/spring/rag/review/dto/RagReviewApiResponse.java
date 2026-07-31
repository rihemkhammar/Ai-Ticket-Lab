package com.genai.java.spring.rag.review.dto;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;

import java.time.LocalDateTime;
import java.util.List;

public class RagReviewApiResponse implements java.io.Serializable {

    private Long reviewId;
    private Long ticketId;
    private String promptVersion;
    private String modelName;
    private AiReviewStatus status;
    private TicketRagReviewResponse result;
    private String errorMessage;
    private LocalDateTime createdAt;

    /** Full retrieved chunks (text included), shown by the frontend as an optional chunk preview. */
    private List<EvidenceChunkResponse> retrievedEvidence;

    public Long getReviewId()                                  { return reviewId; }
    public void setReviewId(Long v)                            { this.reviewId = v; }

    public Long getTicketId()                                  { return ticketId; }
    public void setTicketId(Long v)                            { this.ticketId = v; }

    public String getPromptVersion()                           { return promptVersion; }
    public void setPromptVersion(String v)                     { this.promptVersion = v; }

    public String getModelName()                                { return modelName; }
    public void setModelName(String v)                          { this.modelName = v; }

    public AiReviewStatus getStatus()                            { return status; }
    public void setStatus(AiReviewStatus v)                      { this.status = v; }

    public TicketRagReviewResponse getResult()                   { return result; }
    public void setResult(TicketRagReviewResponse v)             { this.result = v; }

    public String getErrorMessage()                              { return errorMessage; }
    public void setErrorMessage(String v)                        { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void setCreatedAt(LocalDateTime v)                    { this.createdAt = v; }

    public List<EvidenceChunkResponse> getRetrievedEvidence()    { return retrievedEvidence; }
    public void setRetrievedEvidence(List<EvidenceChunkResponse> v) { this.retrievedEvidence = v; }
}