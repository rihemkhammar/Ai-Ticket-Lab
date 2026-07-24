package com.genai.java.spring.agent;

/**
 * Type of AI workflow that produced an agent_run row.
 * Used purely for trace/observability display — does not affect
 * agent behavior.
 */
public enum AgentRunType {
    AGENT_INVESTIGATION,
    HITL_AGENT_REVIEW
}