package com.genai.java.spring.shared.advisor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Détection (non bloquante) de tentatives d'injection de prompt dans du
 * texte non fiable (titre/description de ticket, contenu récupéré, etc.).
 *
 * Logique partagée entre "aireview" (PromptInjectionDefenseAdvisor) et
 * "agent" (TicketAgentInvestigationService) : une seule liste de patterns,
 * un seul endroit à faire évoluer.
 *
 * Ne bloque jamais l'appel au modèle : sert uniquement à détecter/auditer/
 * logger, à croiser ensuite avec l'invariant needsHumanReview.
 */
@Component
public class PromptInjectionGuard {

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

    /** Retourne les patterns suspects trouvés (liste vide si rien de suspect). */
    public List<String> scan(String... untrustedTexts) {
        String haystack = toHaystack(untrustedTexts);

        List<String> flags = new ArrayList<>();
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (haystack.contains(pattern)) {
                flags.add(pattern);
            }
        }
        return flags;
    }

    private String toHaystack(String... texts) {
        StringBuilder sb = new StringBuilder();
        for (String text : texts) {
            if (text != null) {
                sb.append(text).append(' ');
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}