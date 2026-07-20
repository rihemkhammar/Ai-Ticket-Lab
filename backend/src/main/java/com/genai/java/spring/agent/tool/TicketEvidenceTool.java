package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.TicketEvidenceResult;
import com.genai.java.spring.rag.retrieval.TicketEvidenceRetriever;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool 2 — TicketEvidenceTool .
 * Read-only. Reuses the existing M3 TicketEvidenceRetriever. Does not
 * create chunks, modify articles, or invent evidence.
 */
@Slf4j
@Component
public class TicketEvidenceTool {

    public static final String NAME = "TicketEvidenceTool";

    private final TicketEvidenceRetriever evidenceRetriever;

    public TicketEvidenceTool(TicketEvidenceRetriever evidenceRetriever) {
        this.evidenceRetriever = evidenceRetriever;
    }

    /**
     * topK is now honored end-to-end — it is forwarded to
     * TicketEvidenceRetriever#retrieve(ticket, topK), which overrides the
     * configured app.rag.top-k for this call (clamped to a safe range).
     */
    public TicketEvidenceResult retrieve(Ticket ticket, int topK) {
        try {
            List<EvidenceChunkResponse> evidence = evidenceRetriever.retrieve(ticket, topK);
            return TicketEvidenceResult.of(ticket.getId(), evidence);
        } catch (Exception e) {
            log.error("TicketEvidenceTool failed for ticketId={}", ticket.getId(), e);
            throw new AgentToolException("Evidence retrieval unavailable.");
        }
    }
}
