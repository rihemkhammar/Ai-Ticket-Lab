package com.genai.java.spring.aireview.dto;

public class ErrorResponse {
    private String message;

    public ErrorResponse() {}
    public ErrorResponse(String message) { this.message = message; }

    public String getMessage()             { return message; }
    public void setMessage(String v)       { this.message = v; }
}