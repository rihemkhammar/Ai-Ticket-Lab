package com.genai.java.spring.hitl.dto;

/**
 * Error body returned for HITL-specific validation failures .
 * "Only one revision cycle is supported in this training milestone.",
 * "comment is required for reject.", "run must be WAITING_FOR_HUMAN.".
 */
public class HitlErrorResponse {

    private String message;

    public HitlErrorResponse() {}
    public HitlErrorResponse(String message) { this.message = message; }

    public String getMessage()       { return message; }
    public void setMessage(String v) { this.message = v; }
}
