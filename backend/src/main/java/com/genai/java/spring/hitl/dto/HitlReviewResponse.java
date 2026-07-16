package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.agent.dto.AgentToolCallTrace;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.rag.review.dto.EvidenceRef;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned when a HITL run is created or reloaded while it is
 * still pending human review (WAITING_FOR_HUMAN) or being revised.
 * Also used to reload a FINALIZED or REJECTED run (finalReviewedResult /
 * humanDecision / humanComment populated in that case).
 */
public class HitlReviewResponse {

    private Long runId;
    private Long ticketId;
    private Long checkpointId;
    private Integer checkpointNumber;
    private AgentRunStatus status;
    private ReviewCheckpointStatus checkpointStatus;

    private String investigationSummary;
    private List<EvidenceRef> evidenceRefs;
    private String previousReviewSummary;
    private List<String> recommendedNextSteps;
    private String draftTechnicianResponse;
    private Confidence confidence;
    private List<String> limitations;
    private Boolean needsHumanReview;
    private List<AgentToolCallTrace> toolCalls;

    private HumanReviewDecision humanDecision;
    private String humanComment;
    private FinalReviewedResult finalReviewedResult;

    private String errorMessage;
    private LocalDateTime createdAt;

    public Long getRunId()                                   { return runId; }
    public void setRunId(Long v)                             { this.runId = v; }

    public Long getTicketId()                                { return ticketId; }
    public void setTicketId(Long v)                          { this.ticketId = v; }

    public Long getCheckpointId()                            { return checkpointId; }
    public void setCheckpointId(Long v)                      { this.checkpointId = v; }

    public Integer getCheckpointNumber()                     { return checkpointNumber; }
    public void setCheckpointNumber(Integer v)               { this.checkpointNumber = v; }

    public AgentRunStatus getStatus()                        { return status; }
    public void setStatus(AgentRunStatus v)                  { this.status = v; }

    public ReviewCheckpointStatus getCheckpointStatus()      { return checkpointStatus; }
    public void setCheckpointStatus(ReviewCheckpointStatus v){ this.checkpointStatus = v; }

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

    public Confidence getConfidence()                         { return confidence; }
    public void setConfidence(Confidence v)                   { this.confidence = v; }

    public List<String> getLimitations()                      { return limitations; }
    public void setLimitations(List<String> v)                { this.limitations = v; }

    public Boolean getNeedsHumanReview()                       { return needsHumanReview; }
    public void setNeedsHumanReview(Boolean v)                 { this.needsHumanReview = v; }

    public List<AgentToolCallTrace> getToolCalls()             { return toolCalls; }
    public void setToolCalls(List<AgentToolCallTrace> v)       { this.toolCalls = v; }

    public HumanReviewDecision getHumanDecision()               { return humanDecision; }
    public void setHumanDecision(HumanReviewDecision v)         { this.humanDecision = v; }

    public String getHumanComment()                             { return humanComment; }
    public void setHumanComment(String v)                       { this.humanComment = v; }

    public FinalReviewedResult getFinalReviewedResult()         { return finalReviewedResult; }
    public void setFinalReviewedResult(FinalReviewedResult v)   { this.finalReviewedResult = v; }

    public String getErrorMessage()                           { return errorMessage; }
    public void setErrorMessage(String v)                     { this.errorMessage = v; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)                  { this.createdAt = v; }
}
