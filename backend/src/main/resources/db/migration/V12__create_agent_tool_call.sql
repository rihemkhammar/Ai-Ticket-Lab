CREATE TABLE agent_tool_call (
                                 id BIGSERIAL PRIMARY KEY,
                                 agent_run_id BIGINT REFERENCES agent_run(id) ON DELETE CASCADE,
                                 tool_name VARCHAR(100) NOT NULL,
                                 input_json JSONB,
                                 output_json JSONB,
                                 status VARCHAR(50) NOT NULL,
                                 error_message TEXT,
                                 started_at TIMESTAMP NOT NULL DEFAULT now(),
                                 completed_at TIMESTAMP
);
