package com.genai.java.spring.triage;

import com.genai.java.spring.triage.dto.TriageBatchRequest;
import com.genai.java.spring.triage.dto.TriageRunResponse;
import com.genai.java.spring.triage.graph.TriageAgentState;
import com.genai.java.spring.triage.graph.TriageGraphState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Point d'entrée UNIQUE du batch de triage (endpoint POST /api/triage/batches).
 *
 * Délègue désormais TOUTE l'orchestration Classify -> Order -> Dispatch
 * -> (Investigation si CRITICAL/HIGH) -> Review -> Rules -> HITL au
 * graphe LangGraph4j compilé par TriageGraphConfig#triageGraph(), au
 * lieu d'enchaîner les nodes "à la main" dans un while (ancien
 * comportement, conservé identique au niveau métier - même ordre,
 * même condition de skip d'Investigation, même boucle tant que la
 * file ordonnée n'est pas vide).
 *
 * Ne duplique AUCUNE logique métier : chaque étape reste déléguée à un
 * bean existant et déjà testé (ClassifyTicketsNode, InvestigationNode,
 * ReviewNode, RulesNode, HitlCheckpointNode) - c'est TriageGraphConfig
 * qui les enveloppe pour le graphe, ce service ne les appelle plus
 * directement.
 *
 * Contrat de retour INCHANGÉ : TriageRunResponse, lu depuis la DB via
 * TriageOrchestratorService.getRun() - chaque ticket traité est déjà
 * persisté au fil de l'eau par HitlCheckpointNode (recordTreated),
 * donc on n'a pas besoin de relire l'état final du graphe pour
 * construire la réponse.
 */
@Slf4j
@Service
public class TriagePipelineService {

    private final TriageOrchestratorService orchestratorService;
    private final CompiledGraph<TriageAgentState> triageGraph;

    public TriagePipelineService(TriageOrchestratorService orchestratorService,
                                 CompiledGraph<TriageAgentState> triageGraph) {
        this.orchestratorService = orchestratorService;
        this.triageGraph = triageGraph;
    }

    /**
     * Crée le run (validation + persistance PENDING déléguées à
     * TriageOrchestratorService.startBatch, inchangé), puis fait
     * exécuter la chaîne complète par le graphe compilé avant de
     * répondre.
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

        TriageGraphState initialState = new TriageGraphState(runId, created.getTicketQueue());
        initialState.setRequesterUsername(requesterUsername);

        log.info("Triage run {} - invoking LangGraph4j pipeline ({} tickets)",
                runId, initialState.getTicketQueue().size());

        // NOTE version-sensitive: verify this call shape against the
        // langgraph4j-core version pinned in pom.xml (1.8.20) before
        // trusting it blindly - invoke()'s signature and return type
        // (Optional<S> vs CompletableFuture<S>) have both changed
        // across releases. See TriageGraphConfig class javadoc.
        Optional<TriageAgentState> finalState =
                triageGraph.invoke(Map.of(TriageAgentState.STATE_KEY, initialState));

        if (finalState.isEmpty()) {
            log.warn("Triage run {} - graph execution produced no final state", runId);
        } else {
            log.info("Triage run {} - graph execution completed", runId);

            // Persist the FULL classification map (all tickets, not just
            // the one dispatched through the full pipeline) so the
            // frontend can display the whole ranking.
            orchestratorService.recordClassifications(
                    runId, finalState.get().triageState().getClassifications());

            // The graph really reached END here (invoke() is blocking) —
            // mark the run COMPLETED explicitly instead of relying on the
            // old "ticketQueue empty" heuristic, which no longer applies
            // since only the top ticket is ever dispatched.
            orchestratorService.markCompleted(runId);
        }

        return orchestratorService.getRun(runId)
                .orElseThrow(() -> new TriageValidationException(
                        "Triage run not found after processing: " + runId));
    }
}