package com.genai.java.spring.triage.dto;

import com.genai.java.spring.triage.TicketCriticality;

public class TriageClassificationResult {

    private Long ticketId;
    private TicketCriticality criticality;
    private String rationale;
    private boolean fallbackApplied;

    public TriageClassificationResult() {}

    public TriageClassificationResult(Long ticketId, TicketCriticality criticality,
                                      String rationale, boolean fallbackApplied) {
        this.ticketId = ticketId;
        this.criticality = criticality;
        this.rationale = rationale;
        this.fallbackApplied = fallbackApplied;
    }

    public Long getTicketId()                        { return ticketId; }
    public void setTicketId(Long v)                   { this.ticketId = v; }

    public TicketCriticality getCriticality()         { return criticality; }
    public void setCriticality(TicketCriticality v)   { this.criticality = v; }

    public String getRationale()                      { return rationale; }
    public void setRationale(String v)                { this.rationale = v; }

    public boolean isFallbackApplied()                { return fallbackApplied; }
    public void setFallbackApplied(boolean v)         { this.fallbackApplied = v; }
}