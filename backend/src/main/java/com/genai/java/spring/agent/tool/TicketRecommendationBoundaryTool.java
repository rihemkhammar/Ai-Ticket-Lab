package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.RecommendationBoundaryResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tool 4 — TicketRecommendationBoundaryTool .
 * Deterministic, read-only. Reinforces the agent's guardrails by handing
 * the model an explicit allow/forbid list.
 */
@Component
public class TicketRecommendationBoundaryTool {

    public static final String NAME = "TicketRecommendationBoundaryTool";

    private static final List<String> ALLOWED = List.of(
            "inspect equipment",
            "verify symptoms",
            "check safety procedure",
            "request human technician review",
            "draft a response"
    );

    private static final List<String> FORBIDDEN = List.of(
            "close ticket",
            "mark repair complete",
            "change ticket status",
            "claim physical work was performed",
            "say no human review is needed"
    );

    public RecommendationBoundaryResult load() {
        return RecommendationBoundaryResult.of(ALLOWED, FORBIDDEN);
    }
}
