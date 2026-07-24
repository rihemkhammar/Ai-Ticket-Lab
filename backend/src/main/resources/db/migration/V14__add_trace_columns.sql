ALTER TABLE agent_run ADD COLUMN trace_id VARCHAR(100);
ALTER TABLE agent_run ADD COLUMN run_type VARCHAR(50);
ALTER TABLE agent_run ADD COLUMN duration_ms BIGINT;

ALTER TABLE agent_tool_call ADD COLUMN trace_id VARCHAR(100);
ALTER TABLE agent_tool_call ADD COLUMN duration_ms BIGINT;

ALTER TABLE agent_review_checkpoint ADD COLUMN trace_id VARCHAR(100);

CREATE INDEX agent_run_trace_id_idx ON agent_run(trace_id);