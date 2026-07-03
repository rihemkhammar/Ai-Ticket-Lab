package com.genai.java.spring.rag.review;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import com.genai.java.spring.rag.review.dto.TicketRagReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagReviewValidatorTest {

    private RagReviewValidator validator;

    @BeforeEach
    void setUp() { validator = new RagReviewValidator(); }

    private TicketRagReviewResponse validResponseWithEvidence(String sourceRef) {
        TicketRagReviewResponse r = new TicketRagReviewResponse();
        r.setSummary("Motor overheating detected.");
        r.setPossibleCauses(List.of("Blocked cooling vent"));
        r.setRecommendedChecks(List.of("Check fan filter"));
        r.setDraftResponse("Please schedule maintenance.");
        r.setConfidence(Confidence.MEDIUM);
        r.setLimitations(List.of("Only top-3 chunks used."));
        r.setNeedsHumanReview(true);
        EvidenceRef ref = new EvidenceRef();
        ref.setSourceRef(sourceRef);
        ref.setArticleTitle("Motor Maintenance Guide");
        r.setEvidenceRefs(List.of(ref));
        return r;
    }

    private TicketRagReviewResponse validNoEvidenceResponse() {
        TicketRagReviewResponse r = new TicketRagReviewResponse();
        r.setSummary("Ticket analyzed without knowledge base.");
        r.setPossibleCauses(List.of("Unknown root cause"));
        r.setRecommendedChecks(List.of("Manual inspection required"));
        r.setDraftResponse("Escalate to senior technician.");
        r.setConfidence(Confidence.LOW);
        r.setLimitations(List.of("No relevant evidence was found in the knowledge base."));
        r.setNeedsHumanReview(true);
        r.setEvidenceRefs(List.of());
        return r;
    }

    private EvidenceChunkResponse chunk() {
        return EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9);
    }

    // ── with evidence ─────────────────────────────────────────────────────────

    @Test @DisplayName("valid response with matching evidenceRef passes")
    void withEvidence_valid() {
        assertThatNoException().isThrownBy(
                () -> validator.validate(validResponseWithEvidence("article:1#chunk:0"), List.of(chunk())));
    }

    @Test @DisplayName("AI invents a sourceRef not in retrieved evidence -> fails")
    void withEvidence_inventedSourceRef() {
        assertThatThrownBy(() -> validator.validate(
                validResponseWithEvidence("article:99#chunk:0"), List.of(chunk())))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("invented a sourceRef");
    }

    @Test @DisplayName("evidence retrieved but AI returns empty evidenceRefs -> fails")
    void withEvidence_missingRefs() {
        TicketRagReviewResponse r = validResponseWithEvidence("article:1#chunk:0");
        r.setEvidenceRefs(List.of());
        assertThatThrownBy(() -> validator.validate(r, List.of(chunk())))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("did not include evidenceRefs");
    }

    // ── no evidence ───────────────────────────────────────────────────────────

    @Test @DisplayName("S3-G02: valid no-evidence response passes")
    void noEvidence_valid() {
        assertThatNoException().isThrownBy(
                () -> validator.validate(validNoEvidenceResponse(), List.of()));
    }

    @Test @DisplayName("S3-G02: no evidence + MEDIUM confidence -> fails")
    void noEvidence_mediumConfidenceFails() {
        TicketRagReviewResponse r = validNoEvidenceResponse();
        r.setConfidence(Confidence.MEDIUM);
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("confidence must be LOW");
    }

    @Test @DisplayName("S3-G02: no evidence + HIGH confidence -> fails")
    void noEvidence_highConfidenceFails() {
        TicketRagReviewResponse r = validNoEvidenceResponse();
        r.setConfidence(Confidence.HIGH);
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("confidence must be LOW");
    }

    @Test @DisplayName("S3-G02: no evidence + invented refs -> fails")
    void noEvidence_inventedRefsFails() {
        TicketRagReviewResponse r = validNoEvidenceResponse();
        EvidenceRef ref = new EvidenceRef();
        ref.setSourceRef("article:1#chunk:0");
        r.setEvidenceRefs(List.of(ref));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("invented evidence references");
    }

    @Test @DisplayName("S3-G02: limitation vague sans mention evidence -> fails")
    void noEvidence_limitationVague() {
        TicketRagReviewResponse r = validNoEvidenceResponse();
        r.setLimitations(List.of("This is a vague limitation."));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("limitations must state that evidence is insufficient");
    }

    @Test @DisplayName("S3-G02: ancien wording 'evidence was found' ne doit plus passer")
    void noEvidence_permissiveEvidenceFoundPhraseFails() {
        TicketRagReviewResponse r = validNoEvidenceResponse();
        r.setLimitations(List.of("Evidence was found to be inconclusive."));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("limitations must state that evidence is insufficient");
    }

    // ── structural ────────────────────────────────────────────────────────────

    @Test @DisplayName("null response -> fails")
    void nullResponse() {
        assertThatThrownBy(() -> validator.validate(null, List.of()))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test @DisplayName("blank summary -> fails")
    void blankSummary() {
        TicketRagReviewResponse r = validResponseWithEvidence("article:1#chunk:0");
        r.setSummary("  ");
        assertThatThrownBy(() -> validator.validate(r, List.of(chunk())))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("Summary");
    }

    @Test @DisplayName("needsHumanReview = false -> fails")
    void needsHumanReviewFalse() {
        TicketRagReviewResponse r = validResponseWithEvidence("article:1#chunk:0");
        r.setNeedsHumanReview(false);
        assertThatThrownBy(() -> validator.validate(r, List.of(chunk())))
                .isInstanceOf(RagReviewValidationException.class)
                .hasMessageContaining("needsHumanReview");
    }
}