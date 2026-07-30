import { TbTool, TbRobot, TbPin, TbAlertTriangle } from 'react-icons/tb';
import { Spinner, ErrorBox, ResultPreview, Section } from '../TicketDetailUi';
import { CONFIDENCE_CONFIG } from '../Ticketconstants';

/** Bouton "Run Agent Assistant" + aperçu du résultat, affiché dans le panneau latéral. */
export function AgentPanel({ agentLoading, agentError, agentResult, onRun, onOpen }) {
  return (
    <>
      <button onClick={onRun} disabled={agentLoading} style={{
        width: '100%', padding: '13px',
        background: agentLoading ? 'rgba(250,204,21,0.05)' : 'rgba(250,204,21,0.10)',
        border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px',
        color: '#facc15', fontSize: '14px', fontWeight: 600,
        cursor: agentLoading ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
      }}>
        {agentLoading ? <Spinner color="#facc15" /> : <TbTool size={16} />}
        {agentLoading ? "Agent en cours d'investigation..." : 'Run Agent Assistant'}
      </button>
      {agentError && <ErrorBox>{agentError}</ErrorBox>}
      {agentResult && (
        <ResultPreview
          color="#facc15"
          label={`Agent — ${agentResult.status}`}
          confidence={agentResult.confidence}
          confConfig={CONFIDENCE_CONFIG}
          needsHuman={agentResult.needsHumanReview}
          onOpen={onOpen}
        />
      )}
    </>
  );
}

/** Contenu détaillé affiché dans la modale plein écran. */
export function AgentModalContent({ agentResult }) {
  if (!agentResult) return null;
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#facc15', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbTool size={14} /> AGENT INVESTIGATION — {agentResult.status}
      </div>

      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        {agentResult.confidence && (
          <span style={{
            fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px',
            color: CONFIDENCE_CONFIG[agentResult.confidence]?.color ?? '#facc15',
            background: `${CONFIDENCE_CONFIG[agentResult.confidence]?.color ?? '#facc15'}18`,
            border: `1px solid ${CONFIDENCE_CONFIG[agentResult.confidence]?.color ?? '#facc15'}33`,
          }}>
            {agentResult.confidence}
          </span>
        )}
        {agentResult.needsHumanReview && (
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px', color: '#f87171', background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.3)' }}>
            <TbAlertTriangle size={11} /> Revue humaine
          </span>
        )}
      </div>

      {(agentResult.modelName || agentResult.promptVersion) && (
        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '16px', display: 'flex', gap: '10px' }}>
          {agentResult.modelName     && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><TbRobot size={12} /> {agentResult.modelName}</span>}
          {agentResult.promptVersion && <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><TbPin size={12} /> {agentResult.promptVersion}</span>}
        </div>
      )}

      {/* Tool-call trace — operational only, never chain-of-thought */}
      {agentResult.toolCalls?.length > 0 && (
        <Section title="Tool-call trace">
          {agentResult.toolCalls.map((tc, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '8px 12px', borderRadius: '10px', marginBottom: '6px',
              background: tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.06)' : 'rgba(248,113,113,0.06)',
              border: `1px solid ${tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.25)' : 'rgba(248,113,113,0.25)'}`,
            }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', fontFamily: 'monospace' }}>{tc.toolName}</span>
              <span style={{
                fontSize: '11px', fontWeight: 700,
                color: tc.status === 'SUCCESS' ? '#4ade80' : '#f87171',
              }}>
                {tc.status}
              </span>
            </div>
          ))}
        </Section>
      )}

      {agentResult.status === 'FAILED' && agentResult.errorMessage && (
        <ErrorBox>{agentResult.errorMessage}</ErrorBox>
      )}

      {agentResult.investigationSummary && (
        <Section title="Investigation Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{agentResult.investigationSummary}</p>
        </Section>
      )}

      {agentResult.previousReviewSummary && (
        <Section title="Previous Review Summary">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{agentResult.previousReviewSummary}</p>
        </Section>
      )}

      {agentResult.recommendedNextSteps?.length > 0 && (
        <Section title="Recommended Next Steps">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {agentResult.recommendedNextSteps.map((c, i) => (
              <li key={i} style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7 }}>{c}</li>
            ))}
          </ul>
        </Section>
      )}

      {agentResult.draftTechnicianResponse && (
        <Section title="Draft Technician Response">
          <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>{agentResult.draftTechnicianResponse}</p>
        </Section>
      )}

      {agentResult.evidenceRefs?.length > 0 && (
        <Section title="Evidence References">
          {agentResult.evidenceRefs.map((ref, i) => (
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

      {agentResult.limitations?.length > 0 && (
        <Section title="Limitations">
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            {agentResult.limitations.map((l, i) => (
              <li key={i} style={{ fontSize: '12px', color: 'var(--text-muted)', lineHeight: 1.6 }}>{l}</li>
            ))}
          </ul>
        </Section>
      )}

      {agentResult.needsHumanReview && (
        <div style={{ marginTop: '12px', padding: '12px', background: 'rgba(250,204,21,0.1)', border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <TbAlertTriangle size={14} color="#facc15" />
          <span style={{ fontSize: '13px', color: '#facc15' }}>AI output is advisory. Human review is required before taking action.</span>
        </div>
      )}
    </>
  );
}