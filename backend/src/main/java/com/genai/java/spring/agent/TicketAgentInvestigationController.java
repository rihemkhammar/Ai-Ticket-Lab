package com.genai.java.spring.agent;

import com.genai.java.spring.agent.dto.TicketAgentInvestigationRequest;
import com.genai.java.spring.agent.dto.TicketAgentInvestigationResponse;
import com.genai.java.spring.aireview.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketAgentInvestigationController {

    private final TicketAgentInvestigationService service;

    public TicketAgentInvestigationController(TicketAgentInvestigationService service) {
        this.service = service;
    }

    @PostMapping("/{ticketId}/agent/investigate")
    public ResponseEntity<TicketAgentInvestigationResponse> investigate(
            @PathVariable Long ticketId,
            @RequestBody(required = false) TicketAgentInvestigationRequest request) {

        TicketAgentInvestigationRequest safeRequest =
                request != null ? request : new TicketAgentInvestigationRequest();

        TicketAgentInvestigationResponse response = service.investigate(ticketId, safeRequest);

        if (response.getStatus() == AgentRunStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(AgentValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(AgentValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
