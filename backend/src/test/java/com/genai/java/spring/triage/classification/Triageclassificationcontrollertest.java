package com.genai.java.spring.triage.classification;

import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.dto.TriageClassificationResult;
import com.genai.java.spring.triage.graph.TriageClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageClassificationControllerTest {

    @Mock private TriageClassificationService service;

    private TriageClassificationController controller;

    @BeforeEach
    void setUp() {
        controller = new TriageClassificationController(service);
    }

    @Test
    @DisplayName("classify maps the service result to a TriageClassificationResult and returns 200 OK")
    void classify_delegatesToService_returnsMappedResult() {
        TriageClassification classification =
                new TriageClassification(1L, TicketCriticality.CRITICAL, "Safety hazard.");
        when(service.classify(1L)).thenReturn(classification);

        ResponseEntity<TriageClassificationResult> response = controller.classify(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTicketId()).isEqualTo(1L);
        assertThat(response.getBody().getCriticality()).isEqualTo(TicketCriticality.CRITICAL);
        assertThat(response.getBody().getRationale()).isEqualTo("Safety hazard.");
        assertThat(response.getBody().isFallbackApplied()).isFalse();
    }

    @Test
    @DisplayName("classify propagates the fallbackApplied flag from the service")
    void classify_fallbackApplied_isPropagated() {
        TriageClassification classification =
                new TriageClassification(2L, TicketCriticality.MEDIUM, "Automatic classification failed.");
        classification.setFallbackApplied(true);
        when(service.classify(2L)).thenReturn(classification);

        ResponseEntity<TriageClassificationResult> response = controller.classify(2L);

        assertThat(response.getBody().isFallbackApplied()).isTrue();
    }
}