package com.genai.java.spring.hitl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentReviewCheckpointRepository extends JpaRepository<AgentReviewCheckpoint, Long> {

    List<AgentReviewCheckpoint> findByAgentRunIdOrderByCheckpointNumberAsc(Long agentRunId);

    Optional<AgentReviewCheckpoint> findFirstByAgentRunIdOrderByCheckpointNumberDesc(Long agentRunId);

    Optional<AgentReviewCheckpoint> findFirstByAgentRunIdAndStatus(Long agentRunId, ReviewCheckpointStatus status);

    /**
     * Latest checkpoint for a ticket, regardless of which agent_run it belongs to.
     * Used to reload the HITL review after a refresh even when a newer, non-HITL
     * agent_run has since been created for the same ticket (S5-G03).
     */
    Optional<AgentReviewCheckpoint> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);
}