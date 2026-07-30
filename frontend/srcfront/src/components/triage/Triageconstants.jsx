export const MAX_BATCH_SIZE = 5;

export const RUN_STATUS_CONFIG = {
  PENDING:   { label: 'Pending',   dot: '#facc15' },
  RUNNING:   { label: 'Running',   dot: '#60a5fa' },
  COMPLETED: { label: 'Completed', dot: '#4ade80' },
  FAILED:    { label: 'Failed',    dot: '#f87171' },
};

export const CRITICALITY_CONFIG = {
  CRITICAL: { label: 'Critical', color: '#f87171' },
  HIGH:     { label: 'High',     color: '#fb923c' },
  MEDIUM:   { label: 'Medium',   color: '#facc15' },
  LOW:      { label: 'Low',      color: '#4ade80' },
};

// Contrat réel de TriageDispatchOutcome côté backend (triage/TriageDispatchOutcome.java) :
// SUCCESS ou FAILED, uniquement. Il n'existe PAS de PENDING_REVIEW / REJECTED /
// RULES_BLOCKED côté backend — ces valeurs ont été retirées ici (elles ne
// pouvaient jamais matcher, donc l'ancien badge tombait toujours sur le
// fallback ?? item.outcome brut).
//
// SUCCESS veut dire : le ticket a traversé tout le pipeline (Classify -> Order
// -> Dispatch -> [Investigation] -> Review -> Rules -> HITL) et un checkpoint
// HITL a bien été créé. Comme HitlAgentReviewService s'arrête TOUJOURS sur
// WAITING_FOR_HUMAN pour un checkpoint fraîchement créé (needsHumanReview
// toujours vrai côté agent), SUCCESS implique concrètement "en attente de
// revue humaine" tant que personne n'a pris de décision.
export const OUTCOME_CONFIG = {
  SUCCESS: { label: 'Terminé — revue en attente', color: '#4ade80' },
  FAILED:  { label: 'Échec',                       color: '#f87171' },
};

// Étapes réellement exécutées par TriagePipelineService, dans l'ordre.
// (OBSERVATION retirée : ce n'était qu'une étape du squelette backend
// jamais implémentée — le vrai pipeline s'arrête à HITL.)
export const PIPELINE_STAGES = [
  { key: 'CLASSIFY',      label: 'Classification' },
  { key: 'ORDER',         label: 'Ordonnancement' },
  { key: 'DISPATCH',      label: 'Dispatch' },
  { key: 'INVESTIGATION', label: 'Investigation' },
  { key: 'REVIEW',        label: 'Review' },
  { key: 'RULES',         label: 'Règles' },
  { key: 'HITL',          label: 'HITL' },
];

// Mirroir exact du seuil appliqué côté backend
// (TriagePipelineService#isCriticalEnoughForInvestigation) : seuls les
// tickets CRITICAL ou HIGH passent par l'Agent 2 (Investigation). Le front
// peut donc reconstruire fidèlement quelles étapes ont tourné, sans que le
// backend ait besoin d'exposer un champ `stage` par ticket.
export function isInvestigationApplicable(criticality) {
  return criticality === 'CRITICAL' || criticality === 'HIGH';
}

// SUCCESS => le checkpoint HITL a été créé et est WAITING_FOR_HUMAN par
// construction (voir OUTCOME_CONFIG ci-dessus). Le statut HITL précis
// (WAITING_FOR_HUMAN / REVISING / FINALIZED / REJECTED) n'est connu qu'en
// rechargeant la revue elle-même (getHitlReview) — c'est ce que fait déjà
// TriageTicketCard au clic ; pas besoin d'un champ hitlStatus sur l'item.
export function isPendingHumanReview(item) {
  return item?.outcome === 'SUCCESS' && !!item?.agentRunId;
}