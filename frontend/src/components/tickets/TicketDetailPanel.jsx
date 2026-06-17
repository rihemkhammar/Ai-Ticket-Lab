import { useState } from 'react';
import { TbRefresh, TbCircleCheck, TbBrain } from 'react-icons/tb';
import { STATUS_CONFIG } from './Ticketconstants';
import { updateTicketStatus, runAiReview } from '../../services/api';

export default function TicketDetailPanel({ ticket, onClose, onStatusUpdated }) {
  const [aiResult, setAiResult] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);

  if (!ticket) return null;

  const s = STATUS_CONFIG[ticket.status] ?? STATUS_CONFIG.OPEN;

  const handleStatusChange = async (newStatus) => {
    try {
      const updated = await updateTicketStatus(ticket.id, newStatus);
      onStatusUpdated(updated);
    } catch (err) {
      console.error('[TicketDetailPanel] updateStatus error:', err);
    }
  };

  const handleAiReview = async () => {
    setAiLoading(true);
    setAiError(null);
    setAiResult(null);
    try {
      const data = await runAiReview(ticket.id);
      setAiResult(data);
    } catch (err) {
      setAiError(err.response?.data?.message ?? 'AI review failed.');
    } finally {
      setAiLoading(false);
    }
  };

  return (
    <div style={{
      background: 'var(--bg-card)',
      border: '1px solid rgba(77,124,199,0.15)',
      borderRadius: '20px',
      padding: '24px',
      position: 'sticky', top: '80px',
    }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
        <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-muted)', fontFamily: 'monospace' }}>
          #{ticket.id?.toString().slice(-6)}
        </span>
        <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '20px', lineHeight: 1 }}>×</button>
      </div>

      <h2 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--text-main)', margin: '0 0 20px', lineHeight: 1.4 }}>
        {ticket.title}
      </h2>

      {/* Meta */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
        {[
          { label: 'Status', val: (
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: s.dot, display: 'inline-block' }} />
              {s.label}
            </span>
          )},
          { label: 'Category', val: ticket.category ?? 'General' },
          { label: 'Opened', val: ticket.createdAt
            ? new Date(ticket.createdAt).toLocaleDateString('fr-BE', { day: '2-digit', month: 'short', year: 'numeric' })
            : '—' },
        ].map(({ label, val }) => (
          <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{label}</span>
            <span style={{ fontSize: '13px', color: 'var(--text-main)', fontWeight: 500 }}>{val}</span>
          </div>
        ))}
      </div>

      <div style={{ borderTop: '1px solid rgba(77,124,199,0.1)', marginBottom: '20px' }} />

      {/* Description */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '10px' }}>
          DESCRIPTION
        </div>
        <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, opacity: 0.7 }}>
          {ticket.description || 'No description provided.'}
        </p>
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {ticket.status !== 'IN_PROGRESS' && (
          <button onClick={() => handleStatusChange('IN_PROGRESS')} style={{
            width: '100%', padding: '13px', background: 'var(--btn-gradient)',
            border: 'none', borderRadius: '12px', color: '#fff', fontSize: '14px',
            fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}>
            <TbRefresh size={16} /> Mark as In Progress
          </button>
        )}
        {ticket.status !== 'CLOSED' && (
          <button onClick={() => handleStatusChange('CLOSED')} style={{
            width: '100%', padding: '13px', background: 'rgba(74,222,128,0.1)',
            border: '1px solid rgba(74,222,128,0.3)', borderRadius: '12px',
            color: '#4ade80', fontSize: '14px', fontWeight: 600, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}>
            <TbCircleCheck size={16} /> Mark as Closed
          </button>
        )}

        {/* Bouton AI Review */}
        <button onClick={handleAiReview} disabled={aiLoading} style={{
          width: '100%', padding: '13px', background: aiLoading ? 'rgba(139,92,246,0.1)' : 'rgba(139,92,246,0.15)',
          border: '1px solid rgba(139,92,246,0.3)', borderRadius: '12px',
          color: '#a78bfa', fontSize: '14px', fontWeight: 600,
          cursor: aiLoading ? 'not-allowed' : 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
        }}>
          <TbBrain size={16} />
          {aiLoading ? 'Running AI Review...' : 'Run AI Review'}
        </button>
      </div>

      {/* Erreur AI */}
      {aiError && (
        <div style={{ marginTop: '16px', padding: '12px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '12px' }}>
          <span style={{ fontSize: '13px', color: '#f87171' }}>⚠ {aiError}</span>
        </div>
      )}

      {/* Résultat AI */}
      {aiResult?.result && (
        <div style={{ marginTop: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ borderTop: '1px solid rgba(139,92,246,0.2)', paddingTop: '16px' }}>
            <div style={{ fontSize: '12px', fontWeight: 700, color: '#a78bfa', letterSpacing: '0.5px', marginBottom: '12px' }}>
              🤖 AI REVIEW — {aiResult.status}
            </div>

            {/* Summary */}
            <Section title="Summary">
              <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>
                {aiResult.result.summary}
              </p>
            </Section>

            {/* Possible Causes */}
            <Section title="Possible Causes">
              <ul style={{ margin: 0, paddingLeft: '18px' }}>
                {aiResult.result.possibleCauses.map((c, i) => (
                  <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
                ))}
              </ul>
            </Section>

            {/* Recommended Checks */}
            <Section title="Recommended Checks">
              <ul style={{ margin: 0, paddingLeft: '18px' }}>
                {aiResult.result.recommendedChecks.map((c, i) => (
                  <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
                ))}
              </ul>
            </Section>

            {/* Draft Response */}
            <Section title="Draft Response">
              <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>
                {aiResult.result.draftResponse}
              </p>
            </Section>

            {/* Confidence */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Confidence</span>
              <span style={{ fontSize: '13px', fontWeight: 700, color: '#a78bfa' }}>
                {Math.round(aiResult.result.confidence * 100)}%
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '14px' }}>
      <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '6px' }}>
        {title.toUpperCase()}
      </div>
      {children}
    </div>
  );
}