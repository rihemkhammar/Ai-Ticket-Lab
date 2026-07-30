package com.genai.java.spring.triage.classification;

import com.genai.java.spring.triage.dto.TriageClassificationResult;
import com.genai.java.spring.triage.graph.TriageClassification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optional endpoint to test Agent 1 (Triage classification) in
 * isolation, without creating a full triage_run batch. Not part of
 * the required  flow (Section 2.10 uses POST /api/triage/batches),
 * but useful during development/demo of Phase 2 before Phase 4 wiring
 * exists.
 */
@RestController
@RequestMapping("/api/triage")
public class TriageClassificationController {

    private final TriageClassificationService service;

    public TriageClassificationController(TriageClassificationService service) {
        this.service = service;
    }

    @PostMapping("/classify/{ticketId}")
    public ResponseEntity<TriageClassificationResult> classify(@PathVariable Long ticketId) {
        TriageClassification classification = service.classify(ticketId);

        TriageClassificationResult result = new TriageClassificationResult(
                classification.getTicketId(),
                classification.getCriticality(),
                classification.getRationale(),
                classification.isFallbackApplied()
        );
        return ResponseEntity.ok(result);
    }
}