package com.genai.java.spring.exception;

import com.genai.java.spring.aireview.dto.ErrorResponse;
import com.genai.java.spring.hitl.HitlValidationException;
import com.genai.java.spring.hitl.dto.HitlErrorResponse;
import com.genai.java.spring.knowledge.KnowledgeArticleNotFoundException;
import com.genai.java.spring.ticket.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Ticket not found."));
    }

    //  knowledge article lookup (GET /api/articles/{id})
    @ExceptionHandler(KnowledgeArticleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleArticleNotFound(KnowledgeArticleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Knowledge article not found."));
    }

    // HITL human-review decision validation failures (POST /api/agent-runs/{runId}/human-review/decision)
    @ExceptionHandler(HitlValidationException.class)
    public ResponseEntity<HitlErrorResponse> handleHitlValidation(HitlValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HitlErrorResponse(ex.getMessage()));
    }
}