package com.genai.java.spring.rag.review;

import com.genai.java.spring.aireview.AiReviewParsingException;
import com.genai.java.spring.aireview.AiReviewProviderException;
import com.genai.java.spring.aireview.dto.ErrorResponse;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketRagReviewController {

    private final TicketRagReviewService service;

    public TicketRagReviewController(TicketRagReviewService service) {
        this.service = service;
    }

    @PostMapping("/{ticketId}/ai-review/rag")
    public ResponseEntity<RagReviewApiResponse> runRagReview(@PathVariable Long ticketId,
                                                             Authentication authentication) {
        RagReviewApiResponse response = service.runRagReview(ticketId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(AiReviewProviderException.class)
    public ResponseEntity<ErrorResponse> handleProviderFailure(AiReviewProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI provider failed. Please try again."));
    }

    @ExceptionHandler(AiReviewParsingException.class)
    public ResponseEntity<ErrorResponse> handleParsingFailure(AiReviewParsingException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("AI returned invalid output."));
    }

    @ExceptionHandler(RagReviewValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(RagReviewValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage()));
    }
}