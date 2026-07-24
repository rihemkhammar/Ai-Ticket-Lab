package com.genai.java.spring.observability;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates unique trace IDs for AI workflows .
 *
 * Format: ai-trace-<uuid> — readable prefix, UUID-based to avoid collisions.
 * A single trace ID is generated once per agent/HITL run and propagated to
 * every tool call and checkpoint created during that run.
 */
@Component
public class AiTraceIdGenerator {

    private static final String PREFIX = "ai-trace-";

    public String generate() {
        return PREFIX + UUID.randomUUID();
    }
}