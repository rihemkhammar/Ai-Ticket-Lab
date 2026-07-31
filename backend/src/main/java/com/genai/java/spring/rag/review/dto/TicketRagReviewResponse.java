package com.genai.java.spring.rag.review.dto;

import com.genai.java.spring.aireview.dto.Confidence;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class TicketRagReviewResponse implements java.io.Serializable {

    private String summary;
    private List<String> possibleCauses;
    private List<String> recommendedChecks;
    private String draftResponse;
    private List<EvidenceRef> evidenceRefs;
    private Confidence confidence;
    private List<String> limitations;
    private Boolean needsHumanReview;
}