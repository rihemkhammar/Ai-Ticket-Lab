CREATE TABLE agent_review_checkpoint (
                                         id BIGSERIAL PRIMARY KEY,
                                         agent_run_id BIGINT NOT NULL REFERENCES agent_run(id) ON DELETE CASCADE,
                                         ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                                         checkpoint_number INTEGER NOT NULL,
                                         status VARCHAR(50) NOT NULL,
                                         draft_json JSONB NOT NULL,
                                         serialized_prompt_json JSONB,
                                         tool_trace_snapshot_json JSONB,
                                         human_decision VARCHAR(50),
                                         human_comment TEXT,
                                         final_reviewed_result_json JSONB,
                                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                                         updated_at TIMESTAMP NOT NULL DEFAULT now(),
                                         completed_at TIMESTAMP
);

CREATE INDEX agent_review_checkpoint_run_idx
    ON agent_review_checkpoint(agent_run_id);