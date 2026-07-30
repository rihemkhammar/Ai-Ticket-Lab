import { TbCircleCheck, TbUserCheck, TbShieldCheck, TbLock } from 'react-icons/tb';
import { Section, Badge } from './HitlUi';
import { CONFIDENCE_CONFIG } from './Hitlconstants';

/**
 * Displays the finalized, human-approved result (S5 §7.5 / §6.6).
 * Always shows humanReviewed=true, officialActionExecuted=false,
 * ticketStatusChanged=false, and an explicit note that approval does
 * NOT close the ticket or mean maintenance work was completed.
 */
export default function HitlFinalizedResult({ result, humanComment, finalizedAt }) {
  if (!result) return null;

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#4ade80', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbCircleCheck size={14} /> FINALIZED — REVIEWED BY HUMAN
      </div>

      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        {result.confidence && (
          <Badge color={CONFIDENCE_CONFIG[result.confidence]?.color ?? '#4ade80'}>{result.confidence}</Badge>
        )}
        <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px', color: '#4ade80', background: 'rgba(74,222,128,0.1)', border: '1px solid rgba(74,222,128,0.3)' }}>
          <TbUserCheck size={11} /> This AI recommendation was reviewed by a human
        </span>
      </div>

      {result.investigationSummary && (
        <Section title="Investigation Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{result.investigationSummary}</p>
        </Section>
      )}

      {result.recommendedNextSteps?.length > 0 && (
        <Section title="Recommended Next Steps">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {result.recommendedNextSteps.map((step, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{step}</li>
            ))}
          </ul>
        </Section>
      )}

      {result.draftTechnicianResponse && (
        <Section title="Draft Technician Response">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{result.draftTechnicianResponse}</p>
        </Section>
      )}

      {result.evidenceRefs?.length > 0 && (
        <Section title="Evidence References">
          {result.evidenceRefs.map((ref, i) => (
            <div key={i} style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace', marginBottom: '4px' }}>
              #{i + 1} {ref.articleTitle ? `${ref.articleTitle} — ` : ''}{ref.sourceRef}
            </div>
          ))}
        </Section>
      )}

      {result.limitations?.length > 0 && (
        <Section title="Limitations">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {result.limitations.map((l, i) => (
              <li key={i} style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{l}</li>
            ))}
          </ul>
        </Section>
      )}

      {humanComment && (
        <Section title="Reviewer Comment">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{humanComment}</p>
        </Section>
      )}

      <div style={{ marginTop: '4px', padding: '12px', background: 'rgba(74,222,128,0.08)', border: '1px solid rgba(74,222,128,0.25)', borderRadius: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
          <TbShieldCheck size={14} color="#4ade80" />
          <span style={{ fontSize: '12px', fontWeight: 700, color: '#4ade80' }}>humanReviewed = true</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
          <TbLock size={14} color="#94a3b8" />
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>officialActionExecuted = false</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <TbLock size={14} color="#94a3b8" />
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>ticketStatusChanged = false</span>
        </div>
      </div>

      <p style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6, marginTop: '12px' }}>
        Approval only means the AI draft was reviewed and accepted by a human.
        It does not close the ticket and does not mean the maintenance action was performed.
        {finalizedAt && <> Finalized on {new Date(finalizedAt).toLocaleString()}.</>}
      </p>
    </>
  );
}
