package com.genai.java.spring.agent.dto;

/**
 * Operational tool-call trace shown to the frontend.
 * Deliberately shallow: tool name + status only .
 * This is NOT a chain-of-thought transcript .
 */
public class AgentToolCallTrace {

    private String toolName;
    private String status;
    private String errorMessage;

    public AgentToolCallTrace() {}

    public AgentToolCallTrace(String toolName, String status, String errorMessage) {
        this.toolName = toolName;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getToolName()             { return toolName; }
    public void setToolName(String v)       { this.toolName = v; }

    public String getStatus()                { return status; }
    public void setStatus(String v)          { this.status = v; }

    public String getErrorMessage()          { return errorMessage; }
    public void setErrorMessage(String v)    { this.errorMessage = v; }
}
