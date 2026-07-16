package com.genai.java.spring.agent;

public enum AgentRunStatus {
    // Ticket Agent Investigation — read-only, no pause
    RUNNING,
    SUCCESS,
    FAILED,

    // Human-in-the-Loop Agent Review
    WAITING_FOR_HUMAN,
    REVISING,
    FINALIZED,
    REJECTED
}
