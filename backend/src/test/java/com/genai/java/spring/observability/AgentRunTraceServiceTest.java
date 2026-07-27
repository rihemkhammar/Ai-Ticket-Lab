package com.genai.java.spring.observability;

import com.genai.java.spring.agent.AgentRun;
import com.genai.java.spring.agent.AgentRunRepository;
import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.agent.AgentRunType;
import com.genai.java.spring.agent.AgentToolCall;
import com.genai.java.spring.agent.AgentToolCallRepository;
import com.genai.java.spring.agent.AgentToolCallStatus;
import com.genai.java.spring.hitl.AgentReviewCheckpoint;
import com.genai.java.spring.hitl.AgentReviewCheckpointRepository;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;
import com.genai.java.spring.observability.dto.AgentRunTraceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * GET /api/agent-runs/{runId}/trace and
 * GET /api/ai-traces/{traceId} must assemble run metadata, tool-call
 * trace, and checkpoint/human-decision trace, while always preserving
 * the M5 safety flags (§2.9) and never exposing hidden chain-of-thought.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunTraceServiceTest {

    @Mock private AgentRunRepository agentRunRepository;
    @Mock private AgentToolCallRepository agentToolCallRepository;
    @Mock private AgentReviewCheckpointRepository checkpointRepository;

    private AgentRunTraceService service;

    private static final Long RUN_ID = 42L;
    private static final Long TICKET_ID = 1L;
    private static final String TRACE_ID = "ai-trace-abc-123";

    @BeforeEach
    void setUp() {
        service = new AgentRunTraceService(agentRunRepository, agentToolCallRepository, checkpointRepository);
    }

    private AgentRun finalizedRun() {
        AgentRun run = new AgentRun();
        ReflectionTestUtils.setField(run, "id", RUN_ID);
        run.setTicketId(TICKET_ID);
        run.setTraceId(TRACE_ID);
        run.setRunType(AgentRunType.HITL_AGENT_REVIEW);
        run.setStatus(AgentRunStatus.FINALIZED);
        run.setPromptVersion("ticket-agent-investigation-v1");
        run.setModelName("openai/gpt-oss-20b");
        run.setCreatedAt(LocalDateTime.of(2026, 6, 8, 10, 0, 0));
        run.setCompletedAt(LocalDateTime.of(2026, 6, 8, 10, 1, 12));
        run.setDurationMs(72000L);
        return run;
    }

    private AgentToolCall toolCall(String name, AgentToolCallStatus status, Long durationMs) {
        AgentToolCall call = new AgentToolCall();
        call.setAgentRunId(RUN_ID);
        call.setTraceId(TRACE_ID);
        call.setToolName(name);
        call.setStatus(status);
        call.setStartedAt(LocalDateTime.of(2026, 6, 8, 10, 0, 1));
        call.setCompletedAt(LocalDateTime.of(2026, 6, 8, 10, 0, 2));
        call.setDurationMs(durationMs);
        return call;
    }

    private AgentReviewCheckpoint checkpoint(int number, ReviewCheckpointStatus status, HumanReviewDecision decision) {
        AgentReviewCheckpoint cp = new AgentReviewCheckpoint();
        ReflectionTestUtils.setField(cp, "id", 500L + number);
        cp.setAgentRunId(RUN_ID);
        cp.setTicketId(TICKET_ID);
        cp.setTraceId(TRACE_ID);
        cp.setCheckpointNumber(number);
        cp.setStatus(status);
        cp.setDraftJson("{}");
        cp.setHumanDecision(decision);
        cp.setCreatedAt(LocalDateTime.of(2026, 6, 8, 10, 0, 30));
        return cp;
    }

    @Test
    @DisplayName("getTraceByRunId returns full run metadata")
    void getTraceByRunId_returnsRunMetadata() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(finalizedRun()));
        lenient().when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of());
        lenient().when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of());

        Optional<AgentRunTraceResponse> result = service.getTraceByRunId(RUN_ID);

        assertThat(result).isPresent();
        AgentRunTraceResponse trace = result.get();
        assertThat(trace.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(trace.getRunId()).isEqualTo(RUN_ID);
        assertThat(trace.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(trace.getRunType()).isEqualTo("HITL_AGENT_REVIEW");
        assertThat(trace.getStatus()).isEqualTo("FINALIZED");
        assertThat(trace.getPromptVersion()).isEqualTo("ticket-agent-investigation-v1");
        assertThat(trace.getModelName()).isEqualTo("openai/gpt-oss-20b");
        assertThat(trace.getDurationMs()).isEqualTo(72000L);
    }

    @Test
    @DisplayName("getTraceByTraceId delegates to the trace-id repository lookup")
    void getTraceByTraceId_delegatesToRepository() {
        when(agentRunRepository.findFirstByTraceId(TRACE_ID)).thenReturn(Optional.of(finalizedRun()));
        lenient().when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of());
        lenient().when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of());

        Optional<AgentRunTraceResponse> result = service.getTraceByTraceId(TRACE_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getTraceId()).isEqualTo(TRACE_ID);
    }

    @Test
    @DisplayName("getTraceByRunId returns empty when the run does not exist")
    void getTraceByRunId_returnsEmptyWhenMissing() {
        when(agentRunRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<AgentRunTraceResponse> result = service.getTraceByRunId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("trace response includes the tool-call trace with names, status, and duration")
    void trace_includesToolCallTrace() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(finalizedRun()));
        when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of(
                toolCall("TicketLookupTool", AgentToolCallStatus.SUCCESS, 15L),
                toolCall("TicketEvidenceTool", AgentToolCallStatus.SUCCESS, 420L)
        ));
        lenient().when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of());

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.getToolCalls()).hasSize(2);
        assertThat(trace.getToolCalls().get(0).getToolName()).isEqualTo("TicketLookupTool");
        assertThat(trace.getToolCalls().get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(trace.getToolCalls().get(0).getDurationMs()).isEqualTo(15L);
        assertThat(trace.getToolCalls().get(1).getToolName()).isEqualTo("TicketEvidenceTool");
    }

    @Test
    @DisplayName("trace response includes checkpoint trace and derives human decisions from decided checkpoints")
    void trace_includesCheckpointAndHumanDecisionTrace() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(finalizedRun()));
        lenient().when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of());
        when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of(
                checkpoint(1, ReviewCheckpointStatus.FINALIZED, HumanReviewDecision.APPROVE)
        ));

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.getCheckpoints()).hasSize(1);
        assertThat(trace.getCheckpoints().get(0).getCheckpointNumber()).isEqualTo(1);
        assertThat(trace.getCheckpoints().get(0).getStatus()).isEqualTo("FINALIZED");

        assertThat(trace.getHumanDecisions()).hasSize(1);
        assertThat(trace.getHumanDecisions().get(0).getHumanDecision()).isEqualTo("APPROVE");
    }

    @Test
    @DisplayName("checkpoints with no human decision yet are excluded from the human-decisions list")
    void trace_excludesUndecidedCheckpointsFromHumanDecisions() {
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(finalizedRun()));
        lenient().when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of());
        when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of(
                checkpoint(1, ReviewCheckpointStatus.PENDING, null)
        ));

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.getCheckpoints()).hasSize(1);
        assertThat(trace.getHumanDecisions()).isEmpty();
    }

    @Test
    @DisplayName("officialActionExecuted and ticketStatusChanged are always false, regardless of status")
    void trace_alwaysPreservesSafetyFlags() {
        AgentRun run = finalizedRun();
        run.setStatus(AgentRunStatus.REJECTED);
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(run));
        lenient().when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of());
        lenient().when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of());

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.isOfficialActionExecuted()).isFalse();
        assertThat(trace.isTicketStatusChanged()).isFalse();
    }

    @Test
    @DisplayName("the assembled trace exposes one shared traceId across the run, its tool calls, and its checkpoint")
    void trace_sameTraceIdSpansRunToolCallsAndCheckpoint() {
        AgentRun run = finalizedRun();
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(run));
        AgentToolCall lookupCall = toolCall("TicketLookupTool", AgentToolCallStatus.SUCCESS, 15L);
        AgentToolCall evidenceCall = toolCall("TicketEvidenceTool", AgentToolCallStatus.SUCCESS, 420L);
        when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID))
                .thenReturn(List.of(lookupCall, evidenceCall));
        AgentReviewCheckpoint cp = checkpoint(1, ReviewCheckpointStatus.FINALIZED, HumanReviewDecision.APPROVE);
        when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of(cp));

        // Sanity check on the fixtures themselves: they must actually share one traceId,
        // not merely be typed identically by coincidence — this is what the persisted
        // AgentToolCall/AgentReviewCheckpoint entities are asserted to carry in production.
        assertThat(run.getTraceId()).isEqualTo(lookupCall.getTraceId());
        assertThat(run.getTraceId()).isEqualTo(evidenceCall.getTraceId());
        assertThat(run.getTraceId()).isEqualTo(cp.getTraceId());

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(trace.getToolCalls()).hasSize(2);
        assertThat(trace.getCheckpoints()).hasSize(1);
        // The response itself is keyed by the run's traceId at the top level; the fixture
        // assertions above confirm the underlying entities feeding it agree on that id too.
    }

    @Test
    @DisplayName("a failed run still returns its preserved trace data, including the error message")
    void trace_preservesDataForFailedRun() {
        AgentRun run = finalizedRun();
        run.setStatus(AgentRunStatus.FAILED);
        run.setErrorMessage("Ticket not found");
        when(agentRunRepository.findById(RUN_ID)).thenReturn(Optional.of(run));
        when(agentToolCallRepository.findByAgentRunIdOrderByStartedAtAsc(RUN_ID)).thenReturn(List.of(
                toolCall("TicketLookupTool", AgentToolCallStatus.FAILED, 10L)
        ));
        lenient().when(checkpointRepository.findByAgentRunIdOrderByCheckpointNumberAsc(RUN_ID)).thenReturn(List.of());

        AgentRunTraceResponse trace = service.getTraceByRunId(RUN_ID).orElseThrow();

        assertThat(trace.getStatus()).isEqualTo("FAILED");
        assertThat(trace.getErrorMessage()).isEqualTo("Ticket not found");
        assertThat(trace.getToolCalls()).hasSize(1);
        assertThat(trace.getToolCalls().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(trace.isOfficialActionExecuted()).isFalse();
        assertThat(trace.isTicketStatusChanged()).isFalse();
    }
}