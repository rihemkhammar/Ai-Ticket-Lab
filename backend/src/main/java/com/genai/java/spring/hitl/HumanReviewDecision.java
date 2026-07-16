package com.genai.java.spring.hitl;

/**
 * Decision made by a human reviewer on a pending {@link AgentReviewCheckpoint}.
 * Source of truth for what the human chose .
 */
public enum HumanReviewDecision {
    APPROVE,
    REJECT,
    REQUEST_REVISION
}
