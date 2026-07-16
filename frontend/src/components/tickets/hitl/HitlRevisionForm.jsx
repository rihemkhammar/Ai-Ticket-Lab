import { useState } from 'react';
import { TbCheck, TbX, TbEdit } from 'react-icons/tb';
import { Spinner } from './HitlUi';

/**
 * Comment box + 3 decision buttons (S5 §5.4 / §7.4).
 * - comment optional for APPROVE
 * - comment required for REJECT and REQUEST_REVISION (validated both in UI and backend)
 * - "Request Revision" disabled once the revision cycle (max 1) is exhausted
 */
export default function HitlRevisionForm({ onDecide, deciding, revisionDisabled }) {
  const [comment, setComment] = useState('');
  const [validationError, setValidationError] = useState(null);

  const handleDecide = (decision) => {
    const requiresComment = decision === 'REJECT' || decision === 'REQUEST_REVISION';
    if (requiresComment && !comment.trim()) {
      setValidationError(
        decision === 'REJECT'
          ? 'A comment is required to reject this draft.'
          : 'A comment is required to request a revision.'
      );
      return;
    }
    setValidationError(null);
    onDecide(decision, comment.trim() || undefined);
  };

  return (
    <div style={{ marginTop: '16px' }}>
      <label style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', textTransform: 'uppercase', display: 'block', marginBottom: '6px' }}>
        Reviewer comment
      </label>
      <textarea
        value={comment}
        onChange={e => setComment(e.target.value)}
        disabled={deciding}
        placeholder="Optional for Approve — required for Reject / Request Revision"
        rows={3}
        style={{
          width: '100%', resize: 'vertical', padding: '10px 12px',
          background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: '10px', color: 'var(--text-main)', fontSize: '13px',
          fontFamily: 'inherit', lineHeight: 1.5, boxSizing: 'border-box',
        }}
      />
      {validationError && (
        <div style={{ fontSize: '12px', color: '#f87171', marginTop: '6px' }}>{validationError}</div>
      )}

      <div style={{ display: 'flex', gap: '8px', marginTop: '12px', flexWrap: 'wrap' }}>
        <DecisionButton
          label="Approve"
          icon={<TbCheck size={15} />}
          color="#4ade80"
          onClick={() => handleDecide('APPROVE')}
          disabled={deciding}
          loading={deciding === 'APPROVE'}
        />
        <DecisionButton
          label="Reject"
          icon={<TbX size={15} />}
          color="#f87171"
          onClick={() => handleDecide('REJECT')}
          disabled={deciding}
          loading={deciding === 'REJECT'}
        />
        <DecisionButton
          label="Request Revision"
          icon={<TbEdit size={15} />}
          color="#a78bfa"
          onClick={() => handleDecide('REQUEST_REVISION')}
          disabled={deciding || revisionDisabled}
          loading={deciding === 'REQUEST_REVISION'}
          title={revisionDisabled ? 'Only one revision cycle is supported in this training milestone.' : undefined}
        />
      </div>
    </div>
  );
}

function DecisionButton({ label, icon, color, onClick, disabled, loading, title }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      style={{
        flex: '1 1 140px', padding: '11px', borderRadius: '10px',
        border: `1px solid ${color}44`,
        background: disabled ? `${color}08` : `${color}14`,
        color, fontSize: '13px', fontWeight: 600,
        cursor: disabled ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
        opacity: disabled && !loading ? 0.5 : 1,
      }}
    >
      {loading ? <Spinner color={color} /> : icon}
      {label}
    </button>
  );
}
