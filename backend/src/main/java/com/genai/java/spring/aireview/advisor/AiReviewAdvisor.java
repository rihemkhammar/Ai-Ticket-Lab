package com.genai.java.spring.aireview.advisor;

/**
 * Contrat commun pour tous les advisors de la revue IA.
 * Chaque advisor agit sur un AiReviewContext partagé, soit avant
 * l'appel au modèle (PRE_CALL), soit après réception/parsing de la
 * réponse (POST_CALL).
 */
public interface AiReviewAdvisor {

    enum Stage { PRE_CALL, POST_CALL }

    Stage getStage();

    void advise(AiReviewContext context);

    /** Ordre d'exécution au sein d'un même stage (croissant). */
    default int getOrder() {
        return 0;
    }
}