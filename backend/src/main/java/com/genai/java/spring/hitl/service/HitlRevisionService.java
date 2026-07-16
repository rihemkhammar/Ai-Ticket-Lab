package com.genai.java.spring.hitl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.AgentOutputValidator;
import com.genai.java.spring.agent.AgentValidationException;
import com.genai.java.spring.agent.dto.TicketAgentSynthesisResult;
import com.genai.java.spring.hitl.dto.HitlDraft;
import com.genai.java.spring.hitl.prompt.HitlRevisionPromptBuilder;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a revised HITL draft from the previous draft + human comment,
 * with a single malformed-JSON retry (S5 §6.4 / §6.5).
 *
 * Flow:
 *  1. Ask GPT to revise using the normal prompt.
 *  2. If parsing/validation fails, retry ONCE with a stricter repair prompt.
 *  3. If it fails again, surface a RevisionFailedException so the caller
 *     (HumanReviewDecisionService) can mark the agent run FAILED while
 *     keeping the previous checkpoint and decision history intact.
 */
@Slf4j
@Service
public class HitlRevisionService {

    private static final int MAX_RETRIES = 1;

    private final ChatClient chatClient;
    private final HitlRevisionPromptBuilder promptBuilder;
    private final AgentOutputValidator validator;
    private final ObjectMapper objectMapper;

    public HitlRevisionService(@Qualifier("openAIChatClient") ChatClient chatClient,
                                HitlRevisionPromptBuilder promptBuilder,
                                AgentOutputValidator validator,
                                ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    /**
     * @param ticketId          ticket the revision belongs to
     * @param humanComment      required human comment that triggered the revision
     * @param previousDraftJson the previous checkpoint's draft_json
     */
    public HitlDraft generateRevisedDraft(Long ticketId, String humanComment, String previousDraftJson) {
        String systemPrompt = promptBuilder.systemPrompt();
        List<EvidenceChunkResponse> knownEvidence = knownEvidenceFrom(previousDraftJson);

        int attempt = 0;
        Exception lastError = null;

        while (attempt <= MAX_RETRIES) {
            String taskPrompt = attempt == 0
                    ? promptBuilder.taskPrompt(ticketId, humanComment, previousDraftJson)
                    : promptBuilder.repairTaskPrompt(ticketId, humanComment, previousDraftJson);

            try {
                HitlDraft draft = chatClient.prompt()
                        .system(systemPrompt)
                        .user(taskPrompt)
                        .call()
                        .entity(HitlDraft.class);

                // Reuses AgentOutputValidator's structural/forbidden-claim/needsHumanReview
                // checks. Revision does not re-retrieve evidence, so the revised draft may
                // only keep referencing the evidence already cited by the previous draft.
                validator.validate(toSynthesis(draft), knownEvidence);

                log.info("HITL revised draft parsed for ticketId={} attempt={} -> {}", ticketId, attempt, draft);
                return draft;

            } catch (AgentValidationException e) {
                lastError = e;
                log.warn("HITL revised draft failed validation ticketId={} attempt={} reason={}",
                        ticketId, attempt, e.getMessage());
            } catch (Exception e) {
                lastError = e;
                log.warn("HITL revised draft malformed/unparseable ticketId={} attempt={}", ticketId, attempt, e);
            }
            attempt++;
        }

        throw new RevisionFailedException(
                "AI revision could not produce valid output after " + (MAX_RETRIES + 1) + " attempt(s).",
                lastError);
    }

    /**
     * Revision reuses whatever evidence the previous draft already cited
     * (no new retrieval happens on revision). Building stub
     * EvidenceChunkResponse entries keyed on the previous sourceRef lets
     * AgentOutputValidator's evidence cross-check accept a revised draft
     * that keeps citing the same sources, while still rejecting any
     * invented sourceRef that wasn't part of the original investigation.
     */
    private List<EvidenceChunkResponse> knownEvidenceFrom(String previousDraftJson) {
        try {
            HitlDraft previous = objectMapper.readValue(previousDraftJson, HitlDraft.class);
            List<EvidenceRef> refs = previous.getEvidenceRefs();
            if (refs == null || refs.isEmpty()) {
                return List.of();
            }
            return refs.stream().map(ref -> {
                EvidenceChunkResponse stub = new EvidenceChunkResponse();
                stub.setSourceRef(ref.getSourceRef());
                stub.setArticleTitle(ref.getArticleTitle());
                return stub;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse previous draft evidenceRefs for revision", e);
            return List.of();
        }
    }

    private TicketAgentSynthesisResult toSynthesis(HitlDraft draft) {
        TicketAgentSynthesisResult synthesis = new TicketAgentSynthesisResult();
        synthesis.setInvestigationSummary(draft.getInvestigationSummary());
        synthesis.setEvidenceRefs(draft.getEvidenceRefs());
        synthesis.setPreviousReviewSummary(draft.getPreviousReviewSummary());
        synthesis.setRecommendedNextSteps(draft.getRecommendedNextSteps());
        synthesis.setDraftTechnicianResponse(draft.getDraftTechnicianResponse());
        synthesis.setConfidence(draft.getConfidence());
        synthesis.setLimitations(draft.getLimitations());
        synthesis.setNeedsHumanReview(draft.getNeedsHumanReview());
        return synthesis;
    }

    /** Raised when revision generation fails even after the single retry (S5 §6.4). */
    public static class RevisionFailedException extends RuntimeException {
        public RevisionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
