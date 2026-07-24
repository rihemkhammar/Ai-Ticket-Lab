package com.genai.java.spring.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured logging for AI workflow events .
 *
 * Every log line carries ONLY safe metadata: traceId, runId, ticketId,
 * runType, status, eventName, durationMs. This component must NEVER be
 * given API keys, raw prompts, raw model responses, or hidden
 * chain-of-thought — callers are responsible for not passing that data in.
 *
 * Example events: AI_RUN_STARTED, TOOL_CALL_STARTED, TOOL_CALL_COMPLETED,
 * CHECKPOINT_CREATED, HUMAN_DECISION_RECORDED, AI_RUN_FINALIZED, AI_RUN_FAILED.
 */
@Slf4j
@Component
public class AiWorkflowLogger {

    public void logEvent(String eventName, String traceId, Long runId, Long ticketId,
                         String runType, String status, Long durationMs) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", eventName);
        fields.put("traceId", traceId);
        fields.put("runId", runId);
        fields.put("ticketId", ticketId);
        fields.put("runType", runType);
        fields.put("status", status);
        if (durationMs != null) {
            fields.put("durationMs", durationMs);
        }
        log.info("[AI_TRACE] {}", fields);
    }

    public void logError(String eventName, String traceId, Long runId, Long ticketId,
                         String runType, String errorSummary) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", eventName);
        fields.put("traceId", traceId);
        fields.put("runId", runId);
        fields.put("ticketId", ticketId);
        fields.put("runType", runType);
        // Short error summary only — never the full stack trace or raw model output.
        fields.put("error", errorSummary);
        log.warn("[AI_TRACE] {}", fields);
    }
}