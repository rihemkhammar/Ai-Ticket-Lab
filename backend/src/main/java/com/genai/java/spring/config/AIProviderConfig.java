package com.genai.java.spring.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.huggingface.HuggingfaceChatModel;

import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIProviderConfig {

    @Bean("openAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("vertexAIChatClient")
    ChatClient vertexAIChatClient(VertexAiGeminiChatModel vertexAiGeminiChatModel) {
        return ChatClient.builder(vertexAiGeminiChatModel).build();
    }
    @Bean("huggingFaceChatClient")
    ChatClient huggingFaceChatClient(HuggingfaceChatModel huggingFaceChatModel) {
        return ChatClient.builder(huggingFaceChatModel).build();
    }

    @Bean("openRouterChatClient")
    ChatClient openRouterChatClient(
            @Value("${spring.ai.openrouter.api-key}") String apiKey,
            @Value("${spring.ai.openrouter.model}") String model) {

        // Spring AI 1.1.7 — syntaxe correcte
        OpenAiApi openRouterApi = OpenAiApi.builder()
                .baseUrl("https://openrouter.ai/api")
                .apiKey(apiKey)
                .build();


        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openRouterApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }
    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel){
        return ChatClient.builder(ollamaChatModel).build();

    }

}