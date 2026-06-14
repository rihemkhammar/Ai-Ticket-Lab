package com.genai.java.spring.chat.openai;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    //private static final String OPENAI_API_URL= "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_API_URL= "https://api.groq.com/openai/v1/chat/completions";

    //private static final String  OPENAI_MODEL= "gpt-4o";
    private static final String  OPENAI_MODEL= "llama-3.3-70b-versatile";
    private static final String  CONTENT_TYPE ="application/json";
    private static final String SYSTEM_PROMPT ="You are a helpful assistant that summarizes any given content. " +
            "Ensure the summary is concise, informative, and captures the key points. " +
            "Use a friendly and approachable tone while maintaining professionalism. " +
            "Do not answer anything other than the summarization. If the question is not about summarization, respond with 'I can";
    private final ObjectMapper objectMapper;

    public OpenAIService(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    public String chat(String prompt) throws OpenAIChatExecption{
        //return "Reponse from openAI API";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = getRequest(prompt);
            var response = httpClient.execute(request, resp -> EntityUtils.toString(resp.getEntity()));

            return parseResponse(response);
        }catch (IOException e){
            throw new OpenAIChatExecption("Could not call OpenAI API using java client",e);
        }
    }

    private HttpPost getRequest(String prompt) throws JsonProcessingException {
        var request = new HttpPost(OPENAI_API_URL);
        var openAIApiKey = System.getenv(OPENAI_API_KEY);
        request.addHeader(HttpHeaders.CONTENT_TYPE,CONTENT_TYPE );
        request.addHeader(HttpHeaders.AUTHORIZATION,"Bearer " + openAIApiKey);
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", prompt
        );
        Map <String, Object> systemMassage= Map.of(
                "role", "system",
                "content", SYSTEM_PROMPT
        );
        Map <String, Object> body = Map.of(
                "model", OPENAI_MODEL,
                //"messages", List.of(userMessage, systemMassage)
                "messages", List.of(systemMassage, userMessage)
        );
        String requestBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(requestBody));
        return request;


    }
    private String parseResponse(String response) throws JsonProcessingException {
        Map<String, Object> openAIResponse = objectMapper.readValue(response, Map.class);
        return openAIResponse.get("choices").toString();
    }
}
/*private String parseResponse(String response) throws JsonProcessingException {
    Map<String, Object> openAIResponse = objectMapper.readValue(response, Map.class);
    if (openAIResponse.containsKey("error")) {
        throw new RuntimeException("API error: " + openAIResponse.get("error"));
    }
    List<Map<String, Object>> choices = (List<Map<String, Object>>) openAIResponse.get("choices");
    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
    return (String) message.get("content");
}*/