package com.genai.java.spring.agent.dto;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.review.dto.EvidenceRef;

import java.time.LocalDateTime;
import java.util.List;

public class TicketAgentInvestigationResponse implements java.io.Serializable {

    private Long runId;
    private Long ticketId;
    private AgentRunStatus status;
    private String promptVersion;
    private String modelName;

    private String investigationSummary;
    private List<EvidenceRef> evidenceRefs;
    private String previousReviewSummary;
    private List<String> recommendedNextSteps;
    private String draftTechnicianResponse;
    private List<AgentToolCallTrace> toolCalls;
    private Confidence confidence;
    private List<String> limitations;
    private Boolean needsHumanReview;

    private String errorMessage;
    private LocalDateTime createdAt;

    public Long getRunId()                                   { return runId; }
    public void setRunId(Long v)                             { this.runId = v; }

    public Long getTicketId()                                { return ticketId; }
    public void setTicketId(Long v)                          { this.ticketId = v; }

    public AgentRunStatus getStatus()                        { return status; }
    public void setStatus(AgentRunStatus v)                  { this.status = v; }

    public String getPromptVersion()                         { return promptVersion; }
    public void setPromptVersion(String v)                   { this.promptVersion = v; }

    public String getModelName()                             { return modelName; }
    public void setModelName(String v)                       { this.modelName = v; }

    public String getInvestigationSummary()                  { return investigationSummary; }
    public void setInvestigationSummary(String v)            { this.investigationSummary = v; }

    public List<EvidenceRef> getEvidenceRefs()                { return evidenceRefs; }
    public void setEvidenceRefs(List<EvidenceRef> v)          { this.evidenceRefs = v; }

    public String getPreviousReviewSummary()                  { return previousReviewSummary; }
    public void setPreviousReviewSummary(String v)            { this.previousReviewSummary = v; }

    public List<String> getRecommendedNextSteps()             { return recommendedNextSteps; }
    public void setRecommendedNextSteps(List<String> v)       { this.recommendedNextSteps = v; }

    public String getDraftTechnicianResponse()                { return draftTechnicianResponse; }
    public void setDraftTechnicianResponse(String v)          { this.draftTechnicianResponse = v; }

    public List<AgentToolCallTrace> getToolCalls()            { return toolCalls; }
    public void setToolCalls(List<AgentToolCallTrace> v)      { this.toolCalls = v; }

    public Confidence getConfidence()                         { return confidence; }
    public void setConfidence(Confidence v)                   { this.confidence = v; }

    public List<String> getLimitations()                      { return limitations; }
    public void setLimitations(List<String> v)                { this.limitations = v; }

    public Boolean getNeedsHumanReview()                       { return needsHumanReview; }
    public void setNeedsHumanReview(Boolean v)                 { this.needsHumanReview = v; }

    public String getErrorMessage()                           { return errorMessage; }
    public void setErrorMessage(String v)                     { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)                  { this.createdAt = v; }
}
