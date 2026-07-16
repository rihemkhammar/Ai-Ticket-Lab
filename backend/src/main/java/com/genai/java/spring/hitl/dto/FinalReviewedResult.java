package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.rag.review.dto.EvidenceRef;

import java.util.List;

/**
 * The finalized, human-reviewed AI recommendation .
 * Persisted as agent_review_checkpoint.final_reviewed_result_json and
 * returned to the frontend after an APPROVE decision.
 *
 * officialActionExecuted and ticketStatusChanged are ALWAYS false in this
 * HITL approval never mutates the ticket.
 */
public class FinalReviewedResult {

    private String investigationSummary;
    private List<EvidenceRef> evidenceRefs;
    private List<String> recommendedNextSteps;
    private String draftTechnicianResponse;
    private Confidence confidence;
    private List<String> limitations;

    private boolean humanReviewed;
    private HumanReviewDecision humanDecision;
    private boolean officialActionExecuted;
    private boolean ticketStatusChanged;

    public String getInvestigationSummary()                 { return investigationSummary; }
    public void setInvestigationSummary(String v)           { this.investigationSummary = v; }

    public List<EvidenceRef> getEvidenceRefs()               { return evidenceRefs; }
    public void setEvidenceRefs(List<EvidenceRef> v)         { this.evidenceRefs = v; }

    public List<String> getRecommendedNextSteps()            { return recommendedNextSteps; }
    public void setRecommendedNextSteps(List<String> v)      { this.recommendedNextSteps = v; }

    public String getDraftTechnicianResponse()               { return draftTechnicianResponse; }
    public void setDraftTechnicianResponse(String v)         { this.draftTechnicianResponse = v; }

    public Confidence getConfidence()                        { return confidence; }
    public void setConfidence(Confidence v)                  { this.confidence = v; }

    public List<String> getLimitations()                     { return limitations; }
    public void setLimitations(List<String> v)               { this.limitations = v; }

    public boolean isHumanReviewed()                         { return humanReviewed; }
    public void setHumanReviewed(boolean v)                  { this.humanReviewed = v; }

    public HumanReviewDecision getHumanDecision()             { return humanDecision; }
    public void setHumanDecision(HumanReviewDecision v)       { this.humanDecision = v; }

    public boolean isOfficialActionExecuted()                 { return officialActionExecuted; }
    public void setOfficialActionExecuted(boolean v)          { this.officialActionExecuted = v; }

    public boolean isTicketStatusChanged()                    { return ticketStatusChanged; }
    public void setTicketStatusChanged(boolean v)              { this.ticketStatusChanged = v; }
}
