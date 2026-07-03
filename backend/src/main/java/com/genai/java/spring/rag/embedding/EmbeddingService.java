package com.genai.java.spring.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring AI's {@link EmbeddingModel}.
 *
 * Logs the actual dimension returned by the model on first call,
 * and validates it matches the configured embedding-dimensions.
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final int expectedDimensions;
    private volatile boolean dimensionLogged = false;

    public EmbeddingService(EmbeddingModel embeddingModel,
                            @Value("${app.rag.embedding-dimensions:384}") int expectedDimensions) {
        this.embeddingModel = embeddingModel;
        this.expectedDimensions = expectedDimensions;
    }

    /** Embeds a single piece of text (one article chunk, or one ticket query). */
    public float[] embed(String text) {
        float[] vector = embeddingModel.embed(text);

        // Log actual dimension on first call to catch mismatches early
        if (!dimensionLogged) {
            dimensionLogged = true;
            log.info("[EmbeddingService] Model returned {} dimensions (expected {} from config)",
                    vector.length, expectedDimensions);
            if (vector.length != expectedDimensions) {
                log.error("[EmbeddingService] DIMENSION MISMATCH: model returns {} but DB column is vector({})." +
                                " Fix: either set app.rag.embedding-dimensions={} in application.yml" +
                                " AND update V8 SQL to vector({}), then recreate DB." +
                                " OR configure the embedding model to return {} dimensions.",
                        vector.length, expectedDimensions,
                        vector.length, vector.length, expectedDimensions);
            }
        }

        return vector;
    }
}