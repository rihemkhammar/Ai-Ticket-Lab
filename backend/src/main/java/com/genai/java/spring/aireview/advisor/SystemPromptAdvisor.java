package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.prompt.TicketReviewPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**

 * Construit le system prompt + user prompt via TicketReviewPromptBuilder
 * et fixe la version de prompt utilisée pour cette review.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemPromptAdvisor implements AiReviewAdvisor {

    private final TicketReviewPromptBuilder promptBuilder;

    @Override
    public Stage getStage() {
        return Stage.PRE_CALL;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void advise(AiReviewContext context) {
        context.setSystemPrompt(promptBuilder.buildSystemPrompt());
        context.setUserPrompt(promptBuilder.buildUserPrompt(
                context.getTicket().getTitle(),
                context.getTicket().getDescription()));
        context.setPromptVersion(promptBuilder.version());

        log.info("[SystemPromptAdvisor] ticketId={} promptVersion={}",
                context.getTicket().getId(), context.getPromptVersion());
    }
}