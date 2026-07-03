-- Adds keyword (full-text) search capability alongside the existing
-- pgvector semantic search, to support hybrid retrieval.

ALTER TABLE semantic_chunk
    ADD COLUMN text_search tsvector
        GENERATED ALWAYS AS (to_tsvector('english', text)) STORED;

CREATE INDEX semantic_chunk_text_search_idx ON semantic_chunk USING GIN (text_search);