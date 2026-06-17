package com.genai.java.spring.ticket;

import jakarta.validation.constraints.NotBlank;

public class CreateTicketRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    public String getTitle()              { return title; }
    public String getDescription()        { return description; }

    public void setTitle(String v)        { this.title = v; }
    public void setDescription(String v)  { this.description = v; }
}