package com.genai.java.spring.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentToolCallRepository extends JpaRepository<AgentToolCall, Long> {
    List<AgentToolCall> findByAgentRunIdOrderByStartedAtAsc(Long agentRunId);
}
