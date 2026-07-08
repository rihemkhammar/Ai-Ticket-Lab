package com.genai.java.spring.agent.tool;

/**
 * Controlled error thrown by any agent tool. Never wraps or leaks raw stack
 * traces to the caller — only a clean, safe message .
 */
public class AgentToolException extends RuntimeException {
    public AgentToolException(String message) {
        super(message);
    }

    public AgentToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
