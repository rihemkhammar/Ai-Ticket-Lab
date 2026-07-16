package com.genai.java.spring.hitl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.agent.AgentRunStatus;
import com.genai.java.spring.exception.GlobalExceptionHandler;
import com.genai.java.spring.hitl.HitlValidationException;
import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.dto.HumanReviewDecisionRequest;
import com.genai.java.spring.hitl.dto.HumanReviewDecisionResponse;
import com.genai.java.spring.hitl.service.HumanReviewDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *  POST /api/agent-runs/{runId}/human-review/decision.
 * Standalone MockMvc test (no Spring context, no security filters) wired
 * with the real GlobalExceptionHandler so HitlValidationException -> 422
 * mapping is exercised end-to-end.
 */
@ExtendWith(MockitoExtension.class)
class HumanReviewDecisionControllerTest {

    @Mock private HumanReviewDecisionService humanReviewDecisionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long RUN_ID = 10L;

    @BeforeEach
    void setUp() {
        HumanReviewDecisionController controller = new HumanReviewDecisionController(humanReviewDecisionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("APPROVE decision returns 200 with finalStatus=FINALIZED")
    void decide_approve_returnsFinalizedResponse() throws Exception {
        HumanReviewDecisionResponse response = new HumanReviewDecisionResponse();
        response.setRunId(RUN_ID);
        response.setFinalStatus(AgentRunStatus.FINALIZED);
        response.setHumanDecision(HumanReviewDecision.APPROVE);
        response.setHumanReviewed(true);
        response.setOfficialActionExecuted(false);
        response.setTicketStatusChanged(false);

        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenReturn(response);

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);
        request.setComment("Looks good.");

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalStatus").value("FINALIZED"))
                .andExpect(jsonPath("$.humanDecision").value("APPROVE"))
                .andExpect(jsonPath("$.humanReviewed").value(true))
                .andExpect(jsonPath("$.officialActionExecuted").value(false))
                .andExpect(jsonPath("$.ticketStatusChanged").value(false));
    }

    @Test
    @DisplayName("REJECT decision returns 200 with finalStatus=REJECTED")
    void decide_reject_returnsRejectedResponse() throws Exception {
        HumanReviewDecisionResponse response = new HumanReviewDecisionResponse();
        response.setRunId(RUN_ID);
        response.setFinalStatus(AgentRunStatus.REJECTED);
        response.setHumanDecision(HumanReviewDecision.REJECT);

        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenReturn(response);

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REJECT);
        request.setComment("Not accurate enough.");

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("REQUEST_REVISION decision returns 200 with a revisedReview payload")
    void decide_requestRevision_returnsRevisedReview() throws Exception {
        HumanReviewDecisionResponse response = new HumanReviewDecisionResponse();
        response.setRunId(RUN_ID);
        response.setFinalStatus(AgentRunStatus.WAITING_FOR_HUMAN);
        response.setHumanDecision(HumanReviewDecision.REQUEST_REVISION);

        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenReturn(response);

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REQUEST_REVISION);
        request.setComment("Please add more detail on electrical checks.");

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalStatus").value("WAITING_FOR_HUMAN"));
    }

    @Test
    @DisplayName("a missing required comment (REJECT) is mapped to HTTP 422 via GlobalExceptionHandler")
    void decide_missingRequiredComment_returns422() throws Exception {
        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenThrow(new HitlValidationException("comment is required for reject."));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REJECT);

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("comment is required for reject."));
    }

    @Test
    @DisplayName("a decision on a non-WAITING_FOR_HUMAN run is mapped to HTTP 422")
    void decide_onNonWaitingRun_returns422() throws Exception {
        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenThrow(new HitlValidationException(
                        "Decision rejected: agent run " + RUN_ID + " is not WAITING_FOR_HUMAN (current status: FINALIZED)."));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.APPROVE);

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("a second revision request beyond the one-cycle limit is mapped to HTTP 422")
    void decide_secondRevisionRequest_returns422() throws Exception {
        when(humanReviewDecisionService.applyDecision(eq(RUN_ID), any(HumanReviewDecisionRequest.class)))
                .thenThrow(new HitlValidationException("Only one revision cycle is supported in this training milestone."));

        HumanReviewDecisionRequest request = new HumanReviewDecisionRequest();
        request.setDecision(HumanReviewDecision.REQUEST_REVISION);
        request.setComment("One more pass please.");

        mockMvc.perform(post("/api/agent-runs/{runId}/human-review/decision", RUN_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Only one revision cycle is supported in this training milestone."));
    }
}
