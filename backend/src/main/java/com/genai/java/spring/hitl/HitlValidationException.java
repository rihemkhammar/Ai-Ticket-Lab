package com.genai.java.spring.hitl;

/**
 * Raised for any HITL business-rule violation: invalid decision, missing
 * required comment, decision on a non-waiting run, invalid state
 * transition, revision-limit exceeded, malformed-JSON-after-retry, etc.
 * Mapped to HTTP 422 by GlobalExceptionHandler.
 */
public class HitlValidationException extends RuntimeException {
    public HitlValidationException(String message) {
        super(message);
    }
}
