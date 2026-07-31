package com.genai.java.spring.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.TriageClassification;
import com.genai.java.spring.triage.graph.TriageTreatedItem;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entry point for the triage batch feature.
 *
 * Responsibilities (Phase 3):
 *  1. Resolve the requested ticket set (explicit ids, or all OPEN tickets)
 *     without modifying TicketService/TicketRepository - filtering is done
 *     here, in-memory, over the existing findAll().
 *  2. Enforce the 5-ticket batch size limit (Rule 2.11).
 *  3. Persist a new triage_run row with status PENDING.
 *  4. Provide an atomic queue/treated update method for the graph nodes
 *     (used starting Phase 4) and expose the current run state to the
 *     frontend via getRun().
 *
 * Graph invocation (Phase 4): once TriageGraphConfig exposes a compiled
 * graph bean, startBatch() will move the run to RUNNING and invoke it
 * with the initial TriageGraphState right after persisting PENDING.
 * That wiring is intentionally not included yet, to keep Phase 3 and
 * Phase 4 independently testable, as specified by the story.
 */
@Service
public class TriageOrchestratorService {

    private static final int MAX_BATCH_SIZE = 5;
    private static final String CLASSIFICATION_PROMPT_VERSION = "ticket-triage-classification-v1";
    private static final String MODEL_NAME = "openai/gpt-oss-20b";

    private final TriageRunRepository triageRunRepository;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    public TriageOrchestratorService(TriageRunRepository triageRunRepository,
                                     TicketService ticketService,
                                     ObjectMapper objectMapper) {
        this.triageRunRepository = triageRunRepository;
        this.ticketService = ticketService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new triage_run in PENDING status for the requested batch.
     * Throws TriageValidationException if the resolved ticket set is
     * empty or exceeds MAX_BATCH_SIZE (Rule 2.11).
     */
    @Transactional
    public TriageRunResponse startBatch(TriageBatchRequest request) {
        List<Long> ticketIds = resolveTicketIds(request);

        if (ticketIds.isEmpty()) {
            throw new TriageValidationException(
                    "A triage batch requires at least one open ticket.");
        }
        if (ticketIds.size() > MAX_BATCH_SIZE) {
            throw new TriageValidationException(
                    "A triage batch cannot exceed " + MAX_BATCH_SIZE + " tickets.");
        }

        TriageRun run = new TriageRun();
        run.setStatus(TriageRunStatus.PENDING);
        run.setPromptVersion(CLASSIFICATION_PROMPT_VERSION);
        run.setModelName(MODEL_NAME);
        run.setTicketQueue(writeJson(ticketIds));
        run.setTreatedJson(writeJson(new ArrayList<TriageTreatedItem>()));
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());

        run = triageRunRepository.save(run);

        // Phase 4 will insert here:
        //   run.setStatus(RUNNING); triageRunRepository.save(run);
        //   TriageGraphState initialState = new TriageGraphState(run.getId(), ticketIds);
        //   compiledGraph.invoke(initialState);
        //   then finalize status COMPLETED based on the returned state.

        return toResponse(run);
    }

    public Optional<TriageRunResponse> getRun(Long runId) {
        return triageRunRepository.findById(runId).map(this::toResponse);
    }

    /**
     * Flips a freshly-created run from PENDING to RUNNING. Called by
     * TriagePipelineService right before it starts executing the
     * Classify/Order/Dispatch/Investigation/Review/Rules/HITL chain for
     * this run, so getRun() reflects that work is in progress.
     */
    @Transactional
    public void markRunning(Long runId) {
        TriageRun run = triageRunRepository.findById(runId)
                .orElseThrow(() -> new TriageValidationException("Triage run not found: " + runId));
        run.setStatus(TriageRunStatus.RUNNING);
        run.setUpdatedAt(LocalDateTime.now());
        triageRunRepository.save(run);
    }

    /**
     * Persists the full classification map (ALL tickets of the batch,
     * not just the one dispatched through the full pipeline) so the
     * frontend can display the complete ranking. Called once by
     * TriagePipelineService right after the graph finishes, using the
     * final state's classifications.
     */
    @Transactional
    public void recordClassifications(Long runId, Map<Long, TriageClassification> classifications) {
        TriageRun run = triageRunRepository.findById(runId)
                .orElseThrow(() -> new TriageValidationException("Triage run not found: " + runId));
        run.setClassificationsJson(writeJson(new ArrayList<>(classifications.values())));
        run.setUpdatedAt(LocalDateTime.now());
        triageRunRepository.save(run);
    }

    /**
     * Marks a run COMPLETED once the graph has genuinely reached END.
     * Replaces the old "ticketQueue is empty" heuristic in
     * recordTreated(), which no longer applies now that OrderQueueNode
     * only dispatches the single most critical ticket through the full
     * pipeline — the rest are classified but never "treated", so the
     * initial ticketQueue never empties on its own.
     */
    @Transactional
    public void markCompleted(Long runId) {
        TriageRun run = triageRunRepository.findById(runId)
                .orElseThrow(() -> new TriageValidationException("Triage run not found: " + runId));
        run.setStatus(TriageRunStatus.COMPLETED);
        run.setCompletedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        triageRunRepository.save(run);
    }

    /**
     * Atomically moves one ticket from the queue to the treated list.
     * Called by DispatchNextTicketNode / pipeline nodes starting Phase 4,
     * one ticket at a time, right after each stage of the pipeline
     * finishes for that ticket (Investigation -> Review -> Rules -> HITL).
     * Kept as a short, standalone transaction: it must NOT stay open
     * while the next ticket's pipeline call is in flight.
     */
    @Transactional
    public void recordTreated(Long runId, Long ticketId, TriageTreatedItem item) {
        TriageRun run = triageRunRepository.findById(runId)
                .orElseThrow(() -> new TriageValidationException("Triage run not found: " + runId));

        List<Long> remaining = readTicketQueue(run).stream()
                .filter(id -> !id.equals(ticketId))
                .collect(Collectors.toList());

        List<TriageTreatedItem> treated = readTreated(run);
        treated.add(item);

        run.setTicketQueue(writeJson(remaining));
        run.setTreatedJson(writeJson(treated));
        run.setUpdatedAt(LocalDateTime.now());
        // COMPLETED is now set explicitly by markCompleted(), called from
        // TriagePipelineService once the graph has actually reached END —
        // not inferred here from "ticketQueue empty", since only the top
        // ticket ever gets dispatched/treated (see OrderQueueNode).

        triageRunRepository.save(run);
    }

    // -- internal helpers -----------------------------------------------

    private List<Long> resolveTicketIds(TriageBatchRequest request) {
        if (request.isIncludeAllOpenTickets()) {
            return ticketService.findAll().stream()
                    .filter(t -> t.getStatus() == TicketStatus.OPEN)
                    .map(Ticket::getId)
                    .collect(Collectors.toList());
        }
        if (request.getTicketIds() == null) {
            return List.of();
        }
        return request.getTicketIds();
    }

    private TriageRunResponse toResponse(TriageRun run) {
        TriageRunResponse response = new TriageRunResponse();
        response.setRunId(run.getId());
        response.setStatus(run.getStatus());
        response.setPromptVersion(run.getPromptVersion());
        response.setTicketQueue(readTicketQueue(run));
        response.setClassifications(readClassifications(run));
        response.setTreated(readTreated(run));
        response.setErrorMessage(run.getErrorMessage());
        response.setCreatedAt(run.getCreatedAt());
        response.setCompletedAt(run.getCompletedAt());
        return response;
    }

    private List<Long> readTicketQueue(TriageRun run) {
        return readJson(run.getTicketQueue(), Long.class);
    }

    private List<TriageTreatedItem> readTreated(TriageRun run) {
        return readJson(run.getTreatedJson(), TriageTreatedItem.class);
    }

    private Map<Long, TriageClassification> readClassifications(TriageRun run) {
        return readJson(run.getClassificationsJson(), TriageClassification.class).stream()
                .collect(Collectors.toMap(TriageClassification::getTicketId, c -> c));
    }

    private <T> String writeJson(List<T> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize triage_run JSON field.", e);
        }
    }

    private <T> List<T> readJson(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.JavaType listType =
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize triage_run JSON field.", e);
        }
    }
}