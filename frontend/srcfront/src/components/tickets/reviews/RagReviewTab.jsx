import { TbRobot, TbAlertTriangle, TbPin } from 'react-icons/tb';
import { Spinner, ErrorBox, ResultPreview, Section } from '../TicketDetailUi';
import { CONFIDENCE_CONFIG } from '../Ticketconstants';

/** Bouton "Run RAG Review" + aperçu du résultat, affiché dans le panneau latéral. */
export function RagReviewPanel({ ragLoading, ragError, ragResult, onRun, onOpen }) {
  return (
    <>
      <button onClick={onRun} disabled={ragLoading} style={{
        width: '100%', padding: '13px',
        background: ragLoading ? 'rgba(167,139,250,0.05)' : 'rgba(167,139,250,0.12)',
        border: '1px solid rgba(167,139,250,0.35)', borderRadius: '12px',
        color: '#c4b5fd', fontSize: '14px', fontWeight: 600,
        cursor: ragLoading ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
      }}>
        {ragLoading ? <Spinner color="#c4b5fd" /> : <TbRobot size={16} />}
        {ragLoading ? 'Running RAG Review...' : 'Run RAG Review'}
      </button>
      {ragError && <ErrorBox>{ragError}</ErrorBox>}
      {ragResult && (
        <ResultPreview
          color="#c4b5fd"
          label={`RAG Review — ${ragResult.status}`}
          confidence={ragResult.result?.confidence}
          confConfig={CONFIDENCE_CONFIG}
          needsHuman={ragResult.result?.needsHumanReview}
          onOpen={onOpen}
        />
      )}
    </>
  );
}

/** Contenu détaillé affiché dans la modale plein écran. */
export function RagReviewModalContent({ ragResult }) {
  if (!ragResult) return null;
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#c4b5fd', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbRobot size={14} /> RAG REVIEW — {ragResult.status}
      </div>
      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        {ragResult.result?.confidence && (
          <span style={{
            fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px',
            color: CONFIDENCE_CONFIG[ragResult.result.confidence]?.color ?? '#a78bfa',
            background: `${CONFIDENCE_CONFIG[ragResult.result.confidence]?.color ?? '#a78bfa'}18`,
            border: `1px solid ${CONFIDENCE_CONFIG[ragResult.result.confidence]?.color ?? '#a78bfa'}33`,
          }}>
            {ragResult.result.confidence}
          </span>
        )}
        {ragResult.result?.needsHumanReview && (
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px', color: '#f87171', background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.3)' }}>
            <TbAlertTriangle size={11} /> Revue humaine
          </span>
        )}
      </div>
      {(ragResult.modelName || ragResult.promptVersion) && (
        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '16px', display: 'flex', gap: '10px' }}>
          {ragResult.modelName     && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><TbRobot size={12} /> {ragResult.modelName}</span>}
          {ragResult.promptVersion && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              <TbPin size={12} /> {ragResult.promptVersion}
            </span>
          )}
        </div>
      )}
      {ragResult.result?.summary && (
        <Section title="Résumé">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{ragResult.result.summary}</p>
        </Section>
      )}

      {ragResult.result?.possibleCauses?.length > 0 && (
        <Section title="Causes possibles">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {ragResult.result.possibleCauses.map((c, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
            ))}
          </ul>
        </Section>
      )}

      {ragResult.result?.recommendedChecks?.length > 0 && (
        <Section title="Vérifications recommandées">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {ragResult.result.recommendedChecks.map((c, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
            ))}
          </ul>
        </Section>
      )}

      {ragResult.result?.draftResponse && (
        <Section title="Réponse suggérée">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{ragResult.result.draftResponse}</p>
        </Section>
      )}

      {ragResult.result?.evidenceRefs?.length > 0 && (
        <Section title="Sources citées">
          {ragResult.result.evidenceRefs.map((ref, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'flex-start', gap: '8px',
              padding: '8px 12px', borderRadius: '10px', marginBottom: '6px',
              background: 'rgba(167,139,250,0.07)', border: '1px solid rgba(167,139,250,0.2)',
            }}>
              <span style={{ fontSize: '11px', fontWeight: 700, color: '#a78bfa', marginTop: '1px', flexShrink: 0 }}>#{i + 1}</span>
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

      {ragResult.result?.limitations?.length > 0 && (
        <Section title="Limitations">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {ragResult.result.limitations.map((l, i) => (
              <li key={i} style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{l}</li>
            ))}
          </ul>
        </Section>
      )}

      {ragResult.retrievedEvidence?.length > 0 && (
        <div style={{ marginTop: '16px' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
            Chunks récupérés ({ragResult.retrievedEvidence.length})
          </div>
          {ragResult.retrievedEvidence.map((e, i) => (
            <div key={i} style={{ fontSize: '12px', color: 'var(--text-muted)', padding: '8px 12px', borderLeft: '2px solid rgba(167,139,250,0.4)', marginBottom: '8px', lineHeight: 1.6 }}>
              {e.articleTitle && <strong style={{ color: 'var(--text-main)', display: 'block', marginBottom: '2px' }}>{e.articleTitle}</strong>}
              {e.content ?? e.text ?? ''}
            </div>
          ))}
        </div>
      )}
    </>
  );
}