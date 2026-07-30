package com.genai.java.spring.triage;

/**
 * Thrown when a triage batch request is invalid: empty ticket list,
 * or batch size exceeding the 5-ticket limit (Rule 2.11).
 * Handled locally by TriageController, same pattern as
 * AgentValidationException in TicketAgentInvestigationController.
 */
public class TriageValidationException extends RuntimeException {
    public TriageValidationException(String message) {
        super(message);
    }
}