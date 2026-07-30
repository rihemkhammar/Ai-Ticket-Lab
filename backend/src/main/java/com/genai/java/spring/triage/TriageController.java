package com.genai.java.spring.triage;

import com.genai.java.spring.aireview.dto.ErrorResponse;
import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final TriageOrchestratorService service;
    private final TriagePipelineService pipelineService;

    public TriageController(TriageOrchestratorService service, TriagePipelineService pipelineService) {
        this.service = service;
        this.pipelineService = pipelineService;
    }

    // Runs the full chain synchronously: Classify -> Order -> Dispatch
    // -> Investigation -> Review -> Rules -> HITL -> observation, for
    // every ticket in the batch, before responding (see TriagePipelineService).
    // /api/triage/** is behind JWT auth (SecurityConfig: anyRequest().authenticated()),
    // so `authentication` is always populated here — its name is passed
    // through to Agent 3 (Review) so stored reviews are attributed to the
    // real technician who launched the batch, not a fake system account.
    @PostMapping("/batches")
    public ResponseEntity<TriageRunResponse> startBatch(@RequestBody TriageBatchRequest request,
                                                        Authentication authentication) {
        TriageRunResponse response = pipelineService.startAndRun(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batches/{runId}")
    public ResponseEntity<TriageRunResponse> getBatch(@PathVariable Long runId) {
        return service.getRun(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Reuses the ErrorResponse DTO already defined for the M4 agent
    // controller instead of creating a new error shape.
    @ExceptionHandler(TriageValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(TriageValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }
}