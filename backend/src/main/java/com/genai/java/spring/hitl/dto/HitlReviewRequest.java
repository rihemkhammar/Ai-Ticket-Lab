package com.genai.java.spring.hitl.dto;

/**
 * Request body for POST /api/tickets/{ticketId}/agent/hitl-review .
 */
public class HitlReviewRequest {

    public static final String DEFAULT_USER_GOAL =
            "Investigate this ticket and draft a recommendation for human review.";
    public static final int DEFAULT_TOP_K = 3;
    public static final int MIN_TOP_K = 1;
    public static final int MAX_TOP_K = 10;
    public static final boolean DEFAULT_INCLUDE_PREVIOUS_REVIEWS = true;

    private String userGoal;
    private Integer topK;
    private Boolean includePreviousReviews;

    public String getUserGoal() {
        return (userGoal == null || userGoal.isBlank()) ? DEFAULT_USER_GOAL : userGoal;
    }
    public void setUserGoal(String v) { this.userGoal = v; }

    public int getTopK() {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.min(Math.max(topK, MIN_TOP_K), MAX_TOP_K);
    }
    public void setTopK(Integer v) { this.topK = v; }

    public boolean getIncludePreviousReviews() {
        return includePreviousReviews == null ? DEFAULT_INCLUDE_PREVIOUS_REVIEWS : includePreviousReviews;
    }
    public void setIncludePreviousReviews(Boolean v) { this.includePreviousReviews = v; }
}
