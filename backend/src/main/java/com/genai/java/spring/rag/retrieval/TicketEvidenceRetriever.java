package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import com.genai.java.spring.rag.embedding.EmbeddingService;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *  given a ticket, builds a retrieval query, embeds it, and returns
 * the top-K most relevant article chunks from semantic_chunk.
 */
@Slf4j
@Service
public class TicketEvidenceRetriever {

    /** Simple keyword -> preferred category map used for optional metadata filtering. */
    private static final Map<String, String> KEYWORD_TO_CATEGORY = Map.of(
            "motor", "MOTOR",
            "conveyor", "CONVEYOR",
            "pump", "PUMP",
            "sensor", "SENSOR"
    );

    private final EmbeddingService embeddingService;
    private final VectorChunkDao vectorChunkDao;

    @Value("${app.rag.top-k:3}")
    private int topK;

    public TicketEvidenceRetriever(EmbeddingService embeddingService, VectorChunkDao vectorChunkDao) {
        this.embeddingService = embeddingService;
        this.vectorChunkDao = vectorChunkDao;
    }

    public List<EvidenceChunkResponse> retrieve(Ticket ticket) {
        String query = buildQuery(ticket);
        float[] queryEmbedding = embeddingService.embed(query);

        // Fetch a slightly larger candidate pool so the optional category
        // preference below has something to reorder.
        List<VectorSearchRow> candidates = vectorChunkDao.search(queryEmbedding, Math.max(topK * 3, topK));

        String preferredCategory = detectPreferredCategory(query);

        List<VectorSearchRow> ranked = candidates.stream()
                .sorted(Comparator
                        .comparing((VectorSearchRow row) -> preferredCategory != null
                                && !preferredCategory.equalsIgnoreCase(row.category()))
                        .thenComparing(VectorSearchRow::distance))
                .limit(topK)
                .toList();

        List<EvidenceChunkResponse> evidence = ranked.stream()
                .map(row -> EvidenceChunkResponse.of(
                        row.articleId(), row.chunkIndex(), row.text(),
                        row.articleTitle(), row.category(),
                        toSimilarityScore(row.distance())))
                .toList();

        log.info("Retrieved {} evidence chunks for ticketId={} preferredCategory={}",
                evidence.size(), ticket.getId(), preferredCategory);
        return evidence;
    }

    private String buildQuery(Ticket ticket) {
        return (nullToEmpty(ticket.getTitle()) + ". " + nullToEmpty(ticket.getDescription())).trim();
    }

    private String detectPreferredCategory(String query) {
        String haystack = query.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : KEYWORD_TO_CATEGORY.entrySet()) {
            if (haystack.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** pgvector Euclidean distance -> a friendlier "the smaller, the better" 0..1 score for the UI. */
    private double toSimilarityScore(double distance) {
        return 1.0 / (1.0 + distance);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}