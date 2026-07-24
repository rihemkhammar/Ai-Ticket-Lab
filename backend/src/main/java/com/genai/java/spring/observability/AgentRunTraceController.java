package com.genai.java.spring.observability;

import com.genai.java.spring.observability.dto.AgentRunTraceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/agent-runs/{runId}/trace
 * GET /api/ai-traces/{traceId}
 */
@RestController
public class AgentRunTraceController {

    private final AgentRunTraceService traceService;

    public AgentRunTraceController(AgentRunTraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/api/agent-runs/{runId}/trace")
    public ResponseEntity<AgentRunTraceResponse> getTraceByRunId(@PathVariable Long runId) {
        return traceService.getTraceByRunId(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/ai-traces/{traceId}")
    public ResponseEntity<AgentRunTraceResponse> getTraceByTraceId(@PathVariable String traceId) {
        return traceService.getTraceByTraceId(traceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}