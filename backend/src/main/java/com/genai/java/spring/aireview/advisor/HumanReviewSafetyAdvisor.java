package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**

 * Garantit que limitations n'est jamais vide et que needsHumanReview
 * est toujours true. Croise aussi avec PromptInjectionDefenseAdvisor :
 * si une injection a été détectée, on logue un avertissement de sécurité
 * supplémentaire même si la review reste valide.
 */
@Slf4j
@Component
public class HumanReviewSafetyAdvisor implements AiReviewAdvisor {

    @Override
    public Stage getStage() {
        return Stage.POST_CALL;
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public void advise(AiReviewContext context) {
        TicketAiReviewResponse response = context.getAiResponse();
        if (response == null) {
            return; // déjà invalidé par StructuralValidationAdvisor
        }

        if (response.getLimitations() == null || response.getLimitations().isEmpty()) {
            context.invalidate("AI response limitations must not be empty.");
        }

        if (response.getNeedsHumanReview() == null || !response.getNeedsHumanReview()) {
            context.invalidate("AI response needsHumanReview must be true.");
            log.warn("[HumanReviewSafetyAdvisor] ticketId={} model returned needsHumanReview=false/null — REJECTED",
                    context.getTicket().getId());
        }

        if (context.isInjectionSuspected() && context.isValid()) {
            log.warn("[HumanReviewSafetyAdvisor] ticketId={} review accepted despite suspected injection patterns={} "
                            + "— needsHumanReview enforced=true",
                    context.getTicket().getId(), context.getInjectionFlags());
        }

        log.info("[HumanReviewSafetyAdvisor] ticketId={} valid={}",
                context.getTicket().getId(), context.isValid());
    }
}