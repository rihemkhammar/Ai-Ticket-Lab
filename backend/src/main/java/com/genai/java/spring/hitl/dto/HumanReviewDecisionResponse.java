package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.HumanReviewDecision;

import java.time.LocalDateTime;

/**
 * Response of POST /api/agent-runs/{runId}/human-review/decision.
 *
 * - APPROVE           -> finalReviewedResult is populated, finalStatus=FINALIZED
 * - REJECT             -> finalReviewedResult is null, finalStatus=REJECTED
 * - REQUEST_REVISION   -> revisedReview is populated (new pending checkpoint), finalStatus=WAITING_FOR_HUMAN
 */
public class HumanReviewDecisionResponse {

    private Long runId;
    private Long ticketId;
    private AgentRunStatus finalStatus;
    private Long checkpointId;

    private HumanReviewDecision humanDecision;
    private String humanComment;

    private FinalReviewedResult finalReviewedResult;
    private HitlReviewResponse revisedReview;

    private boolean humanReviewed;
    private boolean officialActionExecuted;
    private boolean ticketStatusChanged;

    private LocalDateTime finalizedAt;

    public Long getRunId()                                    { return runId; }
    public void setRunId(Long v)                              { this.runId = v; }

    public Long getTicketId()                                 { return ticketId; }
    public void setTicketId(Long v)                           { this.ticketId = v; }

    public AgentRunStatus getFinalStatus()                    { return finalStatus; }
    public void setFinalStatus(AgentRunStatus v)              { this.finalStatus = v; }

    public Long getCheckpointId()                             { return checkpointId; }
    public void setCheckpointId(Long v)                       { this.checkpointId = v; }

    public HumanReviewDecision getHumanDecision()              { return humanDecision; }
    public void setHumanDecision(HumanReviewDecision v)        { this.humanDecision = v; }

    public String getHumanComment()                            { return humanComment; }
    public void setHumanComment(String v)                      { this.humanComment = v; }

    public FinalReviewedResult getFinalReviewedResult()        { return finalReviewedResult; }
    public void setFinalReviewedResult(FinalReviewedResult v)  { this.finalReviewedResult = v; }

    public HitlReviewResponse getRevisedReview()                { return revisedReview; }
    public void setRevisedReview(HitlReviewResponse v)          { this.revisedReview = v; }

    public boolean isHumanReviewed()                            { return humanReviewed; }
    public void setHumanReviewed(boolean v)                     { this.humanReviewed = v; }

    public boolean isOfficialActionExecuted()                   { return officialActionExecuted; }
    public void setOfficialActionExecuted(boolean v)            { this.officialActionExecuted = v; }

    public boolean isTicketStatusChanged()                      { return ticketStatusChanged; }
    public void setTicketStatusChanged(boolean v)               { this.ticketStatusChanged = v; }

    public LocalDateTime getFinalizedAt()                       { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime v)                 { this.finalizedAt = v; }
}
