package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.AgentOutputValidator;
import com.genai.java.spring.agent.AgentValidationException;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.hitl.dto.HitlDraft;
import com.genai.java.spring.hitl.prompt.HitlRevisionPromptBuilder;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HitlRevisionService: generates a revised draft from the human
 * comment + previous draft, with a single malformed-JSON retry.
 */
@ExtendWith(MockitoExtension.class)
class HitlRevisionServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock private AgentOutputValidator validator;

    private HitlRevisionService service;

    private static final Long TICKET_ID = 1L;
    private static final String HUMAN_COMMENT = "Please double-check the electrical readings first.";

    @BeforeEach
    void setUp() {
        service = new HitlRevisionService(chatClient, new HitlRevisionPromptBuilder(), validator, new ObjectMapper());
    }

    private String previousDraftJson() {
        return """
                {"investigationSummary":"Motor overheating.","evidenceRefs":[{"sourceRef":"KB-001","articleTitle":"Motor Guide"}],
                 "recommendedNextSteps":["inspect equipment"],"draftTechnicianResponse":"Inspect vent.",
                 "confidence":"MEDIUM","limitations":[],"needsHumanReview":true}
                """;
    }

    private HitlDraft revisedDraft() {
        HitlDraft d = new HitlDraft();
        d.setInvestigationSummary("Motor overheating; electrical readings checked per reviewer request.");
        d.setEvidenceRefs(List.of(evidenceRef()));
        d.setRecommendedNextSteps(List.of("inspect equipment", "check electrical draw"));
        d.setDraftTechnicianResponse("Inspect vent and verify current draw.");
        d.setConfidence(Confidence.MEDIUM);
        d.setLimitations(List.of());
        d.setNeedsHumanReview(true);
        return d;
    }

    private EvidenceRef evidenceRef() {
        EvidenceRef ref = new EvidenceRef();
        ref.setSourceRef("KB-001");
        ref.setArticleTitle("Motor Guide");
        return ref;
    }

    private void stubChatClientReturns(HitlDraft draft) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(HitlDraft.class)).thenReturn(draft);
        // The when(...) chain above already invoked chatClient.prompt() once
        // (required to obtain the deep-stub chain) - that invocation would
        // otherwise be double-counted by a later verify(chatClient, times(n)).prompt().
        clearInvocations(chatClient);
    }

    private void stubChatClientThrows(RuntimeException ex) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(HitlDraft.class)).thenThrow(ex);
        clearInvocations(chatClient);
    }

    private void stubChatClientThenThrowThenReturn(RuntimeException first, HitlDraft second) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(HitlDraft.class))
                .thenThrow(first)
                .thenReturn(second);
        // With RETURNS_DEEP_STUBS, prompt() and system(...) are real invocations
        // recorded on TWO different mocks: chatClient itself, and the nested
        // ChatClientRequestSpec mock returned by .system(...) (deep stubs cache
        // and reuse that same nested mock instance across calls). Clearing only
        // chatClient leaves the setup-time .user(...) call counted on the nested
        // mock, which later inflates verify(chatClient.prompt().system(...), times(n)).user(...).
        // Clear both levels so only real service invocations are counted.
        Object requestSpec = chatClient.prompt().system(anyString());
        clearInvocations(chatClient, requestSpec);
    }

    @Test
    @DisplayName("generates a valid revised draft on the first attempt")
    void generateRevisedDraft_success_firstAttempt() {
        stubChatClientReturns(revisedDraft());
        doNothing().when(validator).validate(any(), any());

        HitlDraft result = service.generateRevisedDraft(TICKET_ID, HUMAN_COMMENT, previousDraftJson());

        assertThat(result.getNeedsHumanReview()).isTrue();
        assertThat(result.getDraftTechnicianResponse()).contains("current draw");
        verify(chatClient, times(1)).prompt();
    }

    @Test
    @DisplayName("malformed revised JSON retries once, and succeeds on the retry")
    void generateRevisedDraft_malformedThenValid_retriesOnceAndSucceeds() {
        stubChatClientThenThrowThenReturn(new RuntimeException("Malformed JSON from model"), revisedDraft());
        doNothing().when(validator).validate(any(), any());

        HitlDraft result = service.generateRevisedDraft(TICKET_ID, HUMAN_COMMENT, previousDraftJson());

        assertThat(result).isNotNull();
        verify(chatClient, times(2)).prompt();
    }

    @Test
    @DisplayName("malformed revised JSON after the retry throws RevisionFailedException")
    void generateRevisedDraft_malformedTwice_throwsAfterRetry() {
        stubChatClientThrows(new RuntimeException("Malformed JSON from model"));

        assertThatThrownBy(() -> service.generateRevisedDraft(TICKET_ID, HUMAN_COMMENT, previousDraftJson()))
                .isInstanceOf(HitlRevisionService.RevisionFailedException.class)
                .hasMessageContaining("2 attempt");

        // exactly 2 attempts total: 1 normal + 1 retry with the repair prompt
        verify(chatClient, times(2)).prompt();
    }

    @Test
    @DisplayName("a revised draft failing validation on both attempts throws RevisionFailedException")
    void generateRevisedDraft_failsValidationTwice_throwsAfterRetry() {
        stubChatClientReturns(revisedDraft());
        doThrow(new AgentValidationException("Agent output contains a forbidden claim: \"resolved\"."))
                .when(validator).validate(any(), any());

        assertThatThrownBy(() -> service.generateRevisedDraft(TICKET_ID, HUMAN_COMMENT, previousDraftJson()))
                .isInstanceOf(HitlRevisionService.RevisionFailedException.class);

        verify(chatClient, times(2)).prompt();
    }

    @Test
    @DisplayName("the retry uses a stricter repair prompt (second call differs from the first)")
    void generateRevisedDraft_retryUsesRepairPrompt() {
        stubChatClientThenThrowThenReturn(new RuntimeException("Malformed JSON from model"), revisedDraft());
        doNothing().when(validator).validate(any(), any());

        service.generateRevisedDraft(TICKET_ID, HUMAN_COMMENT, previousDraftJson());

        org.mockito.ArgumentCaptor<String> userPromptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(chatClient.prompt().system(anyString()), times(2)).user(userPromptCaptor.capture());

        List<String> prompts = userPromptCaptor.getAllValues();
        assertThat(prompts.get(0)).doesNotContain("IMPORTANT");
        assertThat(prompts.get(1)).contains("IMPORTANT");
    }
}