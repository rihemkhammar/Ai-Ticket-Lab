package com.genai.java.spring.aireview.advisor;

import com.genai.java.spring.aireview.dto.TicketAiReviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Remplace l'ancien AiReviewValidator
 * summary, possibleCauses, recommendedChecks, draftResponse, confidence.
 */
@Slf4j
@Component
public class StructuralValidationAdvisor implements AiReviewAdvisor {

    @Override
    public Stage getStage() {
        return Stage.POST_CALL;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void advise(AiReviewContext context) {
        TicketAiReviewResponse response = context.getAiResponse();

        if (response == null) {
            context.invalidate("AI response could not be parsed.");
            log.warn("[StructuralValidationAdvisor] ticketId={} response is null", context.getTicket().getId());
            return;
        }
        if (isBlank(response.getSummary())) {
            context.invalidate("Summary must not be blank.");
        }
        if (isEmpty(response.getPossibleCauses())) {
            context.invalidate("Possible causes must not be empty.");
        }
        if (isEmpty(response.getRecommendedChecks())) {
            context.invalidate("Recommended checks must not be empty.");
        }
        if (isBlank(response.getDraftResponse())) {
            context.invalidate("Draft response must not be blank.");
        }
        if (response.getConfidence() == null) {
            context.invalidate("Confidence must be LOW, MEDIUM, or HIGH.");
        }

        log.info("[StructuralValidationAdvisor] ticketId={} valid={}",
                context.getTicket().getId(), context.isValid());
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isEmpty(List<String> list) { return list == null || list.isEmpty(); }
}