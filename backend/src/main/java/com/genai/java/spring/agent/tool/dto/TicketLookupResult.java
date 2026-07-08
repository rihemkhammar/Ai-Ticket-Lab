package com.genai.java.spring.agent.tool.dto;

public class TicketLookupResult {

    private Long ticketId;
    private String title;
    private String description;
    private String status;

    public static TicketLookupResult of(Long ticketId, String title, String description, String status) {
        TicketLookupResult r = new TicketLookupResult();
        r.ticketId = ticketId;
        r.title = title;
        r.description = description;
        r.status = status;
        return r;
    }

    public Long getTicketId()               { return ticketId; }
    public void setTicketId(Long v)         { this.ticketId = v; }

    public String getTitle()                { return title; }
    public void setTitle(String v)          { this.title = v; }

    public String getDescription()          { return description; }
    public void setDescription(String v)    { this.description = v; }

    public String getStatus()               { return status; }
    public void setStatus(String v)         { this.status = v; }
}
