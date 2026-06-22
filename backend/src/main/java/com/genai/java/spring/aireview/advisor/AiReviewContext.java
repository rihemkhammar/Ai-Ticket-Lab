package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.ticket.Ticket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Objet mutable qui circule à travers la chaîne d'advisors.
 * Sert d'état partagé entre la phase PRE_CALL et POST_CALL.
 */
@Getter
@Setter
@ToString
public class AiReviewContext {

    private final Ticket ticket;
    private final UUID requesterId;
    private final String requesterUsername;

    private String promptVersion;
    private String systemPrompt;
    private String userPrompt;

    private boolean injectionSuspected;
    private final List<String> injectionFlags = new ArrayList<>();

    private TicketAiReviewResponse aiResponse;

    private boolean valid = true;
    private final List<String> validationErrors = new ArrayList<>();

    public AiReviewContext(Ticket ticket, UUID requesterId, String requesterUsername) {
        this.ticket = ticket;
        this.requesterId = requesterId;
        this.requesterUsername = requesterUsername;
    }

    public void invalidate(String reason) {
        this.valid = false;
        this.validationErrors.add(reason);
    }

    public String firstError() {
        return validationErrors.isEmpty() ? "AI returned invalid output." : validationErrors.get(0);
    }
}