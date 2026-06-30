package com.genai.java.spring.rag.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aireview.AiReview;
import com.genai.java.spring.aireview.AiReviewProviderException;
import com.genai.java.spring.aireview.AiReviewRepository;
import com.genai.java.spring.aireview.AiReviewStatus;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.retrieval.TicketEvidenceRetriever;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import com.genai.java.spring.rag.review.dto.RagReviewApiResponse;
import com.genai.java.spring.rag.review.dto.TicketRagReviewResponse;
import com.genai.java.spring.rag.review.prompt.TicketRagReviewPromptBuilder;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.user.User;
import com.genai.java.spring.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRagReviewServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock private TicketService ticketService;
    @Mock private UserRepository userRepository;
    @Mock private TicketEvidenceRetriever evidenceRetriever;
    @Mock private TicketRagReviewPromptBuilder promptBuilder;
    @Mock private RagReviewValidator validator;
    @Mock private AiReviewRepository repository;

    private TicketRagReviewService service;

    private static final Long TICKET_ID = 1L;
    private static final String USERNAME = "tech1";

    @BeforeEach
    void setUp() {
        service = new TicketRagReviewService(
                chatClient, ticketService, userRepository,
                evidenceRetriever, promptBuilder, validator,
                repository, new ObjectMapper()
        );

        // Ticket.setId() n'existe pas — on mock l'entité directement
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(ticket.getTitle()).thenReturn("Motor overheating");
        when(ticket.getDescription()).thenReturn("Fan not working");
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(USERNAME);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(anyString(), anyString(), anyList())).thenReturn("user");
        when(promptBuilder.version()).thenReturn("v1");

        // lenient() car providerFailure/validationFailure ne lisent pas le retour de save()
        AiReview savedReview = mock(AiReview.class);
        lenient().when(savedReview.getId()).thenReturn(10L);
        lenient().when(savedReview.getTicketId()).thenReturn(TICKET_ID);
        lenient().when(savedReview.getStatus()).thenReturn(AiReviewStatus.SUCCESS);
        lenient().when(repository.save(any())).thenReturn(savedReview);
    }

    private EvidenceChunkResponse evidence() {
        return EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9);
    }

    private TicketRagReviewResponse validParsed() {
        TicketRagReviewResponse r = new TicketRagReviewResponse();
        r.setSummary("Motor overheating summary.");
        r.setPossibleCauses(List.of("Blocked vent"));
        r.setRecommendedChecks(List.of("Check fan"));
        r.setDraftResponse("Schedule maintenance.");
        r.setConfidence(Confidence.MEDIUM);
        r.setLimitations(List.of("Top-3 chunks only."));
        r.setNeedsHumanReview(true);
        EvidenceRef ref = new EvidenceRef();
        ref.setSourceRef("article:1#chunk:0");
        ref.setArticleTitle("Motor Guide");
        r.setEvidenceRefs(List.of(ref));
        return r;
    }

    @Test
    @DisplayName("success flow: retourne SUCCESS avec evidence")
    void runRagReview_success() {
        when(evidenceRetriever.retrieve(any())).thenReturn(List.of(evidence()));
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketRagReviewResponse.class)).thenReturn(validParsed());
        doNothing().when(validator).validate(any(), any());

        RagReviewApiResponse response = service.runRagReview(TICKET_ID, USERNAME);

        assertThat(response.getStatus()).isEqualTo(AiReviewStatus.SUCCESS);
        assertThat(response.getRetrievedEvidence()).hasSize(1);
    }

    @Test
    @DisplayName("provider failure -> sauvegarde FAILED et lance AiReviewProviderException")
    void runRagReview_providerFailure() {
        when(evidenceRetriever.retrieve(any())).thenReturn(List.of(evidence()));
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketRagReviewResponse.class))
                .thenThrow(new RuntimeException("OpenRouter timeout"));

        assertThatThrownBy(() -> service.runRagReview(TICKET_ID, USERNAME))
                .isInstanceOf(AiReviewProviderException.class);

        // On vérifie l'objet PASSÉ à save(), pas ce qu'il retourne
        ArgumentCaptor<AiReview> captor = ArgumentCaptor.forClass(AiReview.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
    }

    @Test
    @DisplayName("validation failure -> sauvegarde FAILED et rethrow")
    void runRagReview_validationFailure() {
        when(evidenceRetriever.retrieve(any())).thenReturn(List.of(evidence()));
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketRagReviewResponse.class)).thenReturn(validParsed());
        doThrow(new RagReviewValidationException("AI invented a sourceRef"))
                .when(validator).validate(any(), any());

        assertThatThrownBy(() -> service.runRagReview(TICKET_ID, USERNAME))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("invented a sourceRef");

        // On vérifie l'objet PASSÉ à save(), pas ce qu'il retourne
        ArgumentCaptor<AiReview> captor = ArgumentCaptor.forClass(AiReview.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AiReviewStatus.FAILED);
    }

    @Test
    @DisplayName("no evidence -> appelle quand même GPT et validator avec liste vide")
    void runRagReview_noEvidence() {
        when(evidenceRetriever.retrieve(any())).thenReturn(List.of());

        TicketRagReviewResponse parsed = new TicketRagReviewResponse();
        parsed.setSummary("No knowledge base match.");
        parsed.setPossibleCauses(List.of("Unknown"));
        parsed.setRecommendedChecks(List.of("Manual check"));
        parsed.setDraftResponse("Escalate.");
        parsed.setConfidence(Confidence.LOW);
        parsed.setLimitations(List.of("No relevant evidence was found."));
        parsed.setNeedsHumanReview(true);
        parsed.setEvidenceRefs(List.of());

        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(TicketRagReviewResponse.class)).thenReturn(parsed);
        doNothing().when(validator).validate(any(), any());

        RagReviewApiResponse response = service.runRagReview(TICKET_ID, USERNAME);

        assertThat(response.getRetrievedEvidence()).isEmpty();
        verify(validator).validate(eq(parsed), eq(List.of()));
    }
}