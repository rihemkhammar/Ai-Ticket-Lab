package com.genai.java.spring.triage.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketNotFoundException;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.triage.TicketCriticality;
import com.genai.java.spring.triage.graph.TriageClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageClassificationServiceTest {

    @Mock private TicketService ticketService;

    private final TriageClassificationPromptBuilder promptBuilder = new TriageClassificationPromptBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long TICKET_ID = 1L;

    private Ticket sampleTicket() {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(TICKET_ID);
        when(ticket.getTitle()).thenReturn("Conveyor motor overheating");
        when(ticket.getDescription()).thenReturn("Motor temperature increases after 20 minutes.");
        return ticket;
    }

    /**
     * Builds a TriageClassificationService whose ChatClient fluent chain
     * (.prompt().system().user().call().content()) returns the given raw
     * JSON response, mirroring the stubbing pattern used for AiReviewService.
     */
    private TriageClassificationService serviceReturning(String rawJsonResponse) {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .call()
                .content())
                .thenReturn(rawJsonResponse);
        return new TriageClassificationService(chatClient, ticketService, promptBuilder, objectMapper);
    }

    private TriageClassificationService serviceThrowing(RuntimeException toThrow) {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .call()
                .content())
                .thenThrow(toThrow);
        return new TriageClassificationService(chatClient, ticketService, promptBuilder, objectMapper);
    }

    @Test
    @DisplayName("classify returns a valid classification for a well-formed model response")
    void classify_validResponse_returnsClassification() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning(
                "{\"ticketId\":1,\"criticality\":\"HIGH\",\"rationale\":\"Overheating risks equipment damage.\"}");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getTicketId()).isEqualTo(TICKET_ID);
        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.HIGH);
        assertThat(result.getRationale()).isEqualTo("Overheating risks equipment damage.");
        assertThat(result.isFallbackApplied()).isFalse();
    }

    @Test
    @DisplayName("classify is case-insensitive for the criticality value")
    void classify_lowercaseCriticality_isNormalized() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning(
                "{\"ticketId\":1,\"criticality\":\"critical\",\"rationale\":\"Immediate safety hazard.\"}");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.CRITICAL);
        assertThat(result.isFallbackApplied()).isFalse();
    }

    @Test
    @DisplayName("classify falls back to MEDIUM when criticality is invalid")
    void classify_invalidCriticality_fallsBackToMedium() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning(
                "{\"ticketId\":1,\"criticality\":\"URGENT\",\"rationale\":\"Some rationale.\"}");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.MEDIUM);
        assertThat(result.isFallbackApplied()).isTrue();
    }

    @Test
    @DisplayName("classify falls back to MEDIUM when criticality is missing")
    void classify_missingCriticality_fallsBackToMedium() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning(
                "{\"ticketId\":1,\"rationale\":\"Some rationale.\"}");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.MEDIUM);
        assertThat(result.isFallbackApplied()).isTrue();
    }

    @Test
    @DisplayName("classify falls back to MEDIUM when rationale is blank")
    void classify_blankRationale_fallsBackToMedium() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning(
                "{\"ticketId\":1,\"criticality\":\"LOW\",\"rationale\":\"   \"}");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.MEDIUM);
        assertThat(result.isFallbackApplied()).isTrue();
    }

    @Test
    @DisplayName("classify falls back to MEDIUM when the model returns malformed JSON")
    void classify_malformedJson_fallsBackToMedium() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning("not valid json");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.MEDIUM);
        assertThat(result.isFallbackApplied()).isTrue();
        assertThat(result.getRationale()).contains("Automatic classification failed");
    }

    @Test
    @DisplayName("classify falls back to MEDIUM when the LLM call throws")
    void classify_llmFailure_fallsBackToMedium() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceThrowing(new RuntimeException("upstream timeout"));

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getCriticality()).isEqualTo(TicketCriticality.MEDIUM);
        assertThat(result.isFallbackApplied()).isTrue();
        assertThat(result.getRationale()).contains("upstream timeout");
    }

    @Test
    @DisplayName("classify never drops the ticket: fallback keeps the same ticketId")
    void classify_fallback_preservesTicketId() {
        Ticket ticket = sampleTicket();
        when(ticketService.findById(TICKET_ID)).thenReturn(ticket);
        TriageClassificationService service = serviceReturning("not valid json");

        TriageClassification result = service.classify(TICKET_ID);

        assertThat(result.getTicketId()).isEqualTo(TICKET_ID);
    }

    @Test
    @DisplayName("classify propagates TicketNotFoundException when the ticket does not exist")
    void classify_missingTicket_propagatesException() {
        when(ticketService.findById(TICKET_ID)).thenThrow(new TicketNotFoundException(TICKET_ID));
        ChatClient chatClient = mock(ChatClient.class);
        TriageClassificationService service =
                new TriageClassificationService(chatClient, ticketService, promptBuilder, objectMapper);

        assertThatThrownBy(() -> service.classify(TICKET_ID))
                .isInstanceOf(TicketNotFoundException.class);
    }
}