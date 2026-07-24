package com.genai.java.spring.observability.dto;

import java.time.LocalDateTime;

/**
 * Checkpoint/human-decision entry within an AgentRunTraceResponse .
 */
public class AgentCheckpointTraceResponse {

    private Long checkpointId;
    private Integer checkpointNumber;
    private String status;
    private String humanDecision;
    private String humanComment;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public Long getCheckpointId()                { return checkpointId; }
    public void setCheckpointId(Long v)          { this.checkpointId = v; }

    public Integer getCheckpointNumber()         { return checkpointNumber; }
    public void setCheckpointNumber(Integer v)   { this.checkpointNumber = v; }

    public String getStatus()                     { return status; }
    public void setStatus(String v)               { this.status = v; }

    public String getHumanDecision()              { return humanDecision; }
    public void setHumanDecision(String v)        { this.humanDecision = v; }

    public String getHumanComment()               { return humanComment; }
    public void setHumanComment(String v)         { this.humanComment = v; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime v)     { this.createdAt = v; }

    public LocalDateTime getCompletedAt()         { return completedAt; }
    public void setCompletedAt(LocalDateTime v)   { this.completedAt = v; }
}