package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.classification.TriageClassificationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 1 - Triage. Classifies every ticket currently in
 * state.ticketQueue and stores the result in state.classifications.
 * Delegates entirely to TriageClassificationService (Phase 2), which
 * already handles the MEDIUM fallback on failure.
 */
@Component
public class ClassifyTicketsNode {

    private final TriageClassificationService classificationService;

    public ClassifyTicketsNode(TriageClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    public TriageGraphState apply(TriageGraphState state) {
        List<Long> queue = new ArrayList<>(state.getTicketQueue());

        for (Long ticketId : queue) {
            TriageClassification classification = classificationService.classify(ticketId);
            state.getClassifications().put(ticketId, classification);
        }

        // All tickets have now been classified; the remaining-to-classify
        // queue is drained. OrderQueueNode reads from classifications
        // (not from ticketQueue) to build orderedQueue.
        state.setTicketQueue(new ArrayList<>());

        return state;
    }
}