package com.genai.java.spring.aireview.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class TicketAiReviewResponse {

    private String summary;
    private List<String> possibleCauses;
    private List<String> recommendedChecks;
    private String draftResponse;
    private Confidence confidence;

    // Phase 4 — Section 9
    private List<String> limitations;
    private Boolean needsHumanReview;
}