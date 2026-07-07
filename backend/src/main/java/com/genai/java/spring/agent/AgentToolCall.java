package com.genai.java.spring.agent;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_tool_call")
public class AgentToolCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_run_id", nullable = false)
    private Long agentRunId;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "input_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentToolCallStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId()                          { return id; }

    public Long getAgentRunId()                  { return agentRunId; }
    public void setAgentRunId(Long v)            { this.agentRunId = v; }

    public String getToolName()                  { return toolName; }
    public void setToolName(String v)            { this.toolName = v; }

    public String getInputJson()                 { return inputJson; }
    public void setInputJson(String v)           { this.inputJson = v; }

    public String getOutputJson()                { return outputJson; }
    public void setOutputJson(String v)          { this.outputJson = v; }

    public AgentToolCallStatus getStatus()        { return status; }
    public void setStatus(AgentToolCallStatus v)  { this.status = v; }

    public String getErrorMessage()              { return errorMessage; }
    public void setErrorMessage(String v)        { this.errorMessage = v; }

    public LocalDateTime getStartedAt()          { return startedAt; }
    public void setStartedAt(LocalDateTime v)    { this.startedAt = v; }

    public LocalDateTime getCompletedAt()        { return completedAt; }
    public void setCompletedAt(LocalDateTime v)  { this.completedAt = v; }
}
