package com.genai.java.spring.hitl.controller;

import com.genai.java.spring.hitl.dto.HumanReviewDecisionRequest;
import com.genai.java.spring.hitl.dto.HumanReviewDecisionResponse;
import com.genai.java.spring.hitl.service.HumanReviewDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/agent-runs/{runId}/human-review/decision
 *
 * Single unified endpoint for APPROVE / REJECT / REQUEST_REVISION.
 * Never mutates ticket state — see HumanReviewDecisionService, which does
 * not even depend on TicketService.
 */
@RestController
public class HumanReviewDecisionController {

    private final HumanReviewDecisionService humanReviewDecisionService;

    public HumanReviewDecisionController(HumanReviewDecisionService humanReviewDecisionService) {
        this.humanReviewDecisionService = humanReviewDecisionService;
    }

    @PostMapping("/api/agent-runs/{runId}/human-review/decision")
    public ResponseEntity<HumanReviewDecisionResponse> decide(@PathVariable Long runId,
                                                                @RequestBody HumanReviewDecisionRequest request) {
        HumanReviewDecisionResponse response = humanReviewDecisionService.applyDecision(runId, request);
        return ResponseEntity.ok(response);
    }
}
