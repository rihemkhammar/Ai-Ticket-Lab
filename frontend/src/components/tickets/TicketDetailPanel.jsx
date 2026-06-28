import { useState, useEffect } from 'react';
import { TbRefresh, TbCircleCheck, TbBrain, TbX, TbRobot, TbSearch, TbAlertTriangle, TbCheck,TbPin } from 'react-icons/tb';
import { STATUS_CONFIG } from './Ticketconstants';
import { updateTicketStatus, runAiReview, runRagReview, getTicketEvidence } from '../../services/api';

const CONFIDENCE_CONFIG = {
  LOW:    { label: 'Low',    color: '#f87171' },
  MEDIUM: { label: 'Medium', color: '#facc15' },
  HIGH:   { label: 'High',   color: '#4ade80' },
};

const TABS = ['AI Review', 'RAG Review', 'Evidence'];

export default function TicketDetailPanel({ ticket, onClose, onStatusUpdated }) {
  const [activeTab,       setActiveTab]       = useState('AI Review');
  const [modalOpen,       setModalOpen]       = useState(false);

  const [aiResult,        setAiResult]        = useState(null);
  const [aiLoading,       setAiLoading]       = useState(false);
  const [aiError,         setAiError]         = useState(null);

  const [ragResult,       setRagResult]       = useState(null);
  const [ragLoading,      setRagLoading]      = useState(false);
  const [ragError,        setRagError]        = useState(null);

  const [evidenceResult,  setEvidenceResult]  = useState(null);
  const [evidenceLoading, setEvidenceLoading] = useState(false);
  const [evidenceError,   setEvidenceError]   = useState(null);

  useEffect(() => {
    setAiResult(null);      setAiError(null);      setAiLoading(false);
    setRagResult(null);     setRagError(null);     setRagLoading(false);
    setEvidenceResult(null);setEvidenceError(null);setEvidenceLoading(false);
    setModalOpen(false);
    setActiveTab('AI Review');
  }, [ticket?.id]);

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
    setAiLoading(true); setAiError(null); setAiResult(null);
    try {
      const data = await runAiReview(ticket.id);
      setAiResult(data);
      setModalOpen(true);
    } catch (err) {
      setAiError(err.response?.data?.message ?? 'AI review failed.');
    } finally { setAiLoading(false); }
  };

  const handleRagReview = async () => {
    setRagLoading(true); setRagError(null); setRagResult(null);
    try {
      const data = await runRagReview(ticket.id);
      setRagResult(data);
      setModalOpen(true);
    } catch (err) {
      setRagError(err.response?.data?.message ?? 'RAG review échouée.');
    } finally { setRagLoading(false); }
  };

  const handleEvidence = async () => {
    setEvidenceLoading(true); setEvidenceError(null); setEvidenceResult(null);
    try {
      const data = await getTicketEvidence(ticket.id);
      setEvidenceResult(data);
      setModalOpen(true);
    } catch (err) {
      setEvidenceError(err.response?.data?.message ?? "Impossible de récupérer l'evidence.");
    } finally { setEvidenceLoading(false); }
  };

  const hasResult = {
    'AI Review':  !!aiResult || !!aiError,
    'RAG Review': !!ragResult || !!ragError,
    'Evidence':   !!evidenceResult || !!evidenceError,
  };

  const anyLoading = aiLoading || ragLoading || evidenceLoading;

  return (
    <>
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
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', padding: 0 }}>
            <TbX size={20} />
          </button>
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

        {/* Status actions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
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
        </div>

        <div style={{ borderTop: '1px solid rgba(77,124,199,0.1)', marginBottom: '16px' }} />

        {/* Onglets */}
        <div style={{ display: 'flex', borderRadius: '12px', background: 'rgba(77,124,199,0.06)', padding: '4px', gap: '4px', marginBottom: '14px' }}>
          {TABS.map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                flex: 1, padding: '8px 4px', borderRadius: '9px', fontSize: '12px', fontWeight: 600,
                border: 'none', cursor: 'pointer', transition: 'all 0.15s', position: 'relative',
                background: activeTab === tab ? 'var(--bg-card)' : 'transparent',
                color: activeTab === tab ? 'var(--text-main)' : 'var(--text-muted)',
                boxShadow: activeTab === tab ? '0 1px 4px rgba(0,0,0,0.15)' : 'none',
              }}
            >
              {tab}
              {hasResult[tab] && (
                <span style={{
                  position: 'absolute', top: '6px', right: '6px',
                  width: '6px', height: '6px', borderRadius: '50%',
                  background: '#4ade80',
                }} />
              )}
            </button>
          ))}
        </div>

        {/* Bouton AI Review */}
        {activeTab === 'AI Review' && (
          <>
            <button onClick={handleAiReview} disabled={aiLoading} style={{
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
                onOpen={() => setModalOpen(true)}
              />
            )}
          </>
        )}

        {/* Bouton RAG Review */}
        {activeTab === 'RAG Review' && (
          <>
            <button onClick={handleRagReview} disabled={ragLoading} style={{
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
                onOpen={() => setModalOpen(true)}
              />
            )}
          </>
        )}

        {/* Bouton Evidence */}
        {activeTab === 'Evidence' && (
          <>
            <button onClick={handleEvidence} disabled={evidenceLoading} style={{
              width: '100%', padding: '13px',
              background: evidenceLoading ? 'rgba(96,165,250,0.05)' : 'rgba(96,165,250,0.10)',
              border: '1px solid rgba(96,165,250,0.3)', borderRadius: '12px',
              color: '#60a5fa', fontSize: '14px', fontWeight: 600,
              cursor: evidenceLoading ? 'not-allowed' : 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
            }}>
              {evidenceLoading ? <Spinner color="#60a5fa" /> : <TbSearch size={16} />}
              {evidenceLoading ? 'Fetching Evidence...' : 'Get Evidence'}
            </button>
            {evidenceError && <ErrorBox>{evidenceError}</ErrorBox>}
            {evidenceResult && (
              <ResultPreview
                color="#60a5fa"
                label={`Evidence — ${evidenceResult.evidence?.length ?? 0} chunks`}
                onOpen={() => setModalOpen(true)}
              />
            )}
          </>
        )}
      </div>

      {/* ── Modal plein écran ── */}
      {modalOpen && (
        <div
          onClick={() => setModalOpen(false)}
          style={{
            position: 'fixed', inset: 0, zIndex: 1000,
            background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            padding: '24px',
          }}
        >
          <div
            onClick={e => e.stopPropagation()}
            style={{
              background: 'var(--bg-card)',
              border: '1px solid rgba(77,124,199,0.2)',
              borderRadius: '24px',
              width: '100%', maxWidth: '700px',
              maxHeight: '85vh',
              display: 'flex', flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            {/* Modal header */}
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid rgba(77,124,199,0.1)',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              flexShrink: 0,
            }}>
              <div style={{ display: 'flex', gap: '4px', background: 'rgba(77,124,199,0.06)', borderRadius: '10px', padding: '3px' }}>
                {TABS.filter(t => hasResult[t]).map(tab => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    style={{
                      padding: '6px 14px', borderRadius: '8px', fontSize: '12px', fontWeight: 600,
                      border: 'none', cursor: 'pointer', transition: 'all 0.15s',
                      background: activeTab === tab ? 'var(--bg-card)' : 'transparent',
                      color: activeTab === tab ? 'var(--text-main)' : 'var(--text-muted)',
                      boxShadow: activeTab === tab ? '0 1px 4px rgba(0,0,0,0.15)' : 'none',
                    }}
                  >
                    {tab}
                  </button>
                ))}
              </div>
              <button
                onClick={() => setModalOpen(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center' }}
              >
                <TbX size={20} />
              </button>
            </div>

            {/* Modal body — scrollable */}
            <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>

              {/* Contenu AI Review */}
              {activeTab === 'AI Review' && aiResult?.result && (
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
              )}

              {/* Contenu RAG Review */}
              {activeTab === 'RAG Review' && ragResult && (
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
                  {ragResult.result?.summary        && <Section title="Résumé"><p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{ragResult.result.summary}</p></Section>}
                  {ragResult.result?.diagnosis      && <Section title="Diagnostic"><p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{ragResult.result.diagnosis}</p></Section>}
                  {ragResult.result?.recommendation && <Section title="Recommandation"><p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{ragResult.result.recommendation}</p></Section>}
                  {ragResult.result?.limitations    && <Section title="Limitations"><p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0 }}>{ragResult.result.limitations}</p></Section>}
                  {ragResult.retrievedEvidence?.length > 0 && (
                    <div style={{ marginTop: '16px' }}>
                      <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
                        Evidence utilisée ({ragResult.retrievedEvidence.length} chunks)
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
              )}

              {/* Contenu Evidence */}
              {activeTab === 'Evidence' && evidenceResult && (
                <>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#60a5fa', letterSpacing: '0.5px', marginBottom: '16px' }}>
                    <TbSearch size={14} /> EVIDENCE — {evidenceResult.evidence?.length ?? 0} chunks
                  </div>
                  {(evidenceResult.evidence ?? []).length === 0 ? (
                    <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Aucun chunk trouvé.</p>
                  ) : (
                    (evidenceResult.evidence ?? []).map((chunk, i) => (
                      <div key={i} style={{
                        marginBottom: '12px', padding: '12px 14px', borderRadius: '12px',
                        background: 'rgba(96,165,250,0.05)', border: '1px solid rgba(96,165,250,0.15)',
                      }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                          <span style={{ fontSize: '11px', fontWeight: 600, color: '#60a5fa' }}>Chunk #{i + 1}</span>
                          {chunk.score !== undefined && (
                            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                              score: {typeof chunk.score === 'number' ? chunk.score.toFixed(4) : chunk.score}
                            </span>
                          )}
                        </div>
                        {chunk.articleTitle && (
                          <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>{chunk.articleTitle}</div>
                        )}
                        <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: 0, lineHeight: 1.6 }}>
                          {chunk.content ?? chunk.text ?? JSON.stringify(chunk)}
                        </p>
                      </div>
                    ))
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </>
  );
}

/* ── Sub-components ── */

function ResultPreview({ color, label, confidence, confConfig, needsHuman, onOpen }) {
  return (
    <div style={{
      marginTop: '12px', padding: '12px 14px', borderRadius: '12px',
      background: `${color}08`, border: `1px solid ${color}22`,
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', fontWeight: 600, color }}>
          <TbCheck size={13} /> {label}
        </span>
        <div style={{ display: 'flex', gap: '6px' }}>
          {confidence && (
            <span style={{ fontSize: '11px', color: confConfig[confidence]?.color ?? color }}>
              {confConfig[confidence]?.label ?? confidence}
            </span>
          )}
          {needsHuman && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px', color: '#f87171' }}>
              <TbAlertTriangle size={11} /> Revue humaine
            </span>
          )}
        </div>
      </div>
      <button onClick={onOpen} style={{
        fontSize: '12px', fontWeight: 600, padding: '5px 12px',
        borderRadius: '8px', border: `1px solid ${color}44`,
        background: `${color}14`, color, cursor: 'pointer',
      }}>
        Voir →
      </button>
    </div>
  );
}

function Spinner({ color }) {
  return (
    <span style={{
      width: '14px', height: '14px', border: `2px solid ${color}`,
      borderTopColor: 'transparent', borderRadius: '50%',
      display: 'inline-block', animation: 'spin 0.8s linear infinite',
    }} />
  );
}

function ErrorBox({ children }) {
  return (
    <div style={{ marginTop: '12px', padding: '12px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
      <TbAlertTriangle size={14} color="#f87171" />
      <span style={{ fontSize: '13px', color: '#f87171' }}>{children}</span>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '16px' }}>
      <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '6px', textTransform: 'uppercase' }}>
        {title}
      </div>
      {children}
    </div>
  );
}