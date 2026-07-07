package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult;
import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult.ReviewSummary;
import com.genai.java.spring.aireview.AiReview;
import com.genai.java.spring.aireview.AiReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool 3 — PreviousAiReviewTool .
 * Read-only. Returns only summarized review content, never raw provider
 * errors, and never modifies reviews.
 */
@Slf4j
@Component
public class PreviousAiReviewTool {

    public static final String NAME = "PreviousAiReviewTool";

    private final AiReviewRepository aiReviewRepository;

    public PreviousAiReviewTool(AiReviewRepository aiReviewRepository) {
        this.aiReviewRepository = aiReviewRepository;
    }

    public PreviousAiReviewResult loadRecent(Long ticketId, int limit) {
        try {
            List<AiReview> reviews = aiReviewRepository
                    .findByTicketIdOrderByCreatedAtDesc(ticketId, PageRequest.of(0, Math.max(1, limit)));

            List<ReviewSummary> summaries = reviews.stream()
                    .map(r -> ReviewSummary.of(
                            r.getId(),
                            r.getPromptVersion(),
                            r.getStatus() != null ? r.getStatus().name() : null,
                            r.getCreatedAt(),
                            summarize(r)))
                    .toList();

            return PreviousAiReviewResult.of(ticketId, summaries);
        } catch (Exception e) {
            log.error("PreviousAiReviewTool failed for ticketId={}", ticketId, e);
            throw new AgentToolException("Previous AI reviews unavailable.");
        }
    }

    /** Never exposes the raw result_json or raw provider errors, only a short summary. */
    private String summarize(AiReview review) {
        if (review.getStatus() != null && review.getStatus().name().equals("FAILED")) {
            return "Previous review failed validation or provider call.";
        }
        String json = review.getResultJson();
        if (json == null || json.isBlank()) {
            return "Previous review completed with no stored summary.";
        }
        // Keep it short and safe: truncate rather than dump raw JSON to the model/UI.
        String trimmed = json.replaceAll("\\s+", " ").trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}
