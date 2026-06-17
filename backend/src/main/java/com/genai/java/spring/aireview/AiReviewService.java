package com.genai.java.spring.aireview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aireview.dto.AiReviewApiResponse;
import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiReviewService {

    private static final String PROMPT_VERSION = "ticket-basic-review-v1";
    private static final String MODEL_NAME = "gpt-4o-mini";

    private static final String SYSTEM_PROMPT = """
            You are an AI maintenance assistant.
            You help technicians understand maintenance tickets.
            You may summarize the issue, suggest possible causes, recommend checks, and draft a response.
            Do not claim that the ticket is officially resolved.
            Return JSON only.
            """;

    private final ChatClient chatClient;
    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final AiReviewValidator validator;
    private final AiReviewRepository repository;
    private final ObjectMapper objectMapper;

    public AiReviewService(@Qualifier("openAIChatClient") ChatClient chatClient,
                           TicketService ticketService,
                           UserRepository userRepository,
                           AiReviewValidator validator,
                           AiReviewRepository repository,
                           ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.validator = validator;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public AiReviewApiResponse runReview(Long ticketId, String requesterUsername) {
        // 1. Charger le ticket (404 si absent, géré par TicketService -> TicketNotFoundException)
        Ticket ticket = ticketService.findById(ticketId);

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        // 2. Construire le prompt
        String userPrompt = buildUserPrompt(ticket.getTitle(), ticket.getDescription());

        // 3. Appeler GPT (hors transaction DB)
        String rawContent;
        try {
            rawContent = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            System.out.println(">>> RAW AI RESPONSE: " + rawContent);
        } catch (Exception e) {
            AiReview failed = saveFailedReview(ticket.getId(), requester.getId(),
                    "AI provider failed. Please try again.");
            throw new AiReviewProviderException("AI provider failed for ticket " + ticketId, e);
        }

        // 4. Parser le JSON manuellement (Jackson)
        TicketAiReviewResponse parsed;
        try {
            String cleaned = stripCodeFences(rawContent);
            parsed = objectMapper.readValue(cleaned, TicketAiReviewResponse.class);
        } catch (Exception e) {
            saveFailedReview(ticket.getId(), requester.getId(), "AI returned invalid output.");
            throw new AiReviewParsingException("Could not parse AI output for ticket " + ticketId, e);
        }

        // 5. Valider
        AiReviewValidator.ValidationResult validation = validator.validate(parsed);
        if (!validation.isValid()) {
            saveFailedReview(ticket.getId(), requester.getId(), "AI returned invalid output.");
            throw new AiReviewParsingException(validation.getErrorMessage());
        }

        // 6. Stocker SUCCESS (transaction courte)
        AiReview saved = saveSuccessReview(ticket.getId(), requester.getId(), parsed);

        return toApiResponse(saved, parsed);
    }

    private AiReview saveFailedReview(Long ticketId, java.util.UUID triggeredBy, String errorMessage) {
        AiReview review = new AiReview();
        review.setTicketId(ticketId);
        review.setTriggeredBy(triggeredBy);
        review.setPromptVersion(PROMPT_VERSION);
        review.setModelName(MODEL_NAME);
        review.setStatus(AiReviewStatus.FAILED);
        review.setErrorMessage(errorMessage);
        review.setCreatedAt(LocalDateTime.now());
        return repository.save(review);
    }

    private AiReview saveSuccessReview(Long ticketId, java.util.UUID triggeredBy, TicketAiReviewResponse parsed) {
        AiReview review = new AiReview();
        review.setTicketId(ticketId);
        review.setTriggeredBy(triggeredBy);
        review.setPromptVersion(PROMPT_VERSION);
        review.setModelName(MODEL_NAME);
        review.setStatus(AiReviewStatus.SUCCESS);
        review.setCreatedAt(LocalDateTime.now());
        try {
            review.setResultJson(objectMapper.writeValueAsString(parsed));
        } catch (Exception e) {
            // Ne devrait pas arriver puisqu'on vient de parser cet objet
            review.setResultJson(null);
        }
        return repository.save(review);
    }

    private AiReviewApiResponse toApiResponse(AiReview saved, TicketAiReviewResponse parsed) {
        AiReviewApiResponse response = new AiReviewApiResponse();
        response.setReviewId(saved.getId());
        response.setTicketId(saved.getTicketId());
        response.setPromptVersion(saved.getPromptVersion());
        response.setModelName(saved.getModelName());
        response.setStatus(saved.getStatus());
        response.setResult(parsed);
        response.setErrorMessage(saved.getErrorMessage());
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    private String buildUserPrompt(String title, String description) {
        return """
                Review the following maintenance ticket.
                Ticket title:
                %s
                Ticket description:
                %s
                Return JSON with:
                summary
                possibleCauses
                recommendedChecks
                draftResponse
                confidence
                """.formatted(title, description);
    }

    private String stripCodeFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}