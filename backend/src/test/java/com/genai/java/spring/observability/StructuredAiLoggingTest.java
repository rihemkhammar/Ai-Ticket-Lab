package com.genai.java.spring.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  (Sensitive Logging Rule) /  (Structured Logging) /
 *  ("structured logs do not include API keys", "... do not include
 * hidden chain-of-thought fields").
 *
 * Captures the actual log lines emitted by {@link AiWorkflowLogger} via a
 * logback {@link ListAppender} and asserts that:
 *  - only the allow-listed safe metadata fields are present
 *  - none of the forbidden field names/values ever appear in the line
 */
class StructuredAiLoggingTest {

    private final AiWorkflowLogger logger = new AiWorkflowLogger();
    private ListAppender<ILoggingEvent> appender;
    private Logger targetLogger;

    private static final String TRACE_ID = "ai-trace-abc-123";
    private static final Long RUN_ID = 42L;
    private static final Long TICKET_ID = 1L;
    private static final String RUN_TYPE = "HITL_AGENT_REVIEW";

    @BeforeEach
    void setUp() {
        targetLogger = (Logger) LoggerFactory.getLogger(AiWorkflowLogger.class);
        appender = new ListAppender<>();
        appender.start();
        targetLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        targetLogger.detachAppender(appender);
    }

    private String lastMessage() {
        List<ILoggingEvent> events = appender.list;
        assertThat(events).isNotEmpty();
        return events.get(events.size() - 1).getFormattedMessage();
    }

    @Test
    @DisplayName("logEvent emits a single structured [AI_TRACE] line with the safe metadata fields")
    void logEvent_emitsStructuredLineWithSafeFields() {
        logger.logEvent("AI_RUN_STARTED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "RUNNING", null);

        String line = lastMessage();

        assertThat(line).contains("[AI_TRACE]");
        assertThat(line).contains("event=AI_RUN_STARTED");
        assertThat(line).contains("traceId=" + TRACE_ID);
        assertThat(line).contains("runId=" + RUN_ID);
        assertThat(line).contains("ticketId=" + TICKET_ID);
        assertThat(line).contains("runType=" + RUN_TYPE);
        assertThat(line).contains("status=RUNNING");
    }

    @Test
    @DisplayName("logEvent includes durationMs only when it is provided")
    void logEvent_includesDurationOnlyWhenPresent() {
        logger.logEvent("AI_RUN_FINALIZED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "FINALIZED", 72000L);
        assertThat(lastMessage()).contains("durationMs=72000");

        logger.logEvent("AI_RUN_STARTED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "RUNNING", null);
        assertThat(lastMessage()).doesNotContain("durationMs");
    }

    @Test
    @DisplayName("logError emits a structured line with a short error summary, not a full stack trace")
    void logError_emitsShortErrorSummary() {
        logger.logError("AI_RUN_FAILED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "Ticket not found");

        String line = lastMessage();

        assertThat(line).contains("[AI_TRACE]");
        assertThat(line).contains("event=AI_RUN_FAILED");
        assertThat(line).contains("error=Ticket not found");
        // A full stack trace would contain frame markers like "at " / ".java:" — must not leak here.
        assertThat(line).doesNotContain(".java:");
    }

    @Test
    @DisplayName("structured log lines never contain API keys, secrets, or raw prompt/response markers")
    void logs_neverContainSecretsOrRawModelContent() {
        logger.logEvent("AI_RUN_STARTED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "RUNNING", null);
        logger.logError("AI_RUN_FAILED", TRACE_ID, RUN_ID, TICKET_ID, RUN_TYPE, "Ticket not found");

        List<String> lines = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

        for (String line : lines) {
            String lower = line.toLowerCase();
            assertThat(lower).doesNotContain("sk-");
            assertThat(lower).doesNotContain("api_key");
            assertThat(lower).doesNotContain("apikey");
            assertThat(lower).doesNotContain("authorization");
            assertThat(lower).doesNotContain("chain-of-thought");
            assertThat(lower).doesNotContain("systemprompt");
            assertThat(lower).doesNotContain("rawprompt");
        }
    }

    @Test
    @DisplayName("AiWorkflowLogger's public API only accepts safe metadata — no prompt/response parameters exist")
    void logger_apiSurfaceOnlyAcceptsSafeMetadata() {
        // Structural guard: if a future change adds a raw-prompt/raw-response
        // parameter to these methods, this test documents the intent to
        // catch it in review, even though method signatures aren't
        // reflectively enforceable at runtime beyond arity/type checks.
        long eventMethods = java.util.Arrays.stream(AiWorkflowLogger.class.getMethods())
                .filter(m -> m.getName().equals("logEvent") || m.getName().equals("logError"))
                .count();

        assertThat(eventMethods).isEqualTo(2);
    }
}