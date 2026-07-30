// Labels/colors for HITL statuses (S5). Follows the same pattern as
// Ticketconstants.jsx (STATUS_CONFIG) to stay visually consistent with
// the rest of the app.

export const RUN_STATUS_CONFIG = {
  RUNNING:           { label: 'Running',              color: '#60a5fa' },
  WAITING_FOR_HUMAN: { label: 'Waiting for review',   color: '#facc15' },
  REVISING:          { label: 'Revising',              color: '#a78bfa' },
  FINALIZED:         { label: 'Finalized',             color: '#4ade80' },
  REJECTED:          { label: 'Rejected',              color: '#f87171' },
  FAILED:            { label: 'Failed',                color: '#f87171' },
};

export const CHECKPOINT_STATUS_CONFIG = {
  PENDING:    { label: 'Pending',     color: '#facc15' },
  SUPERSEDED: { label: 'Superseded',  color: '#94a3b8' },
  FINALIZED:  { label: 'Finalized',   color: '#4ade80' },
  REJECTED:   { label: 'Rejected',    color: '#f87171' },
};

export const CONFIDENCE_CONFIG = {
  LOW:    { label: 'Low',    color: '#f87171' },
  MEDIUM: { label: 'Medium', color: '#facc15' },
  HIGH:   { label: 'High',   color: '#4ade80' },
};

export const DECISION_LABELS = {
  APPROVE:          'Approved',
  REJECT:           'Rejected',
  REQUEST_REVISION: 'Revision requested',
};

export const MAX_REVISION_CYCLES = 1;
