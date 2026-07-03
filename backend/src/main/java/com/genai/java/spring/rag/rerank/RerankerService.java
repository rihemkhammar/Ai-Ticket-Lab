package com.genai.java.spring.rag.rerank;

import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Slf4j
@Service
public class RerankerService {

    private final RestClient restClient;
    private final String model;

    public RerankerService(
            @Value("${app.rag.huggingface.base-url:https://router.huggingface.co}") String baseUrl,
            @Value("${app.rag.huggingface.api-key}") String apiKey,
            @Value("${app.rag.reranker.model:cross-encoder/ms-marco-MiniLM-L-6-v2}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public List<VectorSearchRow> rerank(String query, List<VectorSearchRow> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        try {
            List<String> texts = candidates.stream().map(VectorSearchRow::text).toList();

            Map<String, Object> body = Map.of(
                    "inputs", Map.of("source_sentence", query, "sentences", texts),
                    "options", Map.of("wait_for_model", true)
            );

            List<Double> scores = restClient.post()
                    .uri("/hf-inference/models/{model}/pipeline/sentence-similarity", model)
                    .body(body)
                    .retrieve()
                    .body(List.class);

            List<ScoredCandidate> scored = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                double score = i < scores.size() ? scores.get(i) : 0.0;
                scored.add(new ScoredCandidate(candidates.get(i), score));
            }

            return scored.stream()
                    .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                    .limit(topK)
                    .map(ScoredCandidate::row)
                    .toList();

        } catch (Exception e) {
            log.warn("Reranking failed, falling back to hybrid search order: {}", e.getMessage());
            return candidates.stream().limit(topK).toList();
        }
    }

    private record ScoredCandidate(VectorSearchRow row, double score) {}
}