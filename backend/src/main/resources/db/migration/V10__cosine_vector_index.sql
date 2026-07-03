CREATE INDEX IF NOT EXISTS semantic_chunk_embedding_cosine_idx
    ON semantic_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 10);