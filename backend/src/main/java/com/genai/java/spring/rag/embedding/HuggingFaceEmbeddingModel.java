package com.genai.java.spring.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom EmbeddingModel that calls HuggingFace's router (hf-inference provider)
 * directly, since it does NOT follow the OpenAI /v1/embeddings contract.
 */
public class HuggingFaceEmbeddingModel implements EmbeddingModel {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HuggingFaceEmbeddingModel(String baseUrl, String apiKey, String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            float[] vector = embedSingle(text);
            embeddings.add(new Embedding(vector, index++));
        }
        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
    }

    @Override
    public float[] embed(String text) {
        return embedSingle(text);
    }

    @Override
    public float[] embed(Document document) {
        return embedSingle(document.getFormattedContent());
    }

    private float[] embedSingle(String text) {
        Map<String, Object> body = Map.of(
                "inputs", text,
                "options", Map.of("wait_for_model", true)
        );

        String response = restClient.post()
                .uri("/hf-inference/models/{model}/pipeline/feature-extraction", model)                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            // hf-inference feature-extraction returns a flat array of floats,
            // or a nested array per-token depending on the model/pipeline.
            JsonNode vectorNode = root.isArray() && root.get(0).isArray() ? root.get(0) : root;
            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vectorNode.size(); i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse HuggingFace embedding response: " + response, e);
        }
    }
}