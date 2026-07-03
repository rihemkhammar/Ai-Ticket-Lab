package com.genai.java.spring.config;

import com.genai.java.spring.rag.embedding.HuggingFaceEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HuggingFaceEmbeddingConfig {

    @Value("${app.rag.huggingface.base-url:https://router.huggingface.co}")
    private String baseUrl;

    @Value("${app.rag.huggingface.api-key}")
    private String apiKey;

    @Value("${app.rag.huggingface.model:sentence-transformers/all-MiniLM-L6-v2}")
    private String model;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new HuggingFaceEmbeddingModel(baseUrl, apiKey, model);
    }
}