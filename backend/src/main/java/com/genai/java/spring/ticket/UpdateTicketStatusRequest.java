package com.genai.java.spring.ticket;

public class UpdateTicketStatusRequest {
    private TicketStatus status;

    public TicketStatus getStatus()       { return status; }
    public void setStatus(TicketStatus v) { this.status = v; }
}