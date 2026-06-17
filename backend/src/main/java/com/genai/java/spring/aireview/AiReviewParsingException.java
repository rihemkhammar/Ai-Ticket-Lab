package com.genai.java.spring.aireview;

public class AiReviewParsingException extends RuntimeException {
    public AiReviewParsingException(String message, Throwable cause) {
        super(message, cause);
    }
    public AiReviewParsingException(String message) {
        super(message);
    }
}