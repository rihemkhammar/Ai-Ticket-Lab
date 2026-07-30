package com.genai.java.spring.triage.dto;

import com.genai.java.spring.triage.TicketCriticality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriageClassificationResultTest {

    @Test
    @DisplayName("all-args constructor sets every field")
    void constructor_setsAllFields() {
        TriageClassificationResult result =
                new TriageClassificationResult(1L, TicketCriticality.HIGH, "Recurring issue.", false);

        assertThat(result.getTicketId()).isEqualTo(1L);
        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.HIGH);
        assertThat(result.getRationale()).isEqualTo("Recurring issue.");
        assertThat(result.isFallbackApplied()).isFalse();
    }

    @Test
    @DisplayName("no-args constructor and setters allow building the object incrementally")
    void noArgsConstructor_andSetters_roundTrip() {
        TriageClassificationResult result = new TriageClassificationResult();

        result.setTicketId(2L);
        result.setCriticality(TicketCriticality.LOW);
        result.setRationale("Minor issue.");
        result.setFallbackApplied(true);

        assertThat(result.getTicketId()).isEqualTo(2L);
        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.LOW);
        assertThat(result.getRationale()).isEqualTo("Minor issue.");
        assertThat(result.isFallbackApplied()).isTrue();
    }
}