package com.genai.java.spring.rag.chunk;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Component
public class VectorChunkDao {

    private final JdbcTemplate jdbcTemplate;

    public VectorChunkDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateEmbedding(Long chunkId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE semantic_chunk SET embedding = ?::vector WHERE id = ?",
                toPgVector(embedding), chunkId);
    }

    /**
     * Top-K nearest chunks to the given query embedding, using pgvector's
     * "<=>" (COSINE distance) operator instead of "<->" (Euclidean).
     * Cosine similarity is the right choice for sentence-embedding models.
     * Lower distance = more similar (0 = identical direction, 2 = opposite).
     */
    public List<VectorSearchRow> search(float[] queryEmbedding, int topK) {
        String sql = """
                SELECT sc.id, sc.article_id, sc.chunk_index, sc.text,
                       ka.title, ka.category,
                       sc.embedding <=> ?::vector AS distance
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

    /**
     * Top-K chunks by keyword relevance (Postgres full-text search / ts_rank).
     */
    public List<VectorSearchRow> keywordSearch(String queryText, int topK) {
        String sql = """
                SELECT sc.id, sc.article_id, sc.chunk_index, sc.text,
                       ka.title, ka.category,
                       ts_rank(sc.text_search, plainto_tsquery('english', ?)) AS rank
                FROM semantic_chunk sc
                JOIN knowledge_article ka ON ka.id = sc.article_id
                WHERE sc.text_search @@ plainto_tsquery('english', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new VectorSearchRow(
                rs.getLong("id"),
                rs.getLong("article_id"),
                rs.getInt("chunk_index"),
                rs.getString("text"),
                rs.getString("title"),
                rs.getString("category"),
                1.0 - rs.getDouble("rank")
        ), queryText, queryText, topK);
    }

    /**
     * Fetches the chunks immediately before and after the given chunk
     * within the same article ("neighbor stitching").
     */
    public List<VectorSearchRow> findNeighbors(Long articleId, int chunkIndex) {
        String sql = """
                SELECT sc.id, sc.article_id, sc.chunk_index, sc.text,
                       ka.title, ka.category
                FROM semantic_chunk sc
                JOIN knowledge_article ka ON ka.id = sc.article_id
                WHERE sc.article_id = ?
                  AND sc.chunk_index IN (?, ?)
                ORDER BY sc.chunk_index ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new VectorSearchRow(
                rs.getLong("id"),
                rs.getLong("article_id"),
                rs.getInt("chunk_index"),
                rs.getString("text"),
                rs.getString("title"),
                rs.getString("category"),
                0.0
        ), articleId, chunkIndex - 1, chunkIndex + 1);
    }

    private PGobject toPgVector(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
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