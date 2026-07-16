import { TbX } from 'react-icons/tb';
import { Section } from './HitlUi';

/**
 * Displays a REJECTED checkpoint (S5 §7.6): the rejection is shown clearly,
 * the human comment is visible, and the original AI draft remains visible
 * for reference — nothing is deleted.
 */
export default function HitlRejectedResult({ review }) {
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#f87171', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbX size={14} /> REJECTED — NO AI RECOMMENDATION WAS ACCEPTED
      </div>

      {review.humanComment && (
        <Section title="Reviewer Comment">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{review.humanComment}</p>
        </Section>
      )}

      {review.investigationSummary && (
        <Section title="Original Draft — Investigation Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.7, margin: 0 }}>{review.investigationSummary}</p>
        </Section>
      )}

      {review.draftTechnicianResponse && (
        <Section title="Original Draft — Technician Response">
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{review.draftTechnicianResponse}</p>
        </Section>
      )}

      <div style={{ marginTop: '4px', padding: '12px', background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.25)', borderRadius: '12px' }}>
        <span style={{ fontSize: '12px', color: '#f87171' }}>
          This draft was rejected by a human reviewer. The ticket was not modified.
        </span>
      </div>
    </>
  );
}
