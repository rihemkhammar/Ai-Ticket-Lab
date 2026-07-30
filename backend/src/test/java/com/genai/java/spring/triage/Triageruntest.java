package com.genai.java.spring.triage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TriageRunTest {

    @Test
    @DisplayName("getters return the values set via setters")
    void settersAndGetters_roundTrip() {
        TriageRun run = new TriageRun();
        LocalDateTime now = LocalDateTime.of(2026, 7, 30, 10, 0);

        run.setStatus(TriageRunStatus.RUNNING);
        run.setPromptVersion("ticket-triage-classification-v1");
        run.setModelName("openai/gpt-oss-20b");
        run.setTicketQueue("[1,2,3]");
        run.setClassificationsJson("{}");
        run.setTreatedJson("[]");
        run.setErrorMessage("some error");
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        run.setCompletedAt(now);

        assertThat(run.getStatus()).isEqualTo(TriageRunStatus.RUNNING);
        assertThat(run.getPromptVersion()).isEqualTo("ticket-triage-classification-v1");
        assertThat(run.getModelName()).isEqualTo("openai/gpt-oss-20b");
        assertThat(run.getTicketQueue()).isEqualTo("[1,2,3]");
        assertThat(run.getClassificationsJson()).isEqualTo("{}");
        assertThat(run.getTreatedJson()).isEqualTo("[]");
        assertThat(run.getErrorMessage()).isEqualTo("some error");
        assertThat(run.getCreatedAt()).isEqualTo(now);
        assertThat(run.getUpdatedAt()).isEqualTo(now);
        assertThat(run.getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("id is null before persistence (no explicit id assigned)")
    void id_isNullBeforePersistence() {
        TriageRun run = new TriageRun();

        assertThat(run.getId()).isNull();
    }
}