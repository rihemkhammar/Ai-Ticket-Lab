package com.genai.java.spring.hitl;

/**
 * Lifecycle status of a single {@link AgentReviewCheckpoint} row.
 *
 * PENDING     - waiting for human decision
 * SUPERSEDED  - replaced by a revised checkpoint (REQUEST_REVISION)
 * FINALIZED   - produced the finalized reviewed result (APPROVE)
 * REJECTED    - human rejected this checkpoint (REJECT)
 */
public enum ReviewCheckpointStatus {
    PENDING,
    SUPERSEDED,
    FINALIZED,
    REJECTED
}
