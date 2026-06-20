package com.genai.java.spring.aireview.dto;

import java.util.List;

public class TicketAiReviewResponse {

    private String summary;
    private List<String> possibleCauses;
    private List<String> recommendedChecks;
    private String draftResponse;
    private Confidence confidence;

    public String getSummary()                         { return summary; }
    public void setSummary(String summary)              { this.summary = summary; }

    public List<String> getPossibleCauses()             { return possibleCauses; }
    public void setPossibleCauses(List<String> v)       { this.possibleCauses = v; }

    public List<String> getRecommendedChecks()          { return recommendedChecks; }
    public void setRecommendedChecks(List<String> v)    { this.recommendedChecks = v; }

    public String getDraftResponse()                    { return draftResponse; }
    public void setDraftResponse(String v)              { this.draftResponse = v; }

    public Confidence getConfidence()        { return confidence; }
    public void setConfidence(Confidence v)  { this.confidence = v; }
}