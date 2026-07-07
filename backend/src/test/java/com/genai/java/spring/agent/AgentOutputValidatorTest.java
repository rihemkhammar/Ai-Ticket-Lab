package com.genai.java.spring.agent;

import com.genai.java.spring.agent.dto.TicketAgentSynthesisResult;
import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentOutputValidatorTest {

    private AgentOutputValidator validator;

    @BeforeEach
    void setUp() { validator = new AgentOutputValidator(); }

    private EvidenceChunkResponse chunk() {
        return EvidenceChunkResponse.of(1L, 0, "text", "Motor Guide", "MOTOR", 0.9);
    }

    private EvidenceRef ref(String sourceRef) {
        EvidenceRef r = new EvidenceRef();
        r.setSourceRef(sourceRef);
        r.setArticleTitle("Motor Guide");
        return r;
    }

    private TicketAgentSynthesisResult validResult() {
        TicketAgentSynthesisResult r = new TicketAgentSynthesisResult();
        r.setInvestigationSummary("Motor overheating investigated, likely ventilation issue.");
        r.setEvidenceRefs(List.of());
        r.setPreviousReviewSummary("No previous AI reviews exist for this ticket.");
        r.setRecommendedNextSteps(List.of("inspect equipment", "verify symptoms"));
        r.setDraftTechnicianResponse("Please inspect the cooling vent and report back.");
        r.setConfidence(Confidence.MEDIUM);
        r.setLimitations(List.of("Only top-3 evidence chunks were considered."));
        r.setNeedsHumanReview(true);
        return r;
    }

    // ── structural ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid response passes")
    void valid_passes() {
        assertThatNoException().isThrownBy(() -> validator.validate(validResult(), List.of()));
    }

    @Test
    @DisplayName("null response -> fails")
    void nullResponse_fails() {
        assertThatThrownBy(() -> validator.validate(null, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("could not be parsed");
    }

    @Test
    @DisplayName("blank investigationSummary -> fails")
    void blankSummary_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setInvestigationSummary("  ");
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("investigationSummary");
    }

    @Test
    @DisplayName("empty recommendedNextSteps -> fails")
    void emptyNextSteps_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setRecommendedNextSteps(List.of());
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("recommendedNextSteps");
    }

    @Test
    @DisplayName("empty limitations -> fails")
    void emptyLimitations_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setLimitations(List.of());
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("limitations");
    }

    @Test
    @DisplayName("needsHumanReview = false -> fails")
    void needsHumanReviewFalse_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setNeedsHumanReview(false);
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("needsHumanReview");
    }

    @Test
    @DisplayName("missing confidence -> fails")
    void missingConfidence_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setConfidence(null);
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("confidence");
    }

    // ── evidence reference honesty ──────────────────────────────────────────────

    @Test
    @DisplayName("evidenceRefs matching retrieved evidence passes")
    void matchingEvidenceRefs_passes() {
        TicketAgentSynthesisResult r = validResult();
        r.setEvidenceRefs(List.of(ref("article:1#chunk:0")));
        assertThatNoException().isThrownBy(() -> validator.validate(r, List.of(chunk())));
    }

    @Test
    @DisplayName("invented evidence sourceRef not in retrieved evidence -> fails")
    void inventedSourceRef_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setEvidenceRefs(List.of(ref("article:99#chunk:0")));
        assertThatThrownBy(() -> validator.validate(r, List.of(chunk())))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("invented a sourceRef");
    }

    @Test
    @DisplayName("evidenceRefs present although no evidence was retrieved -> fails")
    void evidenceRefsWithoutRetrieval_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setEvidenceRefs(List.of(ref("article:1#chunk:0")));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("invented evidence references");
    }

    // ── forbidden-claim guardrail ────────────────────────────────────────────────

    @Test
    @DisplayName("claiming ticket was closed -> fails")
    void claimTicketClosed_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setInvestigationSummary("The ticket is closed after inspection.");
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("forbidden claim");
    }

    @Test
    @DisplayName("claiming repair was completed -> fails")
    void claimRepairCompleted_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setDraftTechnicianResponse("Repair completed, motor now runs at normal temperature.");
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("forbidden claim");
    }

    @Test
    @DisplayName("claiming maintenance action was performed -> fails")
    void claimMaintenancePerformed_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setRecommendedNextSteps(List.of("maintenance action was performed on the motor"));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("forbidden claim");
    }

    @Test
    @DisplayName("claiming human review is unnecessary -> fails")
    void claimNoHumanReviewNeeded_fails() {
        TicketAgentSynthesisResult r = validResult();
        r.setLimitations(List.of("no human review needed for this straightforward case"));
        assertThatThrownBy(() -> validator.validate(r, List.of()))
                .isInstanceOf(AgentValidationException.class)
                .hasMessageContaining("forbidden claim");
    }
}
