package com.genai.java.spring.hitl.dto;

import com.genai.java.spring.hitl.HumanReviewDecision;
import com.genai.java.spring.hitl.ReviewCheckpointStatus;

import java.time.LocalDateTime;

/**
 * Lightweight, DB-facing snapshot of a single checkpoint row.
 * Used internally by AgentReviewCheckpointService to move data between the
 * repository and the higher-level response DTOs (HitlReviewResponse /
 * HumanReviewDecisionResponse) without leaking the JPA entity.
 */
public class CheckpointSnapshot {

    private Long checkpointId;
    private Long agentRunId;
    private Long ticketId;
    private Integer checkpointNumber;
    private ReviewCheckpointStatus status;
    private String draftJson;
    private String serializedPromptJson;
    private String toolTraceSnapshotJson;
    private HumanReviewDecision humanDecision;
    private String humanComment;
    private String finalReviewedResultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public static CheckpointSnapshot from(com.genai.java.spring.hitl.AgentReviewCheckpoint entity) {
        CheckpointSnapshot s = new CheckpointSnapshot();
        s.checkpointId = entity.getId();
        s.agentRunId = entity.getAgentRunId();
        s.ticketId = entity.getTicketId();
        s.checkpointNumber = entity.getCheckpointNumber();
        s.status = entity.getStatus();
        s.draftJson = entity.getDraftJson();
        s.serializedPromptJson = entity.getSerializedPromptJson();
        s.toolTraceSnapshotJson = entity.getToolTraceSnapshotJson();
        s.humanDecision = entity.getHumanDecision();
        s.humanComment = entity.getHumanComment();
        s.finalReviewedResultJson = entity.getFinalReviewedResultJson();
        s.createdAt = entity.getCreatedAt();
        s.updatedAt = entity.getUpdatedAt();
        s.completedAt = entity.getCompletedAt();
        return s;
    }

    public Long getCheckpointId()                              { return checkpointId; }
    public Long getAgentRunId()                                { return agentRunId; }
    public Long getTicketId()                                  { return ticketId; }
    public Integer getCheckpointNumber()                       { return checkpointNumber; }
    public ReviewCheckpointStatus getStatus()                  { return status; }
    public String getDraftJson()                               { return draftJson; }
    public String getSerializedPromptJson()                    { return serializedPromptJson; }
    public String getToolTraceSnapshotJson()                   { return toolTraceSnapshotJson; }
    public HumanReviewDecision getHumanDecision()               { return humanDecision; }
    public String getHumanComment()                            { return humanComment; }
    public String getFinalReviewedResultJson()                 { return finalReviewedResultJson; }
    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public LocalDateTime getUpdatedAt()                        { return updatedAt; }
    public LocalDateTime getCompletedAt()                      { return completedAt; }
}
