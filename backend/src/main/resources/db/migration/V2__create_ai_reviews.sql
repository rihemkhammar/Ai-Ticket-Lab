CREATE TABLE ai_reviews (
                            id             BIGSERIAL    PRIMARY KEY,
                            ticket_id      BIGINT       NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                            triggered_by   UUID         NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
                            prompt_version VARCHAR(50)  NOT NULL,
                            model_name     VARCHAR(100) NOT NULL,
                            status         VARCHAR(50)  NOT NULL,
                            result         JSONB,
                            error_message  TEXT,
                            created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
                            CONSTRAINT chk_success_has_result CHECK (status != 'SUCCESS' OR result IS NOT NULL),
    CONSTRAINT chk_failed_has_error   CHECK (status != 'FAILED'  OR error_message IS NOT NULL)
);

CREATE INDEX idx_ai_reviews_ticket    ON ai_reviews(ticket_id);
CREATE INDEX idx_ai_reviews_triggered ON ai_reviews(triggered_by);
CREATE INDEX idx_ai_reviews_status    ON ai_reviews(status);