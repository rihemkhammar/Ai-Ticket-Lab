package com.genai.java.spring.rag.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aireview.AiReview;
import com.genai.java.spring.aireview.AiReviewProviderException;
import com.genai.java.spring.aireview.AiReviewRepository;
import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.rag.retrieval.TicketEvidenceRetriever;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import com.genai.java.spring.rag.review.dto.TicketRagReviewResponse;
import com.genai.java.spring.rag.review.prompt.TicketRagReviewPromptBuilder;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Phase 4 — evidence-grounded AI review.
 *
 * Flow: load ticket -> retrieve evidence chunks -> build RAG prompt
 * -> call GPT -> parse JSON -> validate (structure + evidence refs)
 * -> store SUCCESS/FAILED in ai_reviews -> return response.
 */
@Slf4j
@Service
public class TicketRagReviewService {

    private static final String MODEL_NAME = "openai/gpt-oss-20b";

    private final ChatClient chatClient;
    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final TicketEvidenceRetriever evidenceRetriever;
    private final TicketRagReviewPromptBuilder promptBuilder;
    private final RagReviewValidator validator;
    private final AiReviewRepository repository;
    private final ObjectMapper objectMapper;

    public TicketRagReviewService(@Qualifier("openAIChatClient") ChatClient chatClient,
                                  TicketService ticketService,
                                  UserRepository userRepository,
                                  TicketEvidenceRetriever evidenceRetriever,
                                  TicketRagReviewPromptBuilder promptBuilder,
                                  RagReviewValidator validator,
                                  AiReviewRepository repository,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.evidenceRetriever = evidenceRetriever;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public RagReviewApiResponse runRagReview(Long ticketId, String requesterUsername) {
        Ticket ticket = ticketService.findById(ticketId);

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        // Phase 3 — retrieve evidence before calling GPT.
        List<EvidenceChunkResponse> evidence = evidenceRetriever.retrieve(ticket);

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(ticket.getTitle(), ticket.getDescription(), evidence);
        String promptVersion = promptBuilder.version();

        TicketRagReviewResponse parsed;
        try {
            parsed = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(TicketRagReviewResponse.class);
            log.info("RAG AI raw response parsed for ticketId={} -> {}", ticketId, parsed);
        } catch (Exception e) {
            log.error("RAG AI provider call failed for ticketId={}", ticketId, e);
            saveFailedReview(ticket.getId(), requester.getId(), promptVersion,
                    "AI provider failed or returned invalid output.");
            throw new AiReviewProviderException("RAG AI call failed for ticket " + ticketId, e);
        }

        try {
            validator.validate(parsed, evidence);
        } catch (RagReviewValidationException e) {
            log.warn("RAG AI review rejected for ticketId={} reason={}", ticketId, e.getMessage());
            saveFailedReview(ticket.getId(), requester.getId(), promptVersion, e.getMessage());
            throw e;
        }

        AiReview saved = saveSuccessReview(ticket.getId(), requester.getId(), promptVersion, parsed);

        return toApiResponse(saved, parsed, evidence);
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

    private AiReview saveSuccessReview(Long ticketId, UUID triggeredBy, String promptVersion,
                                       TicketRagReviewResponse parsed) {
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
            log.error("Failed to serialize RAG AI response for ticketId={}", ticketId, e);
            review.setResultJson(null);
        }
        return repository.save(review);
    }

    private RagReviewApiResponse toApiResponse(AiReview saved, TicketRagReviewResponse parsed,
                                               List<EvidenceChunkResponse> evidence) {
        RagReviewApiResponse response = new RagReviewApiResponse();
        response.setReviewId(saved.getId());
        response.setTicketId(saved.getTicketId());
        response.setPromptVersion(saved.getPromptVersion());
        response.setModelName(saved.getModelName());
        response.setStatus(saved.getStatus());
        response.setResult(parsed);
        response.setErrorMessage(saved.getErrorMessage());
        response.setCreatedAt(saved.getCreatedAt());
        response.setRetrievedEvidence(evidence);
        return response;
    }
}