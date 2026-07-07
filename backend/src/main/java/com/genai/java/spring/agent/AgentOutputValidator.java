package com.genai.java.spring.agent;

import com.genai.java.spring.agent.dto.TicketAgentSynthesisResult;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the final agent synthesis output .
 * Enforces structural completeness, evidence-reference honesty, and the
 * forbidden-claim guardrail that keeps the agent strictly advisory.
 */
@Component
public class AgentOutputValidator {

    private static final List<String> FORBIDDEN_CLAIM_PHRASES = List.of(
            "ticket is closed",
            "ticket has been closed",
            "ticket has been resolved",
            "ticket is resolved",
            "repair completed",
            "repair has been completed",
            "maintenance action was performed",
            "maintenance has been performed",
            "work order completed",
            "approved",
            "no human review needed",
            "no human review is needed",
            "human review is not needed",
            "human review is unnecessary"
    );

    public void validate(TicketAgentSynthesisResult result, List<EvidenceChunkResponse> retrievedEvidence) {
        if (result == null) {
            throw new AgentValidationException("Agent response could not be parsed.");
        }

        validateStructure(result);
        validateEvidenceReferences(result, retrievedEvidence);
        validateForbiddenClaims(result);

        if (result.getNeedsHumanReview() == null || !result.getNeedsHumanReview()) {
            throw new AgentValidationException("Agent response needsHumanReview must be true.");
        }
    }

    private void validateStructure(TicketAgentSynthesisResult result) {
        if (isBlank(result.getInvestigationSummary()))
            throw new AgentValidationException("investigationSummary must not be blank.");
        if (isEmpty(result.getRecommendedNextSteps()))
            throw new AgentValidationException("recommendedNextSteps must not be empty.");
        if (isBlank(result.getDraftTechnicianResponse()))
            throw new AgentValidationException("draftTechnicianResponse must not be blank.");
        if (result.getConfidence() == null)
            throw new AgentValidationException("confidence must be LOW, MEDIUM, or HIGH.");
        if (isEmpty(result.getLimitations()))
            throw new AgentValidationException("limitations must not be empty.");
    }

    private void validateEvidenceReferences(TicketAgentSynthesisResult result,
                                             List<EvidenceChunkResponse> retrievedEvidence) {
        boolean evidenceWasRetrieved = retrievedEvidence != null && !retrievedEvidence.isEmpty();
        List<EvidenceRef> returnedRefs = result.getEvidenceRefs();
        boolean hasReturnedRefs = returnedRefs != null && !returnedRefs.isEmpty();

        if (!evidenceWasRetrieved) {
            if (hasReturnedRefs) {
                throw new AgentValidationException(
                        "Agent invented evidence references although no evidence was retrieved.");
            }
            return;
        }

        if (!hasReturnedRefs) {
            // Evidence may exist but not be directly cited; that's acceptable as
            // long as nothing invented is returned. Nothing further to check.
            return;
        }

        Set<String> validSourceRefs = retrievedEvidence.stream()
                .map(EvidenceChunkResponse::getSourceRef)
                .collect(Collectors.toSet());

        for (EvidenceRef ref : returnedRefs) {
            if (ref.getSourceRef() == null || !validSourceRefs.contains(ref.getSourceRef())) {
                throw new AgentValidationException(
                        "Agent invented a sourceRef that was not part of the retrieved evidence: "
                                + ref.getSourceRef());
            }
        }
    }

    private void validateForbiddenClaims(TicketAgentSynthesisResult result) {
        String haystack = String.join(" \n",
                nullToEmpty(result.getInvestigationSummary()),
                nullToEmpty(result.getPreviousReviewSummary()),
                nullToEmpty(result.getDraftTechnicianResponse()),
                String.join(" ", result.getRecommendedNextSteps() == null ? List.of() : result.getRecommendedNextSteps()),
                String.join(" ", result.getLimitations() == null ? List.of() : result.getLimitations())
        ).toLowerCase();

        for (String phrase : FORBIDDEN_CLAIM_PHRASES) {
            if (haystack.contains(phrase)) {
                throw new AgentValidationException(
                        "Agent output contains a forbidden claim: \"" + phrase + "\".");
            }
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isEmpty(List<?> list) { return list == null || list.isEmpty(); }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
