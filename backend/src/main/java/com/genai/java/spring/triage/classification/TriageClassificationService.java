package com.genai.java.spring.triage.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.graph.TriageClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * TriageClassificationService .
 *
 * Loads a ticket, asks the model to classify it by criticality, and
 * validates the structured output. If classification fails for any
 * reason (LLM error, invalid/missing criticality, blank rationale),
 * the ticket is NOT dropped from the batch: it defaults to MEDIUM
 * and a fallback note is recorded (Phase 2 done criteria).
 */
@Slf4j
@Service
public class TriageClassificationService {

    private final ChatClient chatClient;
    private final TicketService ticketService;
    private final TriageClassificationPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public TriageClassificationService(@Qualifier("openAIChatClient") ChatClient chatClient,
                                       TicketService ticketService,
                                       TriageClassificationPromptBuilder promptBuilder,
                                       ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ticketService = ticketService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Classifies one ticket by criticality. Never throws for LLM/parsing
     * failures — falls back to MEDIUM instead, per Phase 2 rule.
     * Only propagates if the ticket itself does not exist
     * (TicketNotFoundException), since that is a real input error,
     * not a classification failure.
     */
    public TriageClassification classify(Long ticketId) {
        Ticket ticket = ticketService.findById(ticketId);

        try {
            String rawJson = chatClient.prompt()
                    .system(promptBuilder.buildSystemPrompt())
                    .user(promptBuilder.buildUserPrompt(ticket))
                    .call()
                    .content();

            RawClassification raw = objectMapper.readValue(rawJson, RawClassification.class);
            TicketCriticality criticality = parseCriticality(raw.criticality);

            if (criticality == null || raw.rationale == null || raw.rationale.isBlank()) {
                return fallback(ticketId, "Model returned invalid or incomplete classification.");
            }

            return new TriageClassification(ticketId, criticality, raw.rationale);

        } catch (Exception e) {
            log.warn("Triage classification failed for ticket {}: {}", ticketId, e.getMessage());
            return fallback(ticketId, "Automatic classification failed: " + e.getMessage());
        }
    }

    private TriageClassification fallback(Long ticketId, String note) {
        TriageClassification classification =
                new TriageClassification(ticketId, TicketCriticality.MEDIUM, note);
        classification.setFallbackApplied(true);
        return classification;
    }

    private TicketCriticality parseCriticality(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TicketCriticality.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Shape of the raw JSON returned by the model, before validation.
     * Kept private: callers only ever see the validated TriageClassification.
     */
    private static class RawClassification {
        public Long ticketId;
        public String criticality;
        public String rationale;
    }
}