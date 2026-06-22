package com.genai.java.spring.aireview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aireview.dto.AiReviewApiResponse;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test unitaire d'AiReviewService.
 * Le ChatClient Spring AI est mocké via RETURNS_DEEP_STUBS pour simuler la
 * chaîne fluide .prompt().system().user().call().entity(...) sans jamais
 * appeler le vrai OpenAI .
 */
class AiReviewServiceTest {

    private ChatClient chatClient;
    private TicketService ticketService;
    private UserRepository userRepository;
    private AiReviewRepository repository;
    private AiReviewService service;

    private Ticket ticket;
    private User requester;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ticketService = mock(TicketService.class);
        userRepository = mock(UserRepository.class);
        repository = mock(AiReviewRepository.class);

        ObjectMapper objectMapper = new ObjectMapper();


        ticket = new Ticket();
        ticket.setTitle("Conveyor motor overheating");
        ticket.setDescription("The motor temperature increases after 20 minutes of operation.");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ReflectionTestUtils.setField(ticket, "id", 1L);

        requester = new User();
        requester.setId(UUID.randomUUID());
        requester.setUsername("demo_technician");

        when(ticketService.findById(1L)).thenReturn(ticket);
        when(userRepository.findByUsername("demo_technician")).thenReturn(Optional.of(requester));

        // simule repository.save() : renvoie l'entité reçue avec un id généré
        when(repository.save(any(AiReview.class))).thenAnswer(invocation -> {
            AiReview review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 42L);
            return review;
        });
    }

    @Test
    void storesSuccess_whenAiOutputIsValid() {
        TicketAiReviewResponse validResponse = new TicketAiReviewResponse();
        validResponse.setSummary("The conveyor motor overheats after running for 20 minutes.");
        validResponse.setPossibleCauses(List.of("Insufficient cooling"));
        validResponse.setRecommendedChecks(List.of("Check ventilation around the motor"));
        validResponse.setDraftResponse("Please inspect the motor cooling.");
        validResponse.setConfidence(Confidence.MEDIUM);

        mockChatClientReturns(validResponse);

        AiReviewApiResponse response = service.runReview(1L, "demo_technician");

        assertThat(response.getStatus()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(response.getResult().getConfidence()).isEqualTo(Confidence.MEDIUM);
        verify(repository).save(argThat(r -> r.getStatus() == AiReviewStatus.SUCCESS));
    }

    @Test
    void storesFailed_whenAiOutputIsInvalid() {
        TicketAiReviewResponse invalidResponse = new TicketAiReviewResponse();
        invalidResponse.setSummary(""); // summary vide -> invalide selon AiReviewValidator
        invalidResponse.setPossibleCauses(List.of("cause"));
        invalidResponse.setRecommendedChecks(List.of("check"));
        invalidResponse.setDraftResponse("draft");
        invalidResponse.setConfidence(Confidence.LOW);

        mockChatClientReturns(invalidResponse);

        assertThrows(AiReviewParsingException.class, () -> service.runReview(1L, "demo_technician"));

        verify(repository).save(argThat(r -> r.getStatus() == AiReviewStatus.FAILED));
    }

    @Test
    void storesFailed_whenAiProviderFails() {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(TicketAiReviewResponse.class))
                .thenThrow(new RuntimeException("OpenAI timeout"));

        assertThrows(AiReviewProviderException.class, () -> service.runReview(1L, "demo_technician"));

        verify(repository).save(argThat(r -> r.getStatus() == AiReviewStatus.FAILED));
    }

    private void mockChatClientReturns(TicketAiReviewResponse parsed) {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(TicketAiReviewResponse.class))
                .thenReturn(parsed);
    }
}