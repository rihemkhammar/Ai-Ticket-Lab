package com.genai.java.spring.rag.review;

import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import com.genai.java.spring.rag.review.dto.TicketRagReviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  validates the parsed RAG AI response.
 *
 * Two layers:
 *  1. Structural validation (same idea as StructuralValidationAdvisor in M2).
 *  2. Evidence-reference validation (Section 2.5 / 2.9 of the story):
 *     - if evidence was retrieved and provided to GPT, evidenceRefs must not be empty
 *     - every sourceRef returned by the AI must exist in the retrieved evidence set
 *     - if no evidence was retrieved, evidenceRefs must be empty, confidence must be
 *       LOW, and limitations must explain that no relevant evidence was found
 */
@Component
public class RagReviewValidator {

    /**
     * @throws RagReviewValidationException with a human-readable reason on the first failure found.
     */
    public void validate(TicketRagReviewResponse response, List<EvidenceChunkResponse> retrievedEvidence) {
        if (response == null) {
            throw new RagReviewValidationException("AI response could not be parsed.");
        }

        validateStructure(response);
        validateEvidenceReferences(response, retrievedEvidence);

        if (response.getNeedsHumanReview() == null || !response.getNeedsHumanReview()) {
            throw new RagReviewValidationException("AI response needsHumanReview must be true.");
        }
    }

    private void validateStructure(TicketRagReviewResponse response) {
        if (isBlank(response.getSummary())) {
            throw new RagReviewValidationException("Summary must not be blank.");
        }
        if (isEmpty(response.getPossibleCauses())) {
            throw new RagReviewValidationException("Possible causes must not be empty.");
        }
        if (isEmpty(response.getRecommendedChecks())) {
            throw new RagReviewValidationException("Recommended checks must not be empty.");
        }
        if (isBlank(response.getDraftResponse())) {
            throw new RagReviewValidationException("Draft response must not be blank.");
        }
        if (response.getConfidence() == null) {
            throw new RagReviewValidationException("Confidence must be LOW, MEDIUM, or HIGH.");
        }
        if (isEmpty(response.getLimitations())) {
            throw new RagReviewValidationException("Limitations must not be empty.");
        }
    }

    private void validateEvidenceReferences(TicketRagReviewResponse response,
                                            List<EvidenceChunkResponse> retrievedEvidence) {
        boolean evidenceWasRetrieved = retrievedEvidence != null && !retrievedEvidence.isEmpty();
        List<EvidenceRef> returnedRefs = response.getEvidenceRefs();
        boolean hasReturnedRefs = returnedRefs != null && !returnedRefs.isEmpty();

        if (!evidenceWasRetrieved) {
            // No-evidence case (story section 6.10): evidenceRefs must stay empty
            // and limitations must explain why.
            if (hasReturnedRefs) {
                throw new RagReviewValidationException(
                        "AI invented evidence references although no evidence was retrieved.");
            }
            if (!mentionsMissingEvidence(response)) {
                throw new RagReviewValidationException(
                        "When no evidence is retrieved, limitations must state that evidence is insufficient.");
            }
            return;
        }

        // Evidence was retrieved and provided to GPT: evidenceRefs must exist.
        if (!hasReturnedRefs) {
            throw new RagReviewValidationException(
                    "Evidence was retrieved but the AI response did not include evidenceRefs.");
        }

        Set<String> validSourceRefs = retrievedEvidence.stream()
                .map(EvidenceChunkResponse::getSourceRef)
                .collect(Collectors.toSet());

        for (EvidenceRef ref : returnedRefs) {
            if (ref.getSourceRef() == null || !validSourceRefs.contains(ref.getSourceRef())) {
                throw new RagReviewValidationException(
                        "AI invented a sourceRef that was not part of the retrieved evidence: "
                                + ref.getSourceRef());
            }
        }
    }

    private boolean mentionsMissingEvidence(TicketRagReviewResponse response) {
        if (isEmpty(response.getLimitations())) {
            return false;
        }
        return response.getLimitations().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(l -> {
                    String lower = l.toLowerCase();
                    return lower.contains("no relevant evidence")
                            || lower.contains("no evidence")
                            || lower.contains("insufficient evidence")
                            || lower.contains("evidence is insufficient")
                            || lower.contains("evidence was found");
                });
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isEmpty(List<?> list) { return list == null || list.isEmpty(); }
}