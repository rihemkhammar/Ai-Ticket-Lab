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


export const OUTCOME_CONFIG = {
  SUCCESS: { label: 'Completed — review pending', color: '#4ade80' },
  FAILED:  { label: 'Failed',                     color: '#f87171' },
};

// Résultat du stage "Rules" (RulesNode.decide, déterministe — pas de LLM).
// Reflète TicketRoutingRules.RoutingDecision côté back.
export const ROUTING_DECISION_CONFIG = {
  ESCALATE_TO_HUMAN_PRIORITY: { label: 'Priority human review', color: '#fb923c' },
  STANDARD_HUMAN_REVIEW:      { label: 'Standard human review', color: '#60a5fa' },
};

// Étapes réellement exécutées par TriagePipelineService, dans l'ordre.
// (OBSERVATION retirée : ce n'était qu'une étape du squelette backend
// jamais implémentée — le vrai pipeline s'arrête à HITL.)
export const PIPELINE_STAGES = [
  { key: 'CLASSIFY',      label: 'Classification' },
  { key: 'ORDER',         label: 'Ordering' },
  { key: 'DISPATCH',      label: 'Dispatch' },
  { key: 'INVESTIGATION', label: 'Investigation' },
  { key: 'REVIEW',        label: 'Review' },
  { key: 'RULES',         label: 'Rules' },
  { key: 'HITL',          label: 'HITL' },
];

export function isInvestigationApplicable(criticality) {
  return criticality === 'CRITICAL' || criticality === 'HIGH';
}

export function isPendingHumanReview(item) {
  return item?.outcome === 'SUCCESS' && !!item?.agentRunId;
}