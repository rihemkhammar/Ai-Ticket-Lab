package com.genai.java.spring.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult;
import com.genai.java.spring.aireview.AiReview;
import com.genai.java.spring.aireview.AiReviewRepository;
import com.genai.java.spring.aireview.AiReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviousAiReviewToolTest {

    @Mock private AiReviewRepository aiReviewRepository;

    private PreviousAiReviewTool tool;

    private static final Long TICKET_ID = 1L;

    @BeforeEach
    void setUp() {
        tool = new PreviousAiReviewTool(aiReviewRepository, new ObjectMapper());
    }

    private AiReview review(Long id, AiReviewStatus status, String resultJson) {
        AiReview r = mock(AiReview.class);
        when(r.getId()).thenReturn(id);
        when(r.getPromptVersion()).thenReturn("ticket-rag-review-v1");
        when(r.getStatus()).thenReturn(status);
        when(r.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 8, 10, 0));
        // lenient: summarize() short-circuits on FAILED status before reading
        // getResultJson(), so this stub is unused (and flagged as unnecessary
        // by strict stubbing) in that case.
        lenient().when(r.getResultJson()).thenReturn(resultJson);
        return r;
    }

    @Test
    @DisplayName("returns recent reviews summarized, most recent first")
    void loadRecent_returnsSummarizedReviews() {
        AiReview success = review(12L, AiReviewStatus.SUCCESS, "{\"summary\":\"Motor may be overheating.\"}");
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenReturn(List.of(success));

        PreviousAiReviewResult result = tool.loadRecent(TICKET_ID, 3);

        assertThat(result.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(result.getReviews()).hasSize(1);
        assertThat(result.getReviews().get(0).getReviewId()).isEqualTo(12L);
        assertThat(result.getReviews().get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getReviews().get(0).getSummary()).contains("Motor may be overheating");
    }

    @Test
    @DisplayName("failed previous review is summarized without exposing raw provider errors")
    void loadRecent_failedReview_doesNotLeakRawError() {
        AiReview failed = review(13L, AiReviewStatus.FAILED, null);
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenReturn(List.of(failed));

        PreviousAiReviewResult result = tool.loadRecent(TICKET_ID, 3);

        assertThat(result.getReviews().get(0).getSummary())
                .doesNotContain("Exception")
                .contains("failed validation or provider call");
    }

    @Test
    @DisplayName("malformed resultJson -> falls back to safe generic text, never the raw JSON")
    void loadRecent_malformedJson_doesNotLeakRawJson() {
        AiReview success = review(14L, AiReviewStatus.SUCCESS, "{not-valid-json,,,");
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenReturn(List.of(success));

        PreviousAiReviewResult result = tool.loadRecent(TICKET_ID, 3);

        assertThat(result.getReviews().get(0).getSummary())
                .doesNotContain("not-valid-json")
                .contains("no readable summary");
    }

    @Test
    @DisplayName("resultJson without a summary field -> falls back to safe generic text, never raw JSON")
    void loadRecent_missingSummaryField_doesNotLeakRawJson() {
        AiReview success = review(15L, AiReviewStatus.SUCCESS,
                "{\"possibleCauses\":[\"fan failure\"],\"confidence\":\"LOW\"}");
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenReturn(List.of(success));

        PreviousAiReviewResult result = tool.loadRecent(TICKET_ID, 3);

        assertThat(result.getReviews().get(0).getSummary())
                .doesNotContain("possibleCauses")
                .doesNotContain("fan failure")
                .contains("no readable summary");
    }

    @Test
    @DisplayName("no previous reviews -> returns empty list")
    void loadRecent_noReviews_returnsEmptyList() {
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenReturn(List.of());

        PreviousAiReviewResult result = tool.loadRecent(TICKET_ID, 3);

        assertThat(result.getReviews()).isEmpty();
    }

    @Test
    @DisplayName("repository failure is wrapped into a controlled AgentToolException")
    void loadRecent_repositoryFailure_wrappedCleanly() {
        when(aiReviewRepository.findByTicketIdOrderByCreatedAtDesc(eq(TICKET_ID), any(Pageable.class)))
                .thenThrow(new RuntimeException("connection pool exhausted"));

        assertThatThrownBy(() -> tool.loadRecent(TICKET_ID, 3))
                .isInstanceOf(AgentToolException.class)
                .hasMessageContaining("Previous AI reviews unavailable")
                .hasMessageNotContaining("connection pool exhausted");
    }
}
