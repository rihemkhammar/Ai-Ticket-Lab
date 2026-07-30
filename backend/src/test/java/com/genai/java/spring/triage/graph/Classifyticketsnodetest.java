package com.genai.java.spring.triage.graph;

import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.classification.TriageClassificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassifyTicketsNodeTest {

    @Mock private TriageClassificationService classificationService;

    private ClassifyTicketsNode node;

    @BeforeEach
    void setUp() {
        node = new ClassifyTicketsNode(classificationService);
    }

    @Test
    @DisplayName("apply classifies every ticket in the queue and stores results by ticket id")
    void apply_classifiesEveryTicketInQueue() {
        TriageGraphState state = new TriageGraphState(1L, List.of(1L, 2L));

        when(classificationService.classify(1L))
                .thenReturn(new TriageClassification(1L, TicketCriticality.CRITICAL, "Safety hazard."));
        when(classificationService.classify(2L))
                .thenReturn(new TriageClassification(2L, TicketCriticality.LOW, "Minor issue."));

        TriageGraphState result = node.apply(state);

        assertThat(result.getClassifications()).hasSize(2);
        assertThat(result.getClassifications().get(1L).getCriticality()).isEqualTo(TicketCriticality.CRITICAL);
        assertThat(result.getClassifications().get(2L).getCriticality()).isEqualTo(TicketCriticality.LOW);
        verify(classificationService).classify(1L);
        verify(classificationService).classify(2L);
    }

    @Test
    @DisplayName("apply drains the ticket queue once classification is done")
    void apply_drainsTicketQueue() {
        TriageGraphState state = new TriageGraphState(1L, List.of(1L));
        when(classificationService.classify(1L))
                .thenReturn(new TriageClassification(1L, TicketCriticality.MEDIUM, "Routine."));

        TriageGraphState result = node.apply(state);

        assertThat(result.getTicketQueue()).isEmpty();
    }

    @Test
    @DisplayName("apply on an empty queue leaves classifications empty")
    void apply_emptyQueue_doesNothing() {
        TriageGraphState state = new TriageGraphState(1L, List.of());

        TriageGraphState result = node.apply(state);

        assertThat(result.getClassifications()).isEmpty();
        assertThat(result.getTicketQueue()).isEmpty();
    }
}