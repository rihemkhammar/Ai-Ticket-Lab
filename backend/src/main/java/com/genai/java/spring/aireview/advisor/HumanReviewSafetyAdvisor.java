package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import com.genai.java.spring.shared.advisor.HumanReviewPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Garantit que limitations n'est jamais vide et que needsHumanReview est
 * toujours true, via HumanReviewPolicy (partagé avec le module agent).
 * Croise aussi avec PromptInjectionDefenseAdvisor : si une injection a été
 * détectée, on logue un avertissement de sécurité supplémentaire même si
 * la review reste valide.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HumanReviewSafetyAdvisor implements AiReviewAdvisor {

    private final HumanReviewPolicy humanReviewPolicy;

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

        humanReviewPolicy.requireLimitations(response.getLimitations(), context::invalidate);

        humanReviewPolicy.requireHumanReview(response.getNeedsHumanReview(), reason -> {
            context.invalidate(reason);
            log.warn("[HumanReviewSafetyAdvisor] ticketId={} model returned needsHumanReview=false/null — REJECTED",
                    context.getTicket().getId());
        });

        if (context.isInjectionSuspected() && context.isValid()) {
            log.warn("[HumanReviewSafetyAdvisor] ticketId={} review accepted despite suspected injection patterns={} "
                            + "— needsHumanReview enforced=true",
                    context.getTicket().getId(), context.getInjectionFlags());
        }

        log.info("[HumanReviewSafetyAdvisor] ticketId={} valid={}",
                context.getTicket().getId(), context.isValid());
    }
}