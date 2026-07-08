package com.genai.java.spring.rag.review;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import com.genai.java.spring.rag.review.dto.TicketRagReviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RagReviewValidator {

    public void validate(TicketRagReviewResponse response, List<EvidenceChunkResponse> retrievedEvidence) {
        if (response == null) {
            throw new RagReviewValidationException("AI response could not be parsed.");
        }

        boolean isNoFindingResponse = isLowConfidenceNoFinding(response);

        validateStructure(response, isNoFindingResponse);
        validateEvidenceReferences(response, retrievedEvidence, isNoFindingResponse);

        if (response.getNeedsHumanReview() == null || !response.getNeedsHumanReview()) {
            throw new RagReviewValidationException("AI response needsHumanReview must be true.");
        }
    }

    /**
     * Cas légitime : le modèle n'a identifié aucune cause exploitable.
     * On l'accepte seulement si confidence=LOW ET aucune cause/check/ref renvoyé,
     * pour éviter qu'un modèle "paresseux" abuse de cette règle en HIGH/MEDIUM.
     */
    private boolean isLowConfidenceNoFinding(TicketRagReviewResponse response) {
        return response.getConfidence() == Confidence.LOW
                && isEmpty(response.getPossibleCauses())
                && isEmpty(response.getRecommendedChecks())
                && isEmpty(response.getEvidenceRefs());
    }

    private void validateStructure(TicketRagReviewResponse response, boolean isNoFindingResponse) {
        if (isBlank(response.getSummary()))
            throw new RagReviewValidationException("Summary must not be blank.");

        if (!isNoFindingResponse && isEmpty(response.getPossibleCauses()))
            throw new RagReviewValidationException("Possible causes must not be empty.");

        if (!isNoFindingResponse && isEmpty(response.getRecommendedChecks()))
            throw new RagReviewValidationException("Recommended checks must not be empty.");

        if (isBlank(response.getDraftResponse()))
            throw new RagReviewValidationException("Draft response must not be blank.");
        if (response.getConfidence() == null)
            throw new RagReviewValidationException("Confidence must be LOW, MEDIUM, or HIGH.");
        if (isEmpty(response.getLimitations()))
            throw new RagReviewValidationException("Limitations must not be empty.");
    }

    private void validateEvidenceReferences(TicketRagReviewResponse response,
                                            List<EvidenceChunkResponse> retrievedEvidence,
                                            boolean isNoFindingResponse) {
        boolean evidenceWasRetrieved = retrievedEvidence != null && !retrievedEvidence.isEmpty();
        List<EvidenceRef> returnedRefs = response.getEvidenceRefs();
        boolean hasReturnedRefs = returnedRefs != null && !returnedRefs.isEmpty();

        if (!evidenceWasRetrieved) {
            if (hasReturnedRefs)
                throw new RagReviewValidationException(
                        "AI invented evidence references although no evidence was retrieved.");
            if (response.getConfidence() != Confidence.LOW)
                throw new RagReviewValidationException(
                        "When no evidence is retrieved, confidence must be LOW (got: "
                                + response.getConfidence() + ").");
            if (!mentionsMissingEvidence(response))
                throw new RagReviewValidationException(
                        "When no evidence is retrieved, limitations must state that evidence is insufficient.");
            return;
        }

        // Evidence a été récupérée (ex: ticket 4 : 3 chunks), mais le modèle a jugé
        // qu'aucun n'était réellement pertinent -> on l'accepte si confidence=LOW.
        if (!hasReturnedRefs) {
            if (isNoFindingResponse) {
                return;
            }
            throw new RagReviewValidationException(
                    "Evidence was retrieved but the AI response did not include evidenceRefs.");
        }

        Set<String> validSourceRefs = retrievedEvidence.stream()
                .map(EvidenceChunkResponse::getSourceRef)
                .collect(Collectors.toSet());

        for (EvidenceRef ref : returnedRefs) {
            if (ref.getSourceRef() == null || !validSourceRefs.contains(ref.getSourceRef()))
                throw new RagReviewValidationException(
                        "AI invented a sourceRef that was not part of the retrieved evidence: "
                                + ref.getSourceRef());
        }
    }

    private boolean mentionsMissingEvidence(TicketRagReviewResponse response) {
        if (isEmpty(response.getLimitations())) return false;
        return response.getLimitations().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(l -> {
                    String lower = l.toLowerCase();
                    return lower.contains("no relevant evidence")
                            || lower.contains("no evidence")
                            || lower.contains("insufficient evidence")
                            || lower.contains("evidence is insufficient");
                });
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isEmpty(List<?> list) { return list == null || list.isEmpty(); }
}