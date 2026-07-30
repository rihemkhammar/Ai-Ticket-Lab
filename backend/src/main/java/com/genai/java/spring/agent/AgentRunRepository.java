package com.genai.java.spring.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    // used to redisplay the last agent investigation for a
    // ticket without re-running it, e.g. when the user navigates back.
    Optional<AgentRun> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);

    // lookup by trace ID for the optional GET /api/ai-traces/{traceId} endpoint.
    Optional<AgentRun> findFirstByTraceId(String traceId);
}