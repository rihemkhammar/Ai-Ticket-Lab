package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *  optional debug endpoint to inspect what evidence would be
 * retrieved for a ticket, without running the full AI review.
 */
@RestController
@RequestMapping("/api/tickets")
public class EvidenceController {

    private final TicketService ticketService;
    private final TicketEvidenceRetriever retriever;

    public EvidenceController(TicketService ticketService, TicketEvidenceRetriever retriever) {
        this.ticketService = ticketService;
        this.retriever = retriever;
    }

    @GetMapping("/{ticketId}/evidence")
    public TicketEvidenceResponse getEvidence(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.findById(ticketId);
        List<EvidenceChunkResponse> evidence = retriever.retrieve(ticket);
        return new TicketEvidenceResponse(ticketId, evidence);
    }

    public record TicketEvidenceResponse(Long ticketId, List<EvidenceChunkResponse> evidence) {}
}