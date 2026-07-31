package com.genai.java.spring.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.genai.java.spring.ticket.Ticket;
import com.genai.java.spring.ticket.TicketService;
import com.genai.java.spring.ticket.TicketStatus;
import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.TriageTreatedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageOrchestratorServiceTest {

    @Mock private TriageRunRepository triageRunRepository;
    @Mock private TicketService ticketService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private TriageOrchestratorService service;

    @BeforeEach
    void setUp() {
        service = new TriageOrchestratorService(triageRunRepository, ticketService, objectMapper);
    }

    private Ticket ticket(Long id, TicketStatus status) {
        Ticket t = mock(Ticket.class);
        org.mockito.Mockito.lenient().when(t.getId()).thenReturn(id);
        when(t.getStatus()).thenReturn(status);
        return t;
    }

    // -- startBatch: explicit ticket ids -----------------------------------

    @Test
    @DisplayName("startBatch persists a PENDING run for explicit ticket ids")
    void startBatch_explicitIds_createsPendingRun() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L, 2L, 3L));

        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> {
            TriageRun run = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 10L);
            return run;
        });

        TriageRunResponse response = service.startBatch(request);

        assertThat(response.getRunId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(TriageRunStatus.PENDING);
        assertThat(response.getTicketQueue()).containsExactly(1L, 2L, 3L);
        assertThat(response.getTreated()).isEmpty();
        assertThat(response.getPromptVersion()).isEqualTo("ticket-triage-classification-v1");
    }

    @Test
    @DisplayName("startBatch resolves includeAllOpenTickets against only OPEN tickets")
    void startBatch_includeAllOpenTickets_filtersByOpenStatus() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setIncludeAllOpenTickets(true);

        List<Ticket> tickets = List.of(
                ticket(1L, TicketStatus.OPEN),
                ticket(2L, TicketStatus.CLOSED),
                ticket(3L, TicketStatus.OPEN),
                ticket(4L, TicketStatus.IN_PROGRESS)
        );
        when(ticketService.findAll()).thenReturn(tickets);
        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> {
            TriageRun run = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 11L);
            return run;
        });

        TriageRunResponse response = service.startBatch(request);

        assertThat(response.getTicketQueue()).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("startBatch rejects an empty ticket set")
    void startBatch_emptyTicketSet_throwsValidationException() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of());

        assertThatThrownBy(() -> service.startBatch(request))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("at least one open ticket");
    }

    @Test
    @DisplayName("startBatch rejects a null ticket id list when includeAllOpenTickets is false")
    void startBatch_nullTicketIds_throwsValidationException() {
        TriageBatchRequest request = new TriageBatchRequest();

        assertThatThrownBy(() -> service.startBatch(request))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("at least one open ticket");
    }

    @Test
    @DisplayName("startBatch rejects a batch exceeding the 5-ticket limit (Rule 2.11)")
    void startBatch_tooManyTickets_throwsValidationException() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThatThrownBy(() -> service.startBatch(request))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("cannot exceed 5 tickets");
    }

    @Test
    @DisplayName("startBatch accepts exactly the 5-ticket limit")
    void startBatch_exactlyFiveTickets_isAccepted() {
        TriageBatchRequest request = new TriageBatchRequest();
        request.setTicketIds(List.of(1L, 2L, 3L, 4L, 5L));

        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> {
            TriageRun run = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 12L);
            return run;
        });

        TriageRunResponse response = service.startBatch(request);

        assertThat(response.getTicketQueue()).hasSize(5);
    }

    // -- getRun ---------------------------------------------------------

    @Test
    @DisplayName("getRun returns a mapped response when the run exists")
    void getRun_existingRun_returnsResponse() throws Exception {
        TriageRun run = new TriageRun();
        org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 20L);
        run.setStatus(TriageRunStatus.RUNNING);
        run.setPromptVersion("ticket-triage-classification-v1");
        run.setModelName("openai/gpt-oss-20b");
        run.setTicketQueue(objectMapper.writeValueAsString(List.of(1L, 2L)));
        run.setTreatedJson(objectMapper.writeValueAsString(List.of()));
        run.setCreatedAt(LocalDateTime.now());

        when(triageRunRepository.findById(20L)).thenReturn(Optional.of(run));

        Optional<TriageRunResponse> response = service.getRun(20L);

        assertThat(response).isPresent();
        assertThat(response.get().getRunId()).isEqualTo(20L);
        assertThat(response.get().getStatus()).isEqualTo(TriageRunStatus.RUNNING);
        assertThat(response.get().getTicketQueue()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("getRun returns empty when the run does not exist")
    void getRun_missingRun_returnsEmpty() {
        when(triageRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getRun(99L)).isEmpty();
    }

    // -- recordTreated ----------------------------------------------------

    @Test
    @DisplayName("recordTreated removes the ticket from the queue and appends to treated")
    void recordTreated_removesFromQueue_appendsTreated() throws Exception {
        TriageRun run = new TriageRun();
        org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 30L);
        run.setStatus(TriageRunStatus.RUNNING);
        run.setTicketQueue(objectMapper.writeValueAsString(List.of(1L, 2L, 3L)));
        run.setTreatedJson(objectMapper.writeValueAsString(List.of()));

        when(triageRunRepository.findById(30L)).thenReturn(Optional.of(run));
        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TriageTreatedItem item = TriageTreatedItem.success(
                1L, TicketCriticality.HIGH, 100L, LocalDateTime.now(), null);

        service.recordTreated(30L, 1L, item);

        ArgumentCaptor<TriageRun> captor = ArgumentCaptor.forClass(TriageRun.class);
        org.mockito.Mockito.verify(triageRunRepository).save(captor.capture());

        TriageRun saved = captor.getValue();
        List<Long> remainingQueue = objectMapper.readValue(
                saved.getTicketQueue(), objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        assertThat(remainingQueue).containsExactly(2L, 3L);
        assertThat(saved.getStatus()).isEqualTo(TriageRunStatus.RUNNING);
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("recordTreated no longer auto-completes the run (completion is explicit via markCompleted)")
    void recordTreated_lastTicket_doesNotAutoCompleteRun() throws Exception {
        // recordTreated() no longer infers COMPLETED from an empty
        // ticketQueue: since OrderQueueNode now only ever dispatches the
        // single most critical ticket, the original batch's ticketQueue
        // (used for display) would otherwise never empty out. Completion
        // is now decided explicitly by markCompleted(), called from
        // TriagePipelineService once the graph has really reached END.
        TriageRun run = new TriageRun();
        org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 31L);
        run.setStatus(TriageRunStatus.RUNNING);
        run.setTicketQueue(objectMapper.writeValueAsString(List.of(5L)));
        run.setTreatedJson(objectMapper.writeValueAsString(List.of()));

        when(triageRunRepository.findById(31L)).thenReturn(Optional.of(run));
        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TriageTreatedItem item = TriageTreatedItem.failure(
                5L, TicketCriticality.LOW, "dispatch failed", LocalDateTime.now());

        service.recordTreated(31L, 5L, item);

        ArgumentCaptor<TriageRun> captor = ArgumentCaptor.forClass(TriageRun.class);
        org.mockito.Mockito.verify(triageRunRepository).save(captor.capture());

        TriageRun saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TriageRunStatus.RUNNING);
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("markCompleted sets status COMPLETED and completedAt")
    void markCompleted_setsCompletedStatusAndTimestamp() {
        TriageRun run = new TriageRun();
        org.springframework.test.util.ReflectionTestUtils.setField(run, "id", 31L);
        run.setStatus(TriageRunStatus.RUNNING);

        when(triageRunRepository.findById(31L)).thenReturn(Optional.of(run));
        when(triageRunRepository.save(any(TriageRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompleted(31L);

        ArgumentCaptor<TriageRun> captor = ArgumentCaptor.forClass(TriageRun.class);
        org.mockito.Mockito.verify(triageRunRepository).save(captor.capture());

        TriageRun saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TriageRunStatus.COMPLETED);
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("recordTreated throws when the run does not exist")
    void recordTreated_missingRun_throwsValidationException() {
        when(triageRunRepository.findById(999L)).thenReturn(Optional.empty());

        TriageTreatedItem item = TriageTreatedItem.success(
                1L, TicketCriticality.MEDIUM, 1L, LocalDateTime.now(), null);

        assertThatThrownBy(() -> service.recordTreated(999L, 1L, item))
                .isInstanceOf(TriageValidationException.class)
                .hasMessageContaining("Triage run not found");
    }
}