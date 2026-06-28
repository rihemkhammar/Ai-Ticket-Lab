package com.genai.java.spring.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring AI's {@link EmbeddingModel}.
 *
 * Kept as its own service (instead of calling EmbeddingModel directly from
 * indexing/retrieval code) so the embedding model can be swapped later
 * without touching chunking, indexing, or retrieval logic.
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Embeds a single piece of text (one article chunk, or one ticket query). */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}