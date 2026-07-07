package com.genai.java.spring.aireview;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiReviewRepository extends JpaRepository<AiReview, Long> {

    // Used by PreviousAiReviewTool (M4 agent) to load the N most recent
    // reviews for a ticket without exposing raw provider errors.
    List<AiReview> findByTicketIdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);
}