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
     * topK is accepted for interface parity with the story spec; the
     * underlying retriever already applies its own configured top-k /
     * rerank pipeline (app.rag.top-k).
     */
    public TicketEvidenceResult retrieve(Ticket ticket, int topK) {
        try {
            List<EvidenceChunkResponse> evidence = evidenceRetriever.retrieve(ticket);
            return TicketEvidenceResult.of(ticket.getId(), evidence);
        } catch (Exception e) {
            log.error("TicketEvidenceTool failed for ticketId={}", ticket.getId(), e);
            throw new AgentToolException("Evidence retrieval unavailable.");
        }
    }
}
