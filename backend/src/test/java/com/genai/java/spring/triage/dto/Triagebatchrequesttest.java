package com.genai.java.spring.triage.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageBatchRequestTest {

    @Test
    @DisplayName("default values are an empty/unset ticket id list and includeAllOpenTickets=false")
    void defaults_areEmptyAndFalse() {
        TriageBatchRequest request = new TriageBatchRequest();

        assertThat(request.getTicketIds()).isNull();
        assertThat(request.isIncludeAllOpenTickets()).isFalse();
    }

    @Test
    @DisplayName("getters return the values set via setters")
    void settersAndGetters_roundTrip() {
        TriageBatchRequest request = new TriageBatchRequest();

        request.setTicketIds(List.of(1L, 2L, 3L));
        request.setIncludeAllOpenTickets(true);

        assertThat(request.getTicketIds()).containsExactly(1L, 2L, 3L);
        assertThat(request.isIncludeAllOpenTickets()).isTrue();
    }
}