
package com.genai.java.spring.aireview.dto;

/**
 * Niveau de confiance renvoyé par le modèle IA pour une revue de ticket.
 * Jackson mappe directement une valeur JSON "LOW" / "MEDIUM" / "HIGH" vers
 * cette enum. Toute autre valeur (ex: "medium", "0.8", "high-ish") provoque
 * une exception de désérialisation, déjà interceptée dans
 * AiReviewService#runReview (étape 4 - parsing) qui marque la review comme
 * FAILED. Pas besoin de revalider la plage de valeurs dans AiReviewValidator.
 */
public enum Confidence {
    LOW,
    MEDIUM,
    HIGH
}

