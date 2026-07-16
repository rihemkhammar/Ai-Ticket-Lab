package com.genai.java.spring.hitl.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.prompt.TicketAgentPromptBuilder;
import com.genai.java.spring.agent.tool.dto.PreviousAiReviewResult;
import com.genai.java.spring.agent.tool.dto.RecommendationBoundaryResult;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serializes the context that produced a checkpoint's draft .
 *
 * This is NOT the raw chat transcript — it is a compact, human-readable
 * snapshot of what went into the prompt: the user goal, which evidence
 * refs were retrieved, how many previous reviews were used, the
 * recommendation boundaries, and the prompt/model versions. It lets a
 * reviewer (or a later developer) understand what context justified the
 * draft, without re-exposing chain-of-thought.
 */
@Slf4j
@Component
public class AgentPromptStateSerializer {

    private final ObjectMapper objectMapper;

    public AgentPromptStateSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(String userGoal,
                             List<EvidenceChunkResponse> evidence,
                             PreviousAiReviewResult previousReviews,
                             RecommendationBoundaryResult boundaries,
                             String modelName) {

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("promptVersion", TicketAgentPromptBuilder.PROMPT_VERSION);
        state.put("modelName", modelName);
        state.put("userGoal", userGoal);

        state.put("evidenceRefs", evidence == null ? List.of() :
                evidence.stream().map(EvidenceChunkResponse::getSourceRef).collect(Collectors.toList()));

        state.put("previousReviewCount",
                previousReviews == null || previousReviews.getReviews() == null
                        ? 0 : previousReviews.getReviews().size());

        state.put("allowedRecommendations", boundaries == null ? List.of() : boundaries.getAllowedRecommendations());
        state.put("forbiddenActions", boundaries == null ? List.of() : boundaries.getForbiddenActions());

        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            log.warn("Failed to serialize HITL prompt/state snapshot", e);
            return null;
        }
    }
}
