package com.genai.java.spring.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  trace ID generation ( "trace ID is generated
 * for HITL run").
 */
class AiTraceIdGeneratorTest {

    private final AiTraceIdGenerator generator = new AiTraceIdGenerator();

    @Test
    @DisplayName("generate() returns a non-null, non-blank trace id")
    void generate_returnsNonBlankId() {
        String traceId = generator.generate();

        assertThat(traceId).isNotNull();
        assertThat(traceId).isNotBlank();
    }

    @Test
    @DisplayName("generate() uses the readable 'ai-trace-' prefix")
    void generate_usesReadablePrefix() {
        String traceId = generator.generate();

        assertThat(traceId).startsWith("ai-trace-");
    }

    @Test
    @DisplayName("generate() produces a UUID-based suffix so ids do not collide")
    void generate_suffixIsUuidShaped() {
        String traceId = generator.generate();
        String suffix = traceId.substring("ai-trace-".length());

        // UUID.toString() shape: 8-4-4-4-12 hex chars.
        assertThat(suffix).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("generate() never returns the same trace id twice across many calls")
    void generate_isUniquePerCall() {
        int sampleSize = 500;

        Set<String> traceIds = new HashSet<>();
        IntStream.range(0, sampleSize).forEach(i -> traceIds.add(generator.generate()));

        assertThat(traceIds).hasSize(sampleSize);
    }
}