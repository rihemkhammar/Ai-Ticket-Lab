package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.shared.advisor.PromptInjectionGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ne bloque jamais l'appel : le ticket reste envoyé au modèle (qui doit
 * le traiter comme donnée, pas comme instruction — cf. system prompt).
 * La détection elle-même vit désormais dans PromptInjectionGuard (partagé
 * avec le module agent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptInjectionDefenseAdvisor implements AiReviewAdvisor {

    private final PromptInjectionGuard promptInjectionGuard;

    @Override
    public Stage getStage() {
        return Stage.PRE_CALL;
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public void advise(AiReviewContext context) {
        List<String> flags = promptInjectionGuard.scan(
                context.getTicket().getTitle(),
                context.getTicket().getDescription());

        if (!flags.isEmpty()) {
            context.setInjectionSuspected(true);
            context.getInjectionFlags().addAll(flags);
            log.warn("[PromptInjectionDefenseAdvisor] SUSPICIOUS ticketId={} patterns={}",
                    context.getTicket().getId(), flags);
        } else {
            log.debug("[PromptInjectionDefenseAdvisor] ticketId={} no suspicious pattern detected",
                    context.getTicket().getId());
        }
    }
}