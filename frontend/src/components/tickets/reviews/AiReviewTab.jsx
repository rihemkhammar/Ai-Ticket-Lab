import { TbBrain, TbAlertTriangle } from 'react-icons/tb';
import { Spinner, ErrorBox, ResultPreview, Section } from '../TicketDetailUi';
import { CONFIDENCE_CONFIG } from '../Ticketconstants';

/** Bouton "Run AI Review" + aperçu du résultat, affiché dans le panneau latéral. */
export function AiReviewPanel({ aiLoading, aiError, aiResult, onRun, onOpen }) {
  return (
    <>
      <button onClick={onRun} disabled={aiLoading} style={{
        width: '100%', padding: '13px',
        background: aiLoading ? 'rgba(139,92,246,0.1)' : 'rgba(139,92,246,0.15)',
        border: '1px solid rgba(139,92,246,0.3)', borderRadius: '12px',
        color: '#a78bfa', fontSize: '14px', fontWeight: 600,
        cursor: aiLoading ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
      }}>
        {aiLoading ? <Spinner color="#a78bfa" /> : <TbBrain size={16} />}
        {aiLoading ? 'Running AI Review...' : 'Run AI Review'}
      </button>
      {aiError && <ErrorBox>{aiError}</ErrorBox>}
      {aiResult && (
        <ResultPreview
          color="#a78bfa"
          label={`AI Review — ${aiResult.status}`}
          confidence={aiResult.result?.confidence}
          confConfig={CONFIDENCE_CONFIG}
          onOpen={onOpen}
        />
      )}
    </>
  );
}

/** Contenu détaillé affiché dans la modale plein écran. */
export function AiReviewModalContent({ aiResult }) {
  if (!aiResult?.result) return null;
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#a78bfa', letterSpacing: '0.5px', marginBottom: '16px' }}>
        <TbBrain size={14} /> AI REVIEW — {aiResult.status}
      </div>
      <Section title="Summary">
        <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{aiResult.result.summary}</p>
      </Section>
      <Section title="Possible Causes">
        <ul style={{ margin: 0, paddingLeft: '18px' }}>
          {aiResult.result.possibleCauses?.map((c, i) => (
            <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
          ))}
        </ul>
      </Section>
      <Section title="Recommended Checks">
        <ul style={{ margin: 0, paddingLeft: '18px' }}>
          {aiResult.result.recommendedChecks?.map((c, i) => (
            <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
          ))}
        </ul>
      </Section>
      <Section title="Draft Response">
        <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{aiResult.result.draftResponse}</p>
      </Section>
      {aiResult.result.limitations?.length > 0 && (
        <Section title="Limitations">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {aiResult.result.limitations.map((l, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{l}</li>
            ))}
          </ul>
        </Section>
      )}
      {aiResult.result.needsHumanReview && (
        <div style={{ marginTop: '12px', padding: '12px', background: 'rgba(250,204,21,0.1)', border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <TbAlertTriangle size={14} color="#facc15" />
          <span style={{ fontSize: '13px', color: '#facc15' }}>AI output is advisory. Human review is required before taking action.</span>
        </div>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px', padding: '10px 14px', background: 'rgba(77,124,199,0.05)', borderRadius: '10px' }}>
        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Confidence</span>
        <span style={{ fontSize: '13px', fontWeight: 700, color: CONFIDENCE_CONFIG[aiResult.result.confidence]?.color ?? '#a78bfa' }}>
          {CONFIDENCE_CONFIG[aiResult.result.confidence]?.label ?? aiResult.result.confidence}
        </span>
      </div>
    </>
  );
}