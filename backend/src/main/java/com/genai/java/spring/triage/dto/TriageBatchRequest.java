package com.genai.java.spring.triage.dto;

import java.util.List;

public class TriageBatchRequest {

    private List<Long> ticketIds;
    private boolean includeAllOpenTickets;

    public TriageBatchRequest() {}

    public List<Long> getTicketIds()                     { return ticketIds; }
    public void setTicketIds(List<Long> v)                { this.ticketIds = v; }

    public boolean isIncludeAllOpenTickets()              { return includeAllOpenTickets; }
    public void setIncludeAllOpenTickets(boolean v)       { this.includeAllOpenTickets = v; }
}