
CREATE TABLE semantic_chunk (
                                id            BIGSERIAL PRIMARY KEY,
                                article_id    BIGINT    NOT NULL REFERENCES knowledge_article(id) ON DELETE CASCADE,
                                chunk_index   INTEGER   NOT NULL,
                                text          TEXT      NOT NULL,
                                embedding vector(384),
                                metadata_json JSONB,
                                created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX semantic_chunk_article_idx ON semantic_chunk(article_id);

