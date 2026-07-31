package com.genai.java.spring.triage.dto;

import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.TriageRunStatus;
import com.genai.java.spring.triage.graph.TriageTreatedItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageRunResponseTest {

    @Test
    @DisplayName("getters return the values set via setters")
    void settersAndGetters_roundTrip() {
        TriageRunResponse response = new TriageRunResponse();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 30, 9, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 30, 9, 5);
        TriageTreatedItem item = TriageTreatedItem.success(1L, TicketCriticality.HIGH, 10L, completedAt,
                com.genai.java.spring.shared.advisor.TicketRoutingRules.RoutingDecision.STANDARD_HUMAN_REVIEW);

        response.setRunId(1L);
        response.setStatus(TriageRunStatus.COMPLETED);
        response.setPromptVersion("ticket-triage-classification-v1");
        response.setTicketQueue(List.of());
        response.setTreated(List.of(item));
        response.setErrorMessage(null);
        response.setCreatedAt(createdAt);
        response.setCompletedAt(completedAt);

        assertThat(response.getRunId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(TriageRunStatus.COMPLETED);
        assertThat(response.getPromptVersion()).isEqualTo("ticket-triage-classification-v1");
        assertThat(response.getTicketQueue()).isEmpty();
        assertThat(response.getTreated()).containsExactly(item);
        assertThat(response.getErrorMessage()).isNull();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("no-args constructor leaves all fields unset")
    void noArgsConstructor_leavesFieldsNull() {
        TriageRunResponse response = new TriageRunResponse();

        assertThat(response.getRunId()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getTicketQueue()).isNull();
        assertThat(response.getTreated()).isNull();
        assertThat(response.getErrorMessage()).isNull();
    }
}