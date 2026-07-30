package com.genai.java.spring.triage;

/**
 * Lifecycle status of a triage_run row.
 * PENDING    -> created, not yet started
 * RUNNING    -> LangGraph4j graph is executing (classify/order/dispatch)
 * COMPLETED  -> ticket queue is empty, all tickets have a treated entry
 * FAILED     -> the batch could not start at all (e.g. empty ticket list,
 *               or batch size exceeding the 5-ticket limit).
 *               Individual ticket dispatch failures do NOT produce this
 *               status — they are recorded per-ticket in treated_json
 *               with outcome = FAILED, and the run still reaches COMPLETED.
 */
public enum TriageRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}