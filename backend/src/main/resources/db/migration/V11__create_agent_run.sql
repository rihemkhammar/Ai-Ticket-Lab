CREATE TABLE agent_run (
                           id BIGSERIAL PRIMARY KEY,
                           ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                           prompt_version VARCHAR(100) NOT NULL,
                           model_name VARCHAR(100) NOT NULL,
                           status VARCHAR(50) NOT NULL,
                           result_json JSONB,
                           error_message TEXT,
                           created_at TIMESTAMP NOT NULL DEFAULT now(),
                           completed_at TIMESTAMP
);
