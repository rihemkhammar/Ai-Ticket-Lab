package com.genai.java.spring.agent.tool.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PreviousAiReviewResult {

    private Long ticketId;
    private List<ReviewSummary> reviews;

    public static PreviousAiReviewResult of(Long ticketId, List<ReviewSummary> reviews) {
        PreviousAiReviewResult r = new PreviousAiReviewResult();
        r.ticketId = ticketId;
        r.reviews = reviews;
        return r;
    }

    public Long getTicketId()                    { return ticketId; }
    public void setTicketId(Long v)              { this.ticketId = v; }

    public List<ReviewSummary> getReviews()       { return reviews; }
    public void setReviews(List<ReviewSummary> v) { this.reviews = v; }

    public static class ReviewSummary {
        private Long reviewId;
        private String promptVersion;
        private String status;
        private LocalDateTime createdAt;
        private String summary;

        public static ReviewSummary of(Long reviewId, String promptVersion, String status,
                                        LocalDateTime createdAt, String summary) {
            ReviewSummary s = new ReviewSummary();
            s.reviewId = reviewId;
            s.promptVersion = promptVersion;
            s.status = status;
            s.createdAt = createdAt;
            s.summary = summary;
            return s;
        }

        public Long getReviewId()                    { return reviewId; }
        public void setReviewId(Long v)              { this.reviewId = v; }

        public String getPromptVersion()             { return promptVersion; }
        public void setPromptVersion(String v)       { this.promptVersion = v; }

        public String getStatus()                    { return status; }
        public void setStatus(String v)              { this.status = v; }

        public LocalDateTime getCreatedAt()          { return createdAt; }
        public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }

        public String getSummary()                    { return summary; }
        public void setSummary(String v)              { this.summary = v; }
    }
}
