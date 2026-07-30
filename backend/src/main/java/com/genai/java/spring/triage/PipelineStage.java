package com.genai.java.spring.triage;

/**
 * Étapes du pipeline synchrone de triage, dans l'ordre d'exécution réel :
 * Classify -> Order -> Dispatch -> Investigation -> Review -> Rules -> HITL -> Observation.
 *
 * Un {@link TriageTicketResult} porte la dernière étape effectivement
 * atteinte pour un ticket donné (utile pour l'affichage front — stepper,
 * badges — et pour savoir où le pipeline s'est arrêté en cas d'échec ou
 * d'attente de revue humaine).
 */
public enum PipelineStage {
    CLASSIFY,
    ORDER,
    DISPATCH,
    INVESTIGATION,
    REVIEW,
    RULES,
    HITL,
    OBSERVATION
}
