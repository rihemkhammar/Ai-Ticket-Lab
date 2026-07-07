package com.genai.java.spring.agent.tool.dto;

import java.util.List;

public class RecommendationBoundaryResult {

    private List<String> allowedRecommendations;
    private List<String> forbiddenActions;

    public static RecommendationBoundaryResult of(List<String> allowed, List<String> forbidden) {
        RecommendationBoundaryResult r = new RecommendationBoundaryResult();
        r.allowedRecommendations = allowed;
        r.forbiddenActions = forbidden;
        return r;
    }

    public List<String> getAllowedRecommendations()          { return allowedRecommendations; }
    public void setAllowedRecommendations(List<String> v)    { this.allowedRecommendations = v; }

    public List<String> getForbiddenActions()                { return forbiddenActions; }
    public void setForbiddenActions(List<String> v)          { this.forbiddenActions = v; }
}
