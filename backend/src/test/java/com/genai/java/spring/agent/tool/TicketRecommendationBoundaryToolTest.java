package com.genai.java.spring.agent.tool;

import com.genai.java.spring.agent.tool.dto.RecommendationBoundaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketRecommendationBoundaryToolTest {

    private final TicketRecommendationBoundaryTool tool = new TicketRecommendationBoundaryTool();

    @Test
    @DisplayName("returns deterministic allowed and forbidden action lists")
    void load_returnsAllowedAndForbiddenActions() {
        RecommendationBoundaryResult result1 = tool.load();
        RecommendationBoundaryResult result2 = tool.load();

        assertThat(result1.getAllowedRecommendations())
                .contains("inspect equipment", "request human technician review");
        assertThat(result1.getForbiddenActions())
                .contains("close ticket", "mark repair complete", "say no human review is needed");

        // deterministic: same content on every call
        assertThat(result1.getAllowedRecommendations()).isEqualTo(result2.getAllowedRecommendations());
        assertThat(result1.getForbiddenActions()).isEqualTo(result2.getForbiddenActions());
    }
}
