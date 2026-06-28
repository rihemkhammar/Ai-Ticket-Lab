package com.genai.java.spring.rag.chunk;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

/**
 * Handles everything related to the pgvector "embedding" column of
 * semantic_chunk, since this is the one column not mapped by JPA
 * (see {@link SemanticChunk}).
 */
@Component
public class VectorChunkDao {

    private final JdbcTemplate jdbcTemplate;

    public VectorChunkDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Writes the embedding for a chunk that was already persisted via JPA. */
    public void updateEmbedding(Long chunkId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE semantic_chunk SET embedding = ?::vector WHERE id = ?",
                toPgVector(embedding), chunkId);
    }

    /**
     * Top-K nearest chunks to the given query embedding, using pgvector's
     * "<->" (Euclidean distance) operator. Lower distance = more similar;
     * we expose it back as a 0..1-ish similarity score for the frontend.
     */
    public List<VectorSearchRow> search(float[] queryEmbedding, int topK) {
        String sql = """
                SELECT sc.id, sc.article_id, sc.chunk_index, sc.text,
                       ka.title, ka.category,
                       sc.embedding <-> ?::vector AS distance
                FROM semantic_chunk sc
                JOIN knowledge_article ka ON ka.id = sc.article_id
                WHERE sc.embedding IS NOT NULL
                ORDER BY distance ASC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new VectorSearchRow(
                rs.getLong("id"),
                rs.getLong("article_id"),
                rs.getInt("chunk_index"),
                rs.getString("text"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getDouble("distance")
        ), toPgVector(queryEmbedding), topK);
    }

    private PGobject toPgVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');

        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        try {
            pgObject.setValue(sb.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to build pgvector literal", e);
        }
        return pgObject;
    }

    public record VectorSearchRow(
            Long chunkId,
            Long articleId,
            int chunkIndex,
            String text,
            String articleTitle,
            String category,
            double distance
    ) {}
}