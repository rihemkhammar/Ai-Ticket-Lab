package com.genai.java.spring.aireview.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**

 * Ne bloque jamais l'appel : le ticket reste envoyé au modèle (qui doit
 * le traiter comme donnée, pas comme instruction — cf. system prompt).
 * Sert à détecter/auditer/logger une tentative d'injection pour pouvoir
 * la croiser plus tard avec HumanReviewSafetyAdvisor.
 */
@Slf4j
@Component
public class PromptInjectionDefenseAdvisor implements AiReviewAdvisor {

    private static final List<String> SUSPICIOUS_PATTERNS = List.of(
            "ignore all previous instructions",
            "ignore previous instructions",
            "disregard the system prompt",
            "you are now",
            "new instructions:",
            "mark this ticket as resolved",
            "no human review is needed",
            "needshumanreview\": false",
            "set confidence high"
    );

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
        String haystack = (nullToEmpty(context.getTicket().getTitle())
                + " " + nullToEmpty(context.getTicket().getDescription()))
                .toLowerCase(Locale.ROOT);

        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (haystack.contains(pattern)) {
                context.setInjectionSuspected(true);
                context.getInjectionFlags().add(pattern);
            }
        }

        if (context.isInjectionSuspected()) {
            log.warn("[PromptInjectionDefenseAdvisor] SUSPICIOUS ticketId={} patterns={}",
                    context.getTicket().getId(), context.getInjectionFlags());
        } else {
            log.debug("[PromptInjectionDefenseAdvisor] ticketId={} no suspicious pattern detected",
                    context.getTicket().getId());
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}