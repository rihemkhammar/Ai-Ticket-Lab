package com.genai.java.spring.shared.advisor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Invariant métier partagé : toute sortie IA (aireview, agent, RAG review…)
 * doit obligatoirement déclencher une revue humaine et fournir des
 * limitations.
 *
 * Le composant reste indépendant du mécanisme d'erreur de chaque appelant
 * (contexte d'advisor invalidé vs exception métier levée) grâce au callback
 * `onViolation` — c'est pour ça qu'on ne le transforme pas en Advisor
 * générique typé sur un Context commun, ce qui aurait forcé un refactor
 * beaucoup plus lourd pour un gain nul.
 */
@Component
public class HumanReviewPolicy {

    public static final String NEEDS_HUMAN_REVIEW_MESSAGE = "needsHumanReview must be true.";
    public static final String LIMITATIONS_MESSAGE = "limitations must not be empty.";

    public void requireHumanReview(Boolean needsHumanReview, Consumer<String> onViolation) {
        if (needsHumanReview == null || !needsHumanReview) {
            onViolation.accept(NEEDS_HUMAN_REVIEW_MESSAGE);
        }
    }

    public void requireLimitations(List<String> limitations, Consumer<String> onViolation) {
        if (limitations == null || limitations.isEmpty()) {
            onViolation.accept(LIMITATIONS_MESSAGE);
        }
    }
}