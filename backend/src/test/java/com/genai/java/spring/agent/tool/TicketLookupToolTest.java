package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.TicketLookupResult;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLookupToolTest {

    @Mock private TicketService ticketService;

    private TicketLookupTool tool;

    private static final Long TICKET_ID = 1L;

    @BeforeEach
    void setUp() {
        tool = new TicketLookupTool(ticketService);
    }

    @Test
    @DisplayName("returns ticket details for an existing ticket")
    void lookup_returnsTicketDetails() {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(ticket.getTitle()).thenReturn("Conveyor motor overheating");
        when(ticket.getDescription()).thenReturn("Motor temperature increases after 20 minutes.");
        when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);

        TicketLookupResult result = tool.lookup(TICKET_ID);

        assertThat(result.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(result.getTitle()).isEqualTo("Conveyor motor overheating");
        assertThat(result.getDescription()).isEqualTo("Motor temperature increases after 20 minutes.");
        assertThat(result.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("fails cleanly for a missing ticket, without leaking internal exception details")
    void lookup_missingTicket_failsCleanly() {
        when(ticketService.findById(TICKET_ID)).thenThrow(new TicketNotFoundException(TICKET_ID));

        assertThatThrownBy(() -> tool.lookup(TICKET_ID))
                .isInstanceOf(AgentToolException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    @DisplayName("unexpected repository failure is wrapped into a controlled AgentToolException")
    void lookup_unexpectedFailure_wrappedCleanly() {
        when(ticketService.findById(TICKET_ID)).thenThrow(new RuntimeException("DB connection reset"));

        assertThatThrownBy(() -> tool.lookup(TICKET_ID))
                .isInstanceOf(AgentToolException.class)
                .hasMessageContaining("Ticket lookup failed")
                .hasMessageNotContaining("DB connection reset");
    }
}
