package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import com.genai.java.spring.rag.rerank.RerankerService;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class TicketEvidenceRetriever {

    private final HybridSearchService hybridSearchService;
    private final RerankerService rerankerService;
    private final NeighborStitchingService neighborStitchingService;

    @Value("${app.rag.top-k:3}")
    private int topK;

    @Value("${app.rag.hybrid.fused-pool:10}")
    private int fusedPoolSize;

    public TicketEvidenceRetriever(HybridSearchService hybridSearchService,
                                   RerankerService rerankerService,
                                   NeighborStitchingService neighborStitchingService) {
        this.hybridSearchService = hybridSearchService;
        this.rerankerService = rerankerService;
        this.neighborStitchingService = neighborStitchingService;
    }

    public List<EvidenceChunkResponse> retrieve(Ticket ticket) {

            String query = buildQuery(ticket);

            List<VectorSearchRow> fused = hybridSearchService.search(query, fusedPoolSize);
            List<VectorSearchRow> reranked = rerankerService.rerank(query, fused, topK);

            Set<String> usedIndexes = new HashSet<>(); // ← nouveau : suivi global pour ce ticket

            List<EvidenceChunkResponse> evidence = reranked.stream()
                    .map(row -> {
                        EvidenceChunkResponse dto = EvidenceChunkResponse.of(
                                row.articleId(), row.chunkIndex(), row.text(),
                                row.articleTitle(), row.category(),
                                toSimilarityScore(row.distance()));
                        dto.setExpandedText(neighborStitchingService.stitch(row, usedIndexes)); // ← passe le Set
                        return dto;
                    })
                    .toList();
        log.info("Retrieved {} evidence chunks for ticketId={} (hybrid+rerank+neighbor-stitching)",
                evidence.size(), ticket.getId());
        return evidence;
    }

    private String buildQuery(Ticket ticket) {
        return (nullToEmpty(ticket.getTitle()) + ". " + nullToEmpty(ticket.getDescription())).trim();
    }

    private double toSimilarityScore(double distance) {
        return 1.0 - (distance / 2.0);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}