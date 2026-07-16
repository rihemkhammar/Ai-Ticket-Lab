import { TbClipboardCheck, TbAlertTriangle, TbTool } from 'react-icons/tb';
import { Section, Badge } from './HitlUi';
import { CONFIDENCE_CONFIG, CHECKPOINT_STATUS_CONFIG, MAX_REVISION_CYCLES } from './Hitlconstants';
import HitlRevisionForm from './HitlRevisionForm';

/**
 * Displays a WAITING_FOR_HUMAN checkpoint: the AI draft, evidence refs,
 * previous review summary, tool-call trace, limitations, and the
 * approve/reject/request-revision decision form (S5 §7.4).
 */
export default function HitlPendingReview({ review, onDecide, deciding }) {
  const checkpointConfig = CHECKPOINT_STATUS_CONFIG[review.checkpointStatus] ?? { label: review.checkpointStatus, color: '#facc15' };
  const revisionDisabled = (review.checkpointNumber ?? 1) > MAX_REVISION_CYCLES;

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#facc15', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbClipboardCheck size={14} /> HUMAN REVIEW — CHECKPOINT #{review.checkpointNumber ?? 1}
      </div>

      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        <Badge color={checkpointConfig.color}>{checkpointConfig.label}</Badge>
        {review.confidence && (
          <Badge color={CONFIDENCE_CONFIG[review.confidence]?.color ?? '#facc15'}>
            {review.confidence}
          </Badge>
        )}
        <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px', color: '#f87171', background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.3)' }}>
          <TbAlertTriangle size={11} /> Human review required
        </span>
      </div>

      {review.toolCalls?.length > 0 && (
        <Section title="Tool-call trace">
          {review.toolCalls.map((tc, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '8px 12px', borderRadius: '10px', marginBottom: '6px',
              background: tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.06)' : 'rgba(248,113,113,0.06)',
              border: `1px solid ${tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.25)' : 'rgba(248,113,113,0.25)'}`,
            }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', fontFamily: 'monospace' }}>
                <TbTool size={12} style={{ marginRight: '6px', verticalAlign: 'middle' }} />{tc.toolName}
              </span>
              <span style={{ fontSize: '11px', fontWeight: 700, color: tc.status === 'SUCCESS' ? '#4ade80' : '#f87171' }}>
                {tc.status}
              </span>
            </div>
          ))}
        </Section>
      )}

      {review.investigationSummary && (
        <Section title="Investigation Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{review.investigationSummary}</p>
        </Section>
      )}

      {review.previousReviewSummary && (
        <Section title="Previous Review Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{review.previousReviewSummary}</p>
        </Section>
      )}

      {review.recommendedNextSteps?.length > 0 && (
        <Section title="Recommended Next Steps">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {review.recommendedNextSteps.map((step, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{step}</li>
            ))}
          </ul>
        </Section>
      )}

      {review.draftTechnicianResponse && (
        <Section title="Draft Technician Response">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{review.draftTechnicianResponse}</p>
        </Section>
      )}

      {review.evidenceRefs?.length > 0 && (
        <Section title="Evidence References">
          {review.evidenceRefs.map((ref, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'flex-start', gap: '8px',
              padding: '8px 12px', borderRadius: '10px', marginBottom: '6px',
              background: 'rgba(250,204,21,0.06)', border: '1px solid rgba(250,204,21,0.2)',
            }}>
              <span style={{ fontSize: '11px', fontWeight: 700, color: '#facc15', marginTop: '1px', flexShrink: 0 }}>#{i + 1}</span>
              <div>
                {ref.articleTitle && (
                  <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '2px' }}>{ref.articleTitle}</div>
                )}
                <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{ref.sourceRef}</div>
              </div>
            </div>
          ))}
        </Section>
      )}

      {review.limitations?.length > 0 && (
        <Section title="Limitations">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {review.limitations.map((l, i) => (
              <li key={i} style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{l}</li>
            ))}
          </ul>
        </Section>
      )}

      <div style={{ marginTop: '4px', padding: '12px', background: 'rgba(250,204,21,0.1)', border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
        <TbAlertTriangle size={14} color="#facc15" />
        <span style={{ fontSize: '13px', color: '#facc15' }}>
          This is an AI draft. It has no effect until a human makes a decision. Approval does not close the ticket.
        </span>
      </div>

      <HitlRevisionForm
        onDecide={onDecide}
        deciding={deciding}
        revisionDisabled={revisionDisabled}
      />
    </>
  );
}
