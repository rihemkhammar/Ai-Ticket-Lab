package com.genai.java.spring.hitl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentReviewCheckpointRepository extends JpaRepository<AgentReviewCheckpoint, Long> {

    List<AgentReviewCheckpoint> findByAgentRunIdOrderByCheckpointNumberAsc(Long agentRunId);

    Optional<AgentReviewCheckpoint> findFirstByAgentRunIdOrderByCheckpointNumberDesc(Long agentRunId);

    Optional<AgentReviewCheckpoint> findFirstByAgentRunIdAndStatus(Long agentRunId, ReviewCheckpointStatus status);
}
