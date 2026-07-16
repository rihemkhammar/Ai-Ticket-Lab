package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.aireview.dto.Confidence;
import com.genai.java.spring.rag.review.dto.EvidenceRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Structured JSON returned by GPT for a HITL draft (initial or revised).
 * Same shape used for the checkpoint's draft_json and for the revision
 * generation output .
 */
@Getter
@Setter
@ToString
public class HitlDraft {

    private String investigationSummary;
    private List<EvidenceRef> evidenceRefs;
    private String previousReviewSummary;
    private List<String> recommendedNextSteps;
    private String draftTechnicianResponse;
    private Confidence confidence;
    private List<String> limitations;
    private Boolean needsHumanReview;
}
