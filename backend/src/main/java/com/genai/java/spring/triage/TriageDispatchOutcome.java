package com.genai.java.spring.triage;

/**
 * Outcome of dispatching a single ticket to the existing M4
 * TicketAgentInvestigationService, as recorded in a triage_run's
 * treated list.
 */
public enum TriageDispatchOutcome {
    SUCCESS,
    FAILED
}