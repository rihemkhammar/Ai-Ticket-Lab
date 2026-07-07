package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.TicketEvidenceResult;
import com.genai.java.spring.rag.retrieval.TicketEvidenceRetriever;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketEvidenceToolTest {

    @Mock private TicketEvidenceRetriever evidenceRetriever;

    private TicketEvidenceTool tool;

    private static final Long TICKET_ID = 1L;

    @BeforeEach
    void setUp() {
        tool = new TicketEvidenceTool(evidenceRetriever);
    }

    private EvidenceChunkResponse chunk() {
        return EvidenceChunkResponse.of(1L, 0, "Motor overheating can be caused by...",
                "Conveyor Motor Overheating Troubleshooting", "CONVEYOR", 0.91);
    }

    @Test
    @DisplayName("returns retrieved evidence chunks for the ticket")
    void retrieve_returnsEvidenceChunks() {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(evidenceRetriever.retrieve(ticket)).thenReturn(List.of(chunk()));

        TicketEvidenceResult result = tool.retrieve(ticket, 3);

        assertThat(result.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(result.getEvidence()).hasSize(1);
        assertThat(result.getEvidence().get(0).getSourceRef()).isEqualTo("article:1#chunk:0");
    }

    @Test
    @DisplayName("returns an empty list when no evidence is found, without inventing any")
    void retrieve_noEvidence_returnsEmptyList() {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(evidenceRetriever.retrieve(ticket)).thenReturn(List.of());

        TicketEvidenceResult result = tool.retrieve(ticket, 3);

        assertThat(result.getEvidence()).isEmpty();
    }

    @Test
    @DisplayName("retrieval failure is wrapped into a controlled AgentToolException")
    void retrieve_failure_wrappedCleanly() {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(evidenceRetriever.retrieve(ticket)).thenThrow(new RuntimeException("pgvector timeout"));

        assertThatThrownBy(() -> tool.retrieve(ticket, 3))
                .isInstanceOf(AgentToolException.class)
                .hasMessageContaining("Evidence retrieval unavailable")
                .hasMessageNotContaining("pgvector timeout");
    }
}
