package com.genai.java.spring.agent.dto;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Structured JSON returned directly by GPT for the final agent synthesis
 *  Parsed via ChatClient#entity(...), then validated
 * by AgentOutputValidator before being persisted and mapped to
 * TicketAgentInvestigationResponse.
 */
@Getter
@Setter
@ToString
public class TicketAgentSynthesisResult {

    private String investigationSummary;
    private List<EvidenceRef> evidenceRefs;
    private String previousReviewSummary;
    private List<String> recommendedNextSteps;
    private String draftTechnicianResponse;
    private Confidence confidence;
    private List<String> limitations;
    private Boolean needsHumanReview;
}
