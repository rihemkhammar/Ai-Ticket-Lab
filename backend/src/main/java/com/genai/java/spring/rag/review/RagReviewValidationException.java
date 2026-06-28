package com.genai.java.spring.rag.review;

public class RagReviewValidationException extends RuntimeException {

    public RagReviewValidationException(String message) {
        super(message);
    }
}