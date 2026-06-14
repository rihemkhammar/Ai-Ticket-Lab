package com.genai.java.spring.chat.openrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final String OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY";
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_MODEL = "meta-llama/llama-3.3-70b-instruct"; // ou tout autre modèle
    private static final String CONTENT_TYPE = "application/json";

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant that summarizes any given content. " +
                    "Ensure the summary is concise, informative, and captures the key points. " +
                    "Use a friendly and approachable tone while maintaining professionalism. " +
                    "Do not answer anything other than summarization. If the request is not about summarization, " +
                    "respond with: \"I can only help with summarization tasks.\"";

    private final ObjectMapper objectMapper;

    public OpenRouterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chat(String prompt) throws OpenRouterChatException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = buildRequest(prompt);
            var response = httpClient.execute(request, resp -> EntityUtils.toString(resp.getEntity()));
            return parseResponse(response);
        } catch (IOException e) {
            throw new OpenRouterChatException("Could not call OpenRouter API", e);
        }
    }

    private HttpPost buildRequest(String prompt) throws JsonProcessingException {
        var request = new HttpPost(OPENROUTER_API_URL);
        String apiKey = System.getenv(OPENROUTER_API_KEY_ENV);

        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        // Headers recommandés par OpenRouter
        request.addHeader("HTTP-Referer", "http://localhost:8081");
        request.addHeader("X-Title", "genai-java-spring");

        Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> body = Map.of(
                "model", OPENROUTER_MODEL,
                "messages", List.of(systemMessage, userMessage)
        );

        request.setEntity(new StringEntity(objectMapper.writeValueAsString(body)));
        return request;
    }

    private String parseResponse(String response) throws JsonProcessingException {
        Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException("OpenRouter error: " + parsed.get("error"));
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}