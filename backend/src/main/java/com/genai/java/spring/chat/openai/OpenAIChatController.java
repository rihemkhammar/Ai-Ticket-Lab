package com.genai.java.spring.chat.openai;


import org.springframework.ai.chat.client.ChatClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api/openai/chat")
public class OpenAIChatController {

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant that summarizes any given content. " +
                    "Ensure the summary is concise, informative, and captures the key points. " +
                    "Use a friendly and approachable tone while maintaining professionalism. " +
                    "Do not answer anything other than summarization. If the request is not about summarization, " +
                    "respond with: \"I can only help with summarization tasks.\"";

    private final ChatClient chatClient;
    private final OpenAIService openAIService;

    public OpenAIChatController(@Qualifier("openAIChatClient") ChatClient chatClient, OpenAIService openAIService){
        this.chatClient = chatClient;
        this.openAIService = openAIService;
    }
    @PostMapping("/summarize")
    public String sumarize(@RequestBody String message){
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();

    }
    @PostMapping(value = "/summarize-with-streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sumarizeWithStreaming(@RequestBody String message){
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .content();

    }
    @PostMapping("/summarize-meeting-notes")
    public String summarizeMeetingNotes(@RequestBody String meetingNotes){

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user( u -> u.text("Can you summarize the following meeting notes: {meetingNotes}" +
                        " Use the format as described in the following example while doing the summarization:" +
                        " Input: In today's sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on ent" +
                        " A proposal was made to expand into two new regions. Marketing suggested aligning campaigns with sales objectives to imp" +
                        " Output:" +
                        " Action Items:" +
                        "* Focus on enterprise clients and partnerships." +
                        "* Explore expansion into two new regions." +
                        "* Align marketing campaigns with sales objectives." +
                        " Decisions:" +
                        "* Enterprise clients prioritized for Q3." +
                        "* Marketing and sales to work jointly on lead conversion.")

                        .param("meetingNotes", meetingNotes))
                .call()
                .content();

    }
    @PostMapping("/summarize-with-openai-java-client")
    public String summerizeWithOpenJavaClient(@RequestBody String message) throws OpenAIChatExecption {
        return openAIService.chat(message);
    }




}
