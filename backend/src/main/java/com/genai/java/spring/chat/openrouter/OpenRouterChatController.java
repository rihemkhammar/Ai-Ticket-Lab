package com.genai.java.spring.chat.openrouter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openrouter/chat")
public class OpenRouterChatController {

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant that summarizes any given content. " +
                    "Ensure the summary is concise, informative, and captures the key points. " +
                    "Use a friendly and approachable tone while maintaining professionalism. " +
                    "Do not answer anything other than summarization. If the request is not about summarization, " +
                    "respond with: \"I can only help with summarization tasks.\"";

    private final ChatClient chatClient;
    private final OpenRouterService openRouterService;

    public OpenRouterChatController(ChatClient chatClient, OpenRouterService openRouterService) {
        this.chatClient = chatClient;
        this.openRouterService = openRouterService;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    @PostMapping("/summarize-meeting-notes")
    public String summarizeMeetingNotes(@RequestBody String meetingNotes) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}")
                        .param("meetingNotes", meetingNotes))
                .call()
                .content();
    }

    @PostMapping("/summarize-with-openrouter-java-client")
    public String summarizeWithOpenRouterJavaClient(@RequestBody String message) throws OpenRouterChatException {
        return openRouterService.chat(message);
    }
}