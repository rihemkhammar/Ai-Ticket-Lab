package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of classifying one ticket by criticality.
 * Held in TriageGraphState.classifications, keyed by ticket id.
 */
@Getter
@Setter
@NoArgsConstructor
public class TriageClassification implements java.io.Serializable {

    private Long ticketId;
    private TicketCriticality criticality;
    private String rationale;

    /**
     * True when the LLM classification failed or returned an invalid
     * value and the service defaulted to MEDIUM (see Phase 2 rule:
     * never drop a ticket from the queue on classification failure).
     */
    private boolean fallbackApplied;

    public TriageClassification(Long ticketId, TicketCriticality criticality, String rationale) {
        this.ticketId = ticketId;
        this.criticality = criticality;
        this.rationale = rationale;
        this.fallbackApplied = false;
    }
}