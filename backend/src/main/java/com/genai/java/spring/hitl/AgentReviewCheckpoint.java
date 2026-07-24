package com.genai.java.spring.hitl;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A persisted human-review checkpoint.
 *
 * One row = one "pause point" of an agent run: the AI draft that was
 * produced, the serialized prompt/state context used to build it, a
 * snapshot of the tool-call trace, and (once decided) the human's
 * decision + the finalized result.
 *
 * Persisted in Postgres so a WAITING_FOR_HUMAN run survives a page
 * refresh or a backend restart .
 */
@Entity
@Table(name = "agent_review_checkpoint")
public class AgentReviewCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_run_id", nullable = false)
    private Long agentRunId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "checkpoint_number", nullable = false)
    private Integer checkpointNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewCheckpointStatus status;

    @Column(name = "draft_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String draftJson;

    @Column(name = "serialized_prompt_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String serializedPromptJson;

    @Column(name = "tool_trace_snapshot_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String toolTraceSnapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "human_decision")
    private HumanReviewDecision humanDecision;

    @Column(name = "human_comment", columnDefinition = "TEXT")
    private String humanComment;

    @Column(name = "final_reviewed_result_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String finalReviewedResultJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    //  observability / trace field
    @Column(name = "trace_id")
    private String traceId;

    public Long getId()                                            { return id; }

    public Long getAgentRunId()                                    { return agentRunId; }
    public void setAgentRunId(Long v)                              { this.agentRunId = v; }

    public Long getTicketId()                                      { return ticketId; }
    public void setTicketId(Long v)                                { this.ticketId = v; }

    public Integer getCheckpointNumber()                           { return checkpointNumber; }
    public void setCheckpointNumber(Integer v)                     { this.checkpointNumber = v; }

    public ReviewCheckpointStatus getStatus()                      { return status; }
    public void setStatus(ReviewCheckpointStatus v)                { this.status = v; }

    public String getDraftJson()                                   { return draftJson; }
    public void setDraftJson(String v)                             { this.draftJson = v; }

    public String getSerializedPromptJson()                        { return serializedPromptJson; }
    public void setSerializedPromptJson(String v)                  { this.serializedPromptJson = v; }

    public String getToolTraceSnapshotJson()                       { return toolTraceSnapshotJson; }
    public void setToolTraceSnapshotJson(String v)                 { this.toolTraceSnapshotJson = v; }

    public HumanReviewDecision getHumanDecision()                  { return humanDecision; }
    public void setHumanDecision(HumanReviewDecision v)            { this.humanDecision = v; }

    public String getHumanComment()                                { return humanComment; }
    public void setHumanComment(String v)                          { this.humanComment = v; }

    public String getFinalReviewedResultJson()                     { return finalReviewedResultJson; }
    public void setFinalReviewedResultJson(String v)               { this.finalReviewedResultJson = v; }

    public LocalDateTime getCreatedAt()                            { return createdAt; }
    public void setCreatedAt(LocalDateTime v)                      { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()                            { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)                      { this.updatedAt = v; }

    public LocalDateTime getCompletedAt()                          { return completedAt; }
    public void setCompletedAt(LocalDateTime v)                    { this.completedAt = v; }

    public String getTraceId()                                     { return traceId; }
    public void setTraceId(String v)                               { this.traceId = v; }
}