package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.TicketLookupResult;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Tool 1 — TicketLookupTool .
 * Read-only. Loads ticket details for a ticket id. Never modifies the ticket.
 */
@Slf4j
@Component
public class TicketLookupTool {

    public static final String NAME = "TicketLookupTool";

    private final TicketService ticketService;

    public TicketLookupTool(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public TicketLookupResult lookup(Long ticketId) {
        try {
            Ticket ticket = ticketService.findById(ticketId);
            return TicketLookupResult.of(
                    ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                    ticket.getStatus() != null ? ticket.getStatus().name() : null);
        } catch (TicketNotFoundException e) {
            log.warn("TicketLookupTool: ticket not found ticketId={}", ticketId);
            throw new AgentToolException("Ticket not found: " + ticketId);
        } catch (Exception e) {
            log.error("TicketLookupTool failed for ticketId={}", ticketId, e);
            throw new AgentToolException("Ticket lookup failed.");
        }
    }
}
