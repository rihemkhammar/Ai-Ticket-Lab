import { TbRobot } from 'react-icons/tb';
import { Spinner, ErrorBox } from './HitlUi';
import { ResultPreview } from '../TicketDetailUi';

const HITL_STATUS_LABEL = {
  WAITING_FOR_HUMAN: 'Waiting for review',
  REVISING:          'Revising…',
  FINALIZED:         'Finalized',
  REJECTED:          'Rejected',
  FAILED:            'Failed',
};

/**
 * Compact HITL entry shown in the side panel: "Run" button when there's no
 * review yet, or a one-line status preview + "Voir →" (opens the modal)
 * once a review exists — matching AI Review / RAG Review / Evidence / Agent.
 */
export function HitlPanel({ hitlLoading, hitlError, hitlResult, onRun, onOpen }) {
  return (
    <>
      {!hitlResult && (
        <button onClick={onRun} disabled={hitlLoading} style={{
          width: '100%', padding: '13px',
          background: hitlLoading ? 'rgba(250,204,21,0.05)' : 'rgba(250,204,21,0.10)',
          border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px',
          color: '#facc15', fontSize: '14px', fontWeight: 600,
          cursor: hitlLoading ? 'not-allowed' : 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
        }}>
          {hitlLoading ? <Spinner color="#facc15" /> : <TbRobot size={16} />}
          {hitlLoading ? 'Running agent investigation...' : 'Run HITL Agent Review'}
        </button>
      )}
      {hitlError && <ErrorBox>{hitlError}</ErrorBox>}
      {hitlResult && (
        <ResultPreview
          color="#facc15"
          label={`HITL — ${HITL_STATUS_LABEL[hitlResult.status] ?? hitlResult.status}`}
          needsHuman={hitlResult.status === 'WAITING_FOR_HUMAN'}
          onOpen={onOpen}
        />
      )}
    </>
  );
}