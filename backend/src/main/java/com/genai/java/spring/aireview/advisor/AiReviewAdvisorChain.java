package com.genai.java.spring.aireview.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrateur de la chaîne d'advisors. Spring injecte automatiquement
 * tous les beans qui implémentent AiReviewAdvisor (List<AiReviewAdvisor>).
 * On les trie une fois pour toutes par stage puis par ordre.
 */
@Slf4j
@Component
public class AiReviewAdvisorChain {

    private final List<AiReviewAdvisor> preCallAdvisors;
    private final List<AiReviewAdvisor> postCallAdvisors;

    public AiReviewAdvisorChain(List<AiReviewAdvisor> advisors) {
        this.preCallAdvisors = advisors.stream()
                .filter(a -> a.getStage() == AiReviewAdvisor.Stage.PRE_CALL)
                .sorted(Comparator.comparingInt(AiReviewAdvisor::getOrder))
                .toList();
        this.postCallAdvisors = advisors.stream()
                .filter(a -> a.getStage() == AiReviewAdvisor.Stage.POST_CALL)
                .sorted(Comparator.comparingInt(AiReviewAdvisor::getOrder))
                .toList();

        log.info("AiReviewAdvisorChain initialised — preCall={} postCall={}",
                preCallAdvisors.stream().map(a -> a.getClass().getSimpleName()).toList(),
                postCallAdvisors.stream().map(a -> a.getClass().getSimpleName()).toList());
    }

    public void runPreCall(AiReviewContext context) {
        for (AiReviewAdvisor advisor : preCallAdvisors) {
            log.debug("Running pre-call advisor: {}", advisor.getClass().getSimpleName());
            advisor.advise(context);
        }
    }

    public void runPostCall(AiReviewContext context) {
        for (AiReviewAdvisor advisor : postCallAdvisors) {
            log.debug("Running post-call advisor: {}", advisor.getClass().getSimpleName());
            advisor.advise(context);
        }
    }
}