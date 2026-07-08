package com.genai.java.spring.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /** Generic, safe fallback shown when resultJson is missing or cannot be parsed. */
    private static final String FALLBACK_SUMMARY = "Previous review completed with no readable summary.";

    private static final int MAX_SUMMARY_LENGTH = 300;

    private final AiReviewRepository aiReviewRepository;
    private final ObjectMapper objectMapper;

    public PreviousAiReviewTool(AiReviewRepository aiReviewRepository, ObjectMapper objectMapper) {
        this.aiReviewRepository = aiReviewRepository;
        this.objectMapper = objectMapper;
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

    /**
     * Never exposes the raw result_json or raw provider errors — only a
     * clean, human-readable summary (S4-G05). Parses the stored JSON and
     * extracts the "summary" field (shared shape between the basic and RAG
     * AI review responses); falls back to a safe generic message if the
     * field is missing or the JSON cannot be parsed, rather than dumping a
     * truncated raw JSON blob.
     */
    private String summarize(AiReview review) {
        if (review.getStatus() != null && review.getStatus().name().equals("FAILED")) {
            return "Previous review failed validation or provider call.";
        }
        String json = review.getResultJson();
        if (json == null || json.isBlank()) {
            return FALLBACK_SUMMARY;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode summaryNode = node.get("summary");
            if (summaryNode == null || summaryNode.isNull() || summaryNode.asText().isBlank()) {
                return FALLBACK_SUMMARY;
            }
            return truncate(summaryNode.asText().trim());
        } catch (Exception e) {
            log.warn("Failed to parse stored resultJson for AiReview id={}, using fallback summary",
                    review.getId(), e);
            return FALLBACK_SUMMARY;
        }
    }

    private String truncate(String text) {
        return text.length() > MAX_SUMMARY_LENGTH
                ? text.substring(0, MAX_SUMMARY_LENGTH) + "..."
                : text;
    }
}
