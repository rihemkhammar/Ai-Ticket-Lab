// Labels/colors for the AI Trace view . Mirrors Hitlconstants.jsx
// (RUN_STATUS_CONFIG / CHECKPOINT_STATUS_CONFIG) for visual consistency.

export const RUN_TYPE_LABELS = {
  AGENT_INVESTIGATION: 'Agent Investigation',
  HITL_AGENT_REVIEW:   'HITL Agent Review',
};

export const TRACE_STATUS_CONFIG = {
  RUNNING:           { label: 'Running',            color: '#60a5fa' },
  SUCCESS:           { label: 'Success',             color: '#4ade80' },
  WAITING_FOR_HUMAN: { label: 'Waiting for review',  color: '#facc15' },
  REVISING:          { label: 'Revising',            color: '#a78bfa' },
  FINALIZED:         { label: 'Finalized',           color: '#4ade80' },
  REJECTED:          { label: 'Rejected',             color: '#f87171' },
  FAILED:            { label: 'Failed',               color: '#f87171' },
};

export const CHECKPOINT_TRACE_STATUS_CONFIG = {
  PENDING:    { label: 'Pending',     color: '#facc15' },
  SUPERSEDED: { label: 'Superseded',  color: '#94a3b8' },
  FINALIZED:  { label: 'Finalized',   color: '#4ade80' },
  REJECTED:   { label: 'Rejected',    color: '#f87171' },
};