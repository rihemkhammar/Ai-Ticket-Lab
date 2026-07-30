package com.genai.java.spring.triage.graph;

import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.review.TicketRagReviewService;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewNodeTest {

    @Mock private TicketRagReviewService ragReviewService;

    private ReviewNode node;

    @BeforeEach
    void setUp() {
        node = new ReviewNode(ragReviewService);
    }

    @Test
    @DisplayName("apply stores the review result when the RAG review succeeds")
    void apply_success_storesReviewResult() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setStatus(AiReviewStatus.SUCCESS);
        when(ragReviewService.runRagReview(eq(1L), anyString())).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentReviewResult()).isEqualTo(response);
        assertThat(result.getCurrentStageError()).isNull();
    }

    @Test
    @DisplayName("apply uses the fixed system requester identifier when no authenticated technician is on the state")
    void apply_usesFixedSystemRequester() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setStatus(AiReviewStatus.SUCCESS);
        when(ragReviewService.runRagReview(eq(1L), anyString())).thenReturn(response);

        node.apply(state);

        verify(ragReviewService).runRagReview(1L, "triage-batch-orchestrator");
    }

    @Test
    @DisplayName("apply uses the authenticated technician's username when the state carries one")
    void apply_usesAuthenticatedRequesterWhenPresent() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.setRequesterUsername("alice");

        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setStatus(AiReviewStatus.SUCCESS);
        when(ragReviewService.runRagReview(eq(1L), anyString())).thenReturn(response);

        node.apply(state);

        verify(ragReviewService).runRagReview(1L, "alice");
    }

    @Test
    @DisplayName("apply records currentStageError when the review status is FAILED")
    void apply_reviewFailed_recordsStageError() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setStatus(AiReviewStatus.FAILED);
        response.setErrorMessage("Grounding check failed.");
        when(ragReviewService.runRagReview(eq(1L), anyString())).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("Grounding check failed.");
        assertThat(result.getCurrentReviewResult()).isNull();
    }

    @Test
    @DisplayName("apply falls back to a generic message when a FAILED response has no error message")
    void apply_reviewFailedNoMessage_usesGenericMessage() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setStatus(AiReviewStatus.FAILED);
        when(ragReviewService.runRagReview(eq(1L), anyString())).thenReturn(response);

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("Recommendation review failed.");
    }

    @Test
    @DisplayName("apply records currentStageError when the review service throws")
    void apply_exception_recordsStageError() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);

        when(ragReviewService.runRagReview(eq(1L), anyString()))
                .thenThrow(new RuntimeException("vector store unavailable"));

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).contains("Recommendation review failed");
        assertThat(result.getCurrentStageError()).contains("vector store unavailable");
    }

    @Test
    @DisplayName("apply does nothing when currentTicketId is null")
    void apply_nullTicketId_doesNothing() {
        TriageGraphState state = new TriageGraphState();

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentReviewResult()).isNull();
        verify(ragReviewService, never()).runRagReview(any(), anyString());
    }

    @Test
    @DisplayName("apply skips the review call when an earlier stage already recorded an error")
    void apply_earlierStageError_skipsReview() {
        TriageGraphState state = new TriageGraphState();
        state.setCurrentTicketId(1L);
        state.setCurrentStageError("investigation failed earlier");

        TriageGraphState result = node.apply(state);

        assertThat(result.getCurrentStageError()).isEqualTo("investigation failed earlier");
        verify(ragReviewService, never()).runRagReview(any(), anyString());
    }
}