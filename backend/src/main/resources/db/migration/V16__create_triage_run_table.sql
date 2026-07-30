CREATE TABLE triage_run (
                            id                    BIGSERIAL    PRIMARY KEY,
                            status                VARCHAR(50)  NOT NULL,
                            prompt_version        VARCHAR(100) NOT NULL,
                            model_name            VARCHAR(100) NOT NULL,
                            ticket_queue          JSONB        NOT NULL,
                            classifications_json  JSONB,
                            treated_json          JSONB        NOT NULL DEFAULT '[]'::jsonb,
                            error_message         TEXT,
                            created_at            TIMESTAMP    NOT NULL DEFAULT now(),
                            updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
                            completed_at          TIMESTAMP
);

CREATE INDEX triage_run_status_idx ON triage_run(status);