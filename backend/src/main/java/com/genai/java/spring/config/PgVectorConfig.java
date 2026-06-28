package com.genai.java.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
public class PgVectorConfig {

    @Value("${app.rag.embedding-dimensions:1536}")
    private int embeddingDimensions;

    @Bean
    public EmbeddingDimensions embeddingDimensions() {
        return new EmbeddingDimensions(embeddingDimensions);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEmbeddingModelOnStartup() {
        log.info("pgvector configured for embedding dimension={} (from app.rag.embedding-dimensions). "
                        + "Make sure this matches the 'vector(...)' size in V8__create_semantic_chunk.sql "
                        + "AND the dimensions actually returned by spring.ai.openai.embedding.options.model.",
                embeddingDimensions);
    }

    public record EmbeddingDimensions(int value) {}
}