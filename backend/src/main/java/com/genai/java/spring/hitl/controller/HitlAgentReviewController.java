package com.genai.java.spring.hitl.controller;

import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.hitl.dto.HitlReviewRequest;
import com.genai.java.spring.hitl.dto.HitlReviewResponse;
import com.genai.java.spring.hitl.service.HitlAgentReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/tickets/{ticketId}/agent/hitl-review
 *
 * Runs the  investigation tool chain, drafts a recommendation, and
 * pauses at a persisted human review checkpoint. Never finalizes
 * immediately — always returns WAITING_FOR_HUMAN (or FAILED).
 */
@RestController
@RequestMapping("/api/tickets/{ticketId}/agent")
public class HitlAgentReviewController {

    private final HitlAgentReviewService hitlAgentReviewService;

    public HitlAgentReviewController(HitlAgentReviewService hitlAgentReviewService) {
        this.hitlAgentReviewService = hitlAgentReviewService;
    }

    @PostMapping("/hitl-review")
    public ResponseEntity<HitlReviewResponse> runHitlReview(@PathVariable Long ticketId,
                                                            @RequestBody(required = false) HitlReviewRequest request) {
        HitlReviewRequest effectiveRequest = request != null ? request : new HitlReviewRequest();
        HitlReviewResponse response = hitlAgentReviewService.startReview(ticketId, effectiveRequest);

        // A FAILED HITL attempt never created a pending human checkpoint — returning
        // 201 CREATED in that case would be misleading to callers/frontend (S5-G04).
        if (response.getStatus() == AgentRunStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Reloads the latest HITL run of a ticket (pending/finalized/rejected), used on page load/refresh. */
    @GetMapping("/hitl-review")
    public ResponseEntity<HitlReviewResponse> getLatestHitlReview(@PathVariable Long ticketId) {
        return hitlAgentReviewService.getLatestReview(ticketId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Reloads the latest pending/finalized checkpoint of a specific run (page refresh). */
    @GetMapping("/hitl-review/{runId}")
    public ResponseEntity<HitlReviewResponse> reloadHitlReview(@PathVariable Long ticketId, @PathVariable Long runId) {
        return hitlAgentReviewService.reloadPendingReview(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}