package com.genai.java.spring.agent;

import com.genai.java.spring.agent.dto.TicketAgentSynthesisResult;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import com.genai.java.spring.shared.advisor.HumanReviewPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates the final agent synthesis output.
 * Enforces structural completeness, evidence-reference honesty, and the
 * forbidden-claim guardrail that keeps the agent strictly advisory.
 *
 * L'invariant "needsHumanReview / limitations" est délégué à
 * HumanReviewPolicy (partagé avec aireview) pour éviter une deuxième
 * copie de cette règle de sécurité.
 */
@Component
@RequiredArgsConstructor
public class AgentOutputValidator {

    private final HumanReviewPolicy humanReviewPolicy;

    /**
     * Regex-based guardrail (S4-G02). Exact-phrase matching missed common
     * variants ("ticket was closed", "repair is complete", "work is
     * complete", ...). Patterns are matched against a whitespace-normalized,
     * lower-cased haystack, and each tolerates the is/was/has been/have been
     * auxiliary forms so paraphrases of the same claim are still caught.
     */
    private static final List<Pattern> FORBIDDEN_CLAIM_PATTERNS = List.of(
            // ticket (is|was|has been|have been)? closed/resolved
            Pattern.compile("\\bticket[s]?\\s+(?:is|was|has been|have been)?\\s*(?:already\\s+)?(?:closed|resolved)\\b"),
            // repair / work / work order / maintenance (action) (is|was|...)? complete(d)/done/finished
            Pattern.compile("\\b(?:repair|work order|work item|work|maintenance action|maintenance)\\s+" +
                    "(?:is|was|has been|have been)?\\s*(?:already\\s+)?(?:complete|completed|done|finished|performed)\\b"),
            // maintenance action was performed / maintenance has been performed (explicit story phrase)
            Pattern.compile("\\bmaintenance\\s+(?:action\\s+)?(?:was|has been|have been)\\s+performed\\b"),
            // no human review needed/necessary/required
            Pattern.compile("\\bno human review\\s+(?:is|was)?\\s*(?:needed|necessary|required)\\b"),
            // human review is not needed / unnecessary / not required
            Pattern.compile("\\bhuman review\\s+(?:is|was)?\\s*(?:not\\s+(?:needed|necessary|required)|unnecessary)\\b"),
            // bare "approved" claim
            Pattern.compile("\\bapproved\\b")
    );

    public void validate(TicketAgentSynthesisResult result, List<EvidenceChunkResponse> retrievedEvidence) {
        if (result == null) {
            throw new AgentValidationException("Agent response could not be parsed.");
        }

        validateStructure(result);
        validateEvidenceReferences(result, retrievedEvidence);
        validateForbiddenClaims(result);

        humanReviewPolicy.requireHumanReview(result.getNeedsHumanReview(), msg -> {
            throw new AgentValidationException(msg);
        });
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

        humanReviewPolicy.requireLimitations(result.getLimitations(), msg -> {
            throw new AgentValidationException(msg);
        });
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
            if (!limitationsExplainMissingEvidence(result.getLimitations())) {
                throw new AgentValidationException(
                        "Evidence was retrieved but evidenceRefs is empty and no limitation "
                                + "explains why no evidence was cited.");
            }
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

    /**
     * A limitation only counts as an explanation if it mentions "evidence"
     * together with a phrase indicating none was cited/applicable — merely
     * mentioning "evidence" in passing (e.g. "top-3 evidence chunks were
     * considered") is not an explanation for withholding evidenceRefs.
     */
    private static final Pattern EVIDENCE_NOT_CITED_PATTERN = Pattern.compile(
            "evidence.*(?:not (?:directly )?(?:applicable|relevant|cited)|no (?:relevant|matching) evidence|" +
                    "no evidence (?:references|refs)?\\s*(?:are|were|is|was)?\\s*cited|none (?:of the )?(?:retrieved )?evidence)" +
                    "|(?:not (?:directly )?(?:applicable|relevant)|no (?:relevant|matching)|none) .*evidence");

    private boolean limitationsExplainMissingEvidence(List<String> limitations) {
        if (limitations == null) {
            return false;
        }
        return limitations.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(l -> EVIDENCE_NOT_CITED_PATTERN.matcher(l).find());
    }

    private void validateForbiddenClaims(TicketAgentSynthesisResult result) {
        String haystack = String.join(" \n",
                nullToEmpty(result.getInvestigationSummary()),
                nullToEmpty(result.getPreviousReviewSummary()),
                nullToEmpty(result.getDraftTechnicianResponse()),
                String.join(" ", result.getRecommendedNextSteps() == null ? List.of() : result.getRecommendedNextSteps()),
                String.join(" ", result.getLimitations() == null ? List.of() : result.getLimitations())
        ).toLowerCase().replaceAll("\\s+", " ").trim();

        for (Pattern pattern : FORBIDDEN_CLAIM_PATTERNS) {
            if (pattern.matcher(haystack).find()) {
                throw new AgentValidationException(
                        "Agent output contains a forbidden claim matching pattern: \"" + pattern.pattern() + "\".");
            }
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isEmpty(List<?> list) { return list == null || list.isEmpty(); }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
}