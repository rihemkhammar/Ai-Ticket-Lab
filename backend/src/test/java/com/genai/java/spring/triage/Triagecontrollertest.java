package com.genai.java.spring.triage;

import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageControllerTest {

    @Mock private TriageOrchestratorService service;
    @Mock private TriagePipelineService pipelineService;
    @Mock private Authentication authentication;

    private TriageController controller;

    @BeforeEach
    void setUp() {
        controller = new TriageController(service, pipelineService);
        lenient().when(authentication.getName()).thenReturn("alice");
    }

    @Test
    @DisplayName("startBatch delegates to the pipeline service with the authenticated username and returns 200 OK")
    void startBatch_delegatesToPipelineService() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L, 2L));

        TriageRunResponse expected = new TriageRunResponse();
        expected.setRunId(1L);
        expected.setStatus(TriageRunStatus.PENDING);

        when(pipelineService.startAndRun(request, "alice")).thenReturn(expected);

        ResponseEntity<TriageRunResponse> response = controller.startBatch(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("getBatch returns 200 OK with the run when it exists")
    void getBatch_existingRun_returnsOk() {
        TriageRunResponse expected = new TriageRunResponse();
        expected.setRunId(5L);
        when(service.getRun(5L)).thenReturn(Optional.of(expected));

        ResponseEntity<TriageRunResponse> response = controller.getBatch(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("getBatch returns 404 when the run does not exist")
    void getBatch_missingRun_returnsNotFound() {
        when(service.getRun(99L)).thenReturn(Optional.empty());

        ResponseEntity<TriageRunResponse> response = controller.getBatch(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("handleValidation maps TriageValidationException to 400 Bad Request")
    void handleValidation_mapsToBadRequest() {
        TriageValidationException ex = new TriageValidationException("A triage batch requires at least one open ticket.");

        var response = controller.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("A triage batch requires at least one open ticket.");
    }
}