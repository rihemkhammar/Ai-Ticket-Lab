package com.genai.java.spring.observability;

import com.genai.java.spring.observability.dto.AgentCheckpointTraceResponse;
import com.genai.java.spring.observability.dto.AgentRunTraceResponse;
import com.genai.java.spring.observability.dto.AgentToolCallTraceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/agent-runs/{runId}/trace and
 *  (optional): GET /api/ai-traces/{traceId}.
 * Standalone MockMvc test (no Spring context, no security filters).
 */
@ExtendWith(MockitoExtension.class)
class AgentRunTraceControllerTest {

    @Mock private AgentRunTraceService traceService;

    private MockMvc mockMvc;

    private static final Long RUN_ID = 42L;
    private static final String TRACE_ID = "ai-trace-abc-123";

    @BeforeEach
    void setUp() {
        AgentRunTraceController controller = new AgentRunTraceController(traceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private AgentRunTraceResponse fullTrace() {
        AgentRunTraceResponse trace = new AgentRunTraceResponse();
        trace.setTraceId(TRACE_ID);
        trace.setRunId(RUN_ID);
        trace.setTicketId(1L);
        trace.setRunType("HITL_AGENT_REVIEW");
        trace.setStatus("FINALIZED");
        trace.setPromptVersion("ticket-agent-investigation-v1");
        trace.setModelName("openai/gpt-oss-20b");
        trace.setStartedAt(LocalDateTime.of(2026, 6, 8, 10, 0, 0));
        trace.setCompletedAt(LocalDateTime.of(2026, 6, 8, 10, 1, 12));
        trace.setDurationMs(72000L);
        trace.setOfficialActionExecuted(false);
        trace.setTicketStatusChanged(false);

        AgentToolCallTraceResponse toolCall = new AgentToolCallTraceResponse();
        toolCall.setToolName("TicketLookupTool");
        toolCall.setStatus("SUCCESS");
        toolCall.setDurationMs(15L);
        trace.setToolCalls(List.of(toolCall));

        AgentCheckpointTraceResponse checkpoint = new AgentCheckpointTraceResponse();
        checkpoint.setCheckpointNumber(1);
        checkpoint.setStatus("FINALIZED");
        checkpoint.setHumanDecision("APPROVE");
        trace.setCheckpoints(List.of(checkpoint));
        trace.setHumanDecisions(List.of(checkpoint));

        return trace;
    }

    @Test
    @DisplayName("GET /api/agent-runs/{runId}/trace returns 200 with full trace payload")
    void getTraceByRunId_returnsFullTrace() throws Exception {
        when(traceService.getTraceByRunId(RUN_ID)).thenReturn(Optional.of(fullTrace()));

        mockMvc.perform(get("/api/agent-runs/{runId}/trace", RUN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(jsonPath("$.runId").value(RUN_ID))
                .andExpect(jsonPath("$.runType").value("HITL_AGENT_REVIEW"))
                .andExpect(jsonPath("$.status").value("FINALIZED"))
                .andExpect(jsonPath("$.promptVersion").value("ticket-agent-investigation-v1"))
                .andExpect(jsonPath("$.modelName").value("openai/gpt-oss-20b"))
                .andExpect(jsonPath("$.durationMs").value(72000))
                .andExpect(jsonPath("$.officialActionExecuted").value(false))
                .andExpect(jsonPath("$.ticketStatusChanged").value(false))
                .andExpect(jsonPath("$.toolCalls[0].toolName").value("TicketLookupTool"))
                .andExpect(jsonPath("$.checkpoints[0].humanDecision").value("APPROVE"))
                .andExpect(jsonPath("$.humanDecisions[0].humanDecision").value("APPROVE"));
    }

    @Test
    @DisplayName("GET /api/agent-runs/{runId}/trace returns 404 when the run does not exist")
    void getTraceByRunId_returns404WhenMissing() throws Exception {
        when(traceService.getTraceByRunId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/agent-runs/{runId}/trace", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/ai-traces/{traceId} returns 200 with the same trace payload")
    void getTraceByTraceId_returnsTrace() throws Exception {
        when(traceService.getTraceByTraceId(TRACE_ID)).thenReturn(Optional.of(fullTrace()));

        mockMvc.perform(get("/api/ai-traces/{traceId}", TRACE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(jsonPath("$.runId").value(RUN_ID));
    }

    @Test
    @DisplayName("GET /api/ai-traces/{traceId} returns 404 when the trace id is unknown")
    void getTraceByTraceId_returns404WhenMissing() throws Exception {
        when(traceService.getTraceByTraceId("unknown-trace")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ai-traces/{traceId}", "unknown-trace"))
                .andExpect(status().isNotFound());
    }
}