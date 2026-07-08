package com.genai.java.spring.aireview;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiReviewRepository extends JpaRepository<AiReview, Long> {

    // Used by PreviousAiReviewTool (M4 agent) to load the N most recent
    // reviews for a ticket without exposing raw provider errors.
    List<AiReview> findByTicketIdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);

    // used to redisplay the last review for a ticket (basic or
    // RAG, disambiguated by promptVersion) without re-running the LLM,
    // e.g. when the user navigates back to the ticket panel.
    Optional<AiReview> findFirstByTicketIdAndPromptVersionOrderByCreatedAtDesc(Long ticketId, String promptVersion);
}