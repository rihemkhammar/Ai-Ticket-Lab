package com.genai.java.spring.aireview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aireview.advisor.AiReviewAdvisorChain;
import com.genai.java.spring.aireview.advisor.AiReviewContext;
import com.genai.java.spring.aireview.dto.AiReviewApiResponse;
import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AiReviewService {

    //private static final String MODEL_NAME = "gpt-4o-mini";
    private static final String MODEL_NAME = "openai/gpt-oss-20b";


    private final ChatClient chatClient;
    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final AiReviewAdvisorChain advisorChain;
    private final AiReviewRepository repository;
    private final ObjectMapper objectMapper;

    public AiReviewService(@Qualifier("openAIChatClient") ChatClient chatClient,
                           TicketService ticketService,
                           UserRepository userRepository,
                           AiReviewAdvisorChain advisorChain,
                           AiReviewRepository repository,
                           ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.advisorChain = advisorChain;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public AiReviewApiResponse runReview(Long ticketId, String requesterUsername) {
        Ticket ticket = ticketService.findById(ticketId);

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        AiReviewContext context = new AiReviewContext(ticket, requester.getId(), requesterUsername);

        // Phase 1 (prompt) + Phase 2 (défense injection)
        advisorChain.runPreCall(context);

        TicketAiReviewResponse parsed;
        try {
            parsed = chatClient.prompt()
                    .system(context.getSystemPrompt())
                    .user(context.getUserPrompt())
                    .call()
                    .entity(TicketAiReviewResponse.class);
            log.info("AI raw response parsed for ticketId={} -> {}", ticketId, parsed);
        } catch (Exception e) {
            log.error("AI provider call failed for ticketId={}", ticketId, e);
            saveFailedReview(ticket.getId(), requester.getId(), context.getPromptVersion(),
                    "AI provider failed or returned invalid output.");
            throw new AiReviewProviderException("AI call failed for ticket " + ticketId, e);
        }

        context.setAiResponse(parsed);

        // Phase 1 (structure) + Phase 4 (limitations / needsHumanReview)
        advisorChain.runPostCall(context);

        if (!context.isValid()) {
            log.warn("AI review rejected for ticketId={} reasons={}", ticketId, context.getValidationErrors());
            saveFailedReview(ticket.getId(), requester.getId(), context.getPromptVersion(), context.firstError());
            throw new AiReviewParsingException(context.firstError());
        }

        AiReview saved = saveSuccessReview(ticket.getId(), requester.getId(), context.getPromptVersion(), parsed);

        return toApiResponse(saved, parsed);
    }

    private AiReview saveFailedReview(Long ticketId, UUID triggeredBy, String promptVersion, String errorMessage) {
        AiReview review = new AiReview();
        review.setTicketId(ticketId);
        review.setTriggeredBy(triggeredBy);
        review.setPromptVersion(promptVersion);
        review.setModelName(MODEL_NAME);
        review.setStatus(AiReviewStatus.FAILED);
        review.setErrorMessage(errorMessage);
        review.setCreatedAt(LocalDateTime.now());
        return repository.save(review);
    }

    private AiReview saveSuccessReview(Long ticketId, UUID triggeredBy, String promptVersion, TicketAiReviewResponse parsed) {
        AiReview review = new AiReview();
        review.setTicketId(ticketId);
        review.setTriggeredBy(triggeredBy);
        review.setPromptVersion(promptVersion);
        review.setModelName(MODEL_NAME);
        review.setStatus(AiReviewStatus.SUCCESS);
        review.setCreatedAt(LocalDateTime.now());
        try {
            review.setResultJson(objectMapper.writeValueAsString(parsed));
        } catch (Exception e) {
            log.error("Failed to serialize AI response for ticketId={}", ticketId, e);
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
}