import { TbListSearch } from 'react-icons/tb';
import { Spinner } from '../hitl/HitlUi';

/**
 * "View Trace" button shown after an agent/HITL run completes .
 * Reuses the Spinner from HitlUi for visual consistency.
 */
export default function TraceButton({ onClick, loading }) {
  return (
    <button onClick={onClick} disabled={loading} style={{
      width: '100%', padding: '11px', marginTop: '10px',
      background: 'rgba(148,163,184,0.08)',
      border: '1px solid rgba(148,163,184,0.25)', borderRadius: '10px',
      color: '#94a3b8', fontSize: '13px', fontWeight: 600,
      cursor: loading ? 'not-allowed' : 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
    }}>
      {loading ? <Spinner color="#94a3b8" /> : <TbListSearch size={15} />}
      {loading ? 'Loading trace...' : 'View Trace'}
    </button>
  );
}