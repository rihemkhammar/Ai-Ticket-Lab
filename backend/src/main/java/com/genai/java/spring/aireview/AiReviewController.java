package com.genai.java.spring.aireview;

import com.genai.java.spring.aireview.dto.AiReviewApiResponse;
import com.genai.java.spring.aireview.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class AiReviewController {

    private final AiReviewService service;

    public AiReviewController(AiReviewService service) {
        this.service = service;
    }

    @PostMapping("/{ticketId}/ai-review/basic")
    public ResponseEntity<AiReviewApiResponse> runReview(@PathVariable Long ticketId,
                                                         Authentication authentication) {
        AiReviewApiResponse response = service.runReview(ticketId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // S4-BUG-02: fetch the last stored review without re-running the LLM,
    // so the dashboard can redisplay it after navigating away and back.
    @GetMapping("/{ticketId}/ai-review/basic")
    public ResponseEntity<AiReviewApiResponse> getLatestReview(@PathVariable Long ticketId) {
        return service.getLatestReview(ticketId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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
}