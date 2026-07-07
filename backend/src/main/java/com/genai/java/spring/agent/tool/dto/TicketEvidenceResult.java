package com.genai.java.spring.agent.tool.dto;

import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;

import java.util.List;

public class TicketEvidenceResult {

    private Long ticketId;
    private List<EvidenceChunkResponse> evidence;

    public static TicketEvidenceResult of(Long ticketId, List<EvidenceChunkResponse> evidence) {
        TicketEvidenceResult r = new TicketEvidenceResult();
        r.ticketId = ticketId;
        r.evidence = evidence;
        return r;
    }

    public Long getTicketId()                          { return ticketId; }
    public void setTicketId(Long v)                    { this.ticketId = v; }

    public List<EvidenceChunkResponse> getEvidence()    { return evidence; }
    public void setEvidence(List<EvidenceChunkResponse> v) { this.evidence = v; }
}
