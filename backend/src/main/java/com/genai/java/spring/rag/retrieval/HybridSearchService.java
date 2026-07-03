package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import com.genai.java.spring.rag.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Combines pgvector semantic search and Postgres full-text keyword search
 * using Reciprocal Rank Fusion (RRF).
 */
@Service
public class HybridSearchService {

    private static final int RRF_K = 60;

    private final EmbeddingService embeddingService;
    private final VectorChunkDao vectorChunkDao;

    @Value("${app.rag.hybrid.candidate-pool:20}")
    private int candidatePoolSize;

    public HybridSearchService(EmbeddingService embeddingService, VectorChunkDao vectorChunkDao) {
        this.embeddingService = embeddingService;
        this.vectorChunkDao = vectorChunkDao;
    }

    public List<VectorSearchRow> search(String queryText, int fusedTopK) {
        float[] queryEmbedding = embeddingService.embed(queryText);

        List<VectorSearchRow> vectorResults = vectorChunkDao.search(queryEmbedding, candidatePoolSize);
        List<VectorSearchRow> keywordResults = vectorChunkDao.keywordSearch(queryText, candidatePoolSize);

        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, VectorSearchRow> rowsById = new HashMap<>();

        accumulateRrf(vectorResults, rrfScores, rowsById);
        accumulateRrf(keywordResults, rrfScores, rowsById);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(fusedTopK)
                .map(e -> rowsById.get(e.getKey()))
                .toList();
    }

    private void accumulateRrf(List<VectorSearchRow> results,
                               Map<Long, Double> rrfScores,
                               Map<Long, VectorSearchRow> rowsById) {
        for (int rank = 0; rank < results.size(); rank++) {
            VectorSearchRow row = results.get(rank);
            double contribution = 1.0 / (RRF_K + rank + 1);
            rrfScores.merge(row.chunkId(), contribution, Double::sum);
            rowsById.putIfAbsent(row.chunkId(), row);
        }
    }
}