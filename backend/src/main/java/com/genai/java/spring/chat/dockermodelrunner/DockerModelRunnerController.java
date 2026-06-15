package com.genai.java.spring.chat.dockermodelrunner;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/docker-model-runner")
public class DockerModelRunnerController {

    private final String SYSTEM_PROMPT = "You are a helpful assistant that generates professional LinkedIn posts about technical subjects. " +
            "The posts are engaging, informative, and tailored to a professional audience. " +
            "Friendly and approachable tone while maintaining professionalism.";

    private final ChatClient chatClient;

    public DockerModelRunnerController(@Qualifier("dockerRunnerChatClient") ChatClient chatClient){

        this.chatClient = chatClient;
    }
    @PostMapping("/Linkedin-post-generator")
    public String generateLinkedinPost(@RequestBody String message){
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}

