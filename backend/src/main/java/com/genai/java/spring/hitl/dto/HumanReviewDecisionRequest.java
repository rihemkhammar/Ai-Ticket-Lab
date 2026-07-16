package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.hitl.HumanReviewDecision;

/**
 * Request body for POST /api/agent-runs/{runId}/human-review/decision.
 */
public class HumanReviewDecisionRequest {

    private HumanReviewDecision decision;
    private String comment;

    public HumanReviewDecision getDecision()   { return decision; }
    public void setDecision(HumanReviewDecision v) { this.decision = v; }

    public String getComment()                 { return comment; }
    public void setComment(String v)           { this.comment = v; }
}
