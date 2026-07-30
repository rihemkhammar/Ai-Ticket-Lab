package com.genai.java.spring.triage;

import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.ClassifyTicketsNode;
import com.genai.java.spring.triage.graph.DispatchNextTicketNode;
import com.genai.java.spring.triage.graph.HitlCheckpointNode;
import com.genai.java.spring.triage.graph.InvestigationNode;
import com.genai.java.spring.triage.graph.OrderQueueNode;
import com.genai.java.spring.triage.graph.ReviewNode;
import com.genai.java.spring.triage.graph.RulesNode;
import com.genai.java.spring.triage.graph.TriageClassification;
import com.genai.java.spring.triage.graph.TriageGraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Point d'entrée UNIQUE du batch de triage (endpoint POST /api/triage/batches).
 *
 * Exécute de façon SYNCHRONE, pour chaque ticket du lot (max 5, Rule 2.11) :
 *
 *   Agent 1 (Classify, avec note = criticité + rationale)
 *     -> Order (tri déterministe CRITICAL > HIGH > MEDIUM > LOW)
 *     -> Dispatch (un ticket à la fois)
 *       -> Agent 2 (Investigation) SEULEMENT si le ticket est CRITICAL/HIGH
 *       -> Agent 3 (Review / grounding RAG), avec le résultat d'Agent 2
 *          disponible dans le state si l'investigation a eu lieu
 *       -> Rules (règles déterministes de routage, sans appel LLM)
 *       -> HITL (checkpoint humain, TOUJOURS créé — enregistre aussi le
 *          "treated item" = résumé/traçabilité/observation du ticket via
 *          TriageOrchestratorService.recordTreated, déjà appelé par
 *          HitlCheckpointNode)
 *     -> boucle jusqu'à ce que la file ordonnée soit vide
 *
 * Ne duplique AUCUNE logique métier : chaque étape délègue à un bean
 * existant et déjà testé (ClassifyTicketsNode, InvestigationNode,
 * ReviewNode, RulesNode, HitlCheckpointNode). Le graphe LangGraph4j
 * (TriageGraphConfig) reste un stub non câblé ; en attendant, ce service
 * enchaîne directement les mêmes nœuds "à la main", dans le même ordre
 * que celui documenté dans TriageGraphConfig.
 *
 * Contrat de retour INCHANGÉ : TriageRunResponse — le même format que
 * GET /api/triage/batches/{runId} (endpoint "ancien format" déjà
 * consommé par le front). Ainsi le front peut appeler indifféremment
 * POST (lancer + attendre le résultat complet) ou GET (recharger l'état
 * d'un run déjà lancé) sans changer son parsing.
 */
@Slf4j
@Service
public class TriagePipelineService {

    private final TriageOrchestratorService orchestratorService;
    private final ClassifyTicketsNode classifyTicketsNode;
    private final OrderQueueNode orderQueueNode;
    private final DispatchNextTicketNode dispatchNextTicketNode;
    private final InvestigationNode investigationNode;
    private final ReviewNode reviewNode;
    private final RulesNode rulesNode;
    private final HitlCheckpointNode hitlCheckpointNode;

    public TriagePipelineService(TriageOrchestratorService orchestratorService,
                                 ClassifyTicketsNode classifyTicketsNode,
                                 OrderQueueNode orderQueueNode,
                                 DispatchNextTicketNode dispatchNextTicketNode,
                                 InvestigationNode investigationNode,
                                 ReviewNode reviewNode,
                                 RulesNode rulesNode,
                                 HitlCheckpointNode hitlCheckpointNode) {
        this.orchestratorService = orchestratorService;
        this.classifyTicketsNode = classifyTicketsNode;
        this.orderQueueNode = orderQueueNode;
        this.dispatchNextTicketNode = dispatchNextTicketNode;
        this.investigationNode = investigationNode;
        this.reviewNode = reviewNode;
        this.rulesNode = rulesNode;
        this.hitlCheckpointNode = hitlCheckpointNode;
    }

    /**
     * Crée le run (validation + persistance PENDING déléguées à
     * TriageOrchestratorService.startBatch, inchangé), puis exécute la
     * chaîne complète pour chaque ticket avant de répondre.
     *
     * @param requesterUsername username du technicien authentifié qui a
     *     lancé ce batch (Authentication.getName() côté contrôleur).
     *     Propagé au state pour qu'Agent 3 (ReviewNode) attribue chaque
     *     ai_review à ce vrai utilisateur plutôt qu'à un compte système.
     */
    public TriageRunResponse startAndRun(TriageBatchRequest request, String requesterUsername) {
        TriageRunResponse created = orchestratorService.startBatch(request);
        Long runId = created.getRunId();

        orchestratorService.markRunning(runId);

        TriageGraphState state = new TriageGraphState(runId, created.getTicketQueue());
        state.setRequesterUsername(requesterUsername);

        // Agent 1 : classification (note = criticité + rationale) de tout le lot.
        classifyTicketsNode.apply(state);
        // Règle déterministe : ordonne la file (CRITICAL -> HIGH -> MEDIUM -> LOW).
        orderQueueNode.apply(state);

        dispatchNextTicketNode.apply(state);
        while (state.getCurrentTicketId() != null) {
            Long ticketId = state.getCurrentTicketId();
            log.info("Triage run {} - processing ticket {}", runId, ticketId);

            if (isCriticalEnoughForInvestigation(state)) {
                // Agent 2 : investigation, uniquement pour les tickets critiques.
                investigationNode.apply(state);
            } else {
                log.debug("Triage run {} - ticket {} skips Agent 2 (not critical)", runId, ticketId);
            }

            // Agent 3 : review / grounding, reçoit le state (donc le résultat
            // d'Agent 2 quand il a été exécuté) via currentInvestigationResult.
            reviewNode.apply(state);

            // Règles déterministes de routage (pas d'appel LLM).
            rulesNode.apply(state);

            // HITL : crée le checkpoint humain et enregistre le résumé /
            // traçabilité / observation du ticket (recordTreated), que le
            // ticket ait réussi ou échoué à une étape précédente (Rule 2.7).
            hitlCheckpointNode.apply(state);

            dispatchNextTicketNode.apply(state);
        }

        return orchestratorService.getRun(runId)
                .orElseThrow(() -> new TriageValidationException(
                        "Triage run not found after processing: " + runId));
    }

    private boolean isCriticalEnoughForInvestigation(TriageGraphState state) {
        TriageClassification classification = state.getClassifications().get(state.getCurrentTicketId());
        if (classification == null || classification.getCriticality() == null) {
            return false;
        }
        return classification.getCriticality() == TicketCriticality.CRITICAL
                || classification.getCriticality() == TicketCriticality.HIGH;
    }
}