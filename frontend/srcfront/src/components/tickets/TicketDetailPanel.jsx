import { useState, useEffect } from 'react';

import { TbRefresh, TbCircleCheck, TbX } from 'react-icons/tb';
import { STATUS_CONFIG } from './Ticketconstants';
import {
  updateTicketStatus, runAiReview, runRagReview, getTicketEvidence, runAgentInvestigation,
  getLatestAiReview, getLatestRagReview, getLatestAgentRun,
  getLatestHitlReview, runHitlReview, submitHumanDecision,
  getAgentRunTrace,
} from '../../services/api';

import { AiReviewPanel, AiReviewModalContent } from './reviews/AiReviewTab';
import { RagReviewPanel, RagReviewModalContent } from './reviews/RagReviewTab';
import { EvidencePanel, EvidenceModalContent } from './reviews/EvidenceTab';
import { AgentPanel, AgentModalContent } from './reviews/AgentTab';
import { HitlPanel } from './hitl/HitlPanel';
import { HitlModalContent } from './hitl/Hitlmodalcontent';
import TraceButton from './trace/TraceButton';
import TraceModalContent from './trace/TraceModalContent';

const TABS = ['AI Review', 'RAG Review', 'Evidence', 'Agent', 'HITL'];

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

  const [agentResult,     setAgentResult]     = useState(null);
  const [agentLoading,    setAgentLoading]    = useState(false);
  const [agentError,      setAgentError]      = useState(null);

  const [hitlResult,      setHitlResult]      = useState(null);
  const [hitlLoading,     setHitlLoading]     = useState(false);
  const [hitlError,       setHitlError]       = useState(null);
  const [hitlDeciding,    setHitlDeciding]     = useState(null); // 'APPROVE'|'REJECT'|'REQUEST_REVISION'|null

  const [traceResult,     setTraceResult]     = useState(null);
  const [traceLoading,    setTraceLoading]    = useState(false);
  const [traceError,      setTraceError]      = useState(null);
  const [traceModalOpen,  setTraceModalOpen]  = useState(false);

  // On ticket change, reload any previously-stored results from
  // the backend instead of just wiping them to null. Evidence is cheap to
  // recompute live and isn't persisted, so it stays reset until the user
  // clicks "Get Evidence" again.
  useEffect(() => {
    let cancelled = false;

    setAiError(null);       setAiLoading(false);
    setRagError(null);      setRagLoading(false);
    setEvidenceResult(null);setEvidenceError(null);setEvidenceLoading(false);
    setAgentError(null);    setAgentLoading(false);
    setHitlResult(null);
    setHitlError(null);     setHitlLoading(false); setHitlDeciding(null);
    setTraceResult(null);   setTraceError(null);   setTraceLoading(false); setTraceModalOpen(false);
    setModalOpen(false);
    setActiveTab('AI Review');

    if (!ticket?.id) {
      setAiResult(null); setRagResult(null); setAgentResult(null);
      return;
    }

    setAiResult(null); setRagResult(null); setAgentResult(null);

    Promise.allSettled([
      getLatestAiReview(ticket.id),
      getLatestRagReview(ticket.id),
      getLatestAgentRun(ticket.id),
      getLatestHitlReview(ticket.id),
    ]).then(([ai, rag, agent, hitl]) => {
      if (cancelled) return;
      if (ai.status === 'fulfilled' && ai.value) setAiResult(ai.value);
      if (rag.status === 'fulfilled' && rag.value) setRagResult(rag.value);
      if (agent.status === 'fulfilled' && agent.value) setAgentResult(agent.value);
      if (hitl.status === 'fulfilled' && hitl.value) setHitlResult(hitl.value);
    });

    return () => { cancelled = true; };

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

  const handleAgentInvestigation = async () => {
    setAgentLoading(true); setAgentError(null); setAgentResult(null);
    try {
      const data = await runAgentInvestigation(ticket.id);
      setAgentResult(data);
      setModalOpen(true);
    } catch (err) {
      // Even a FAILED agent run (422) carries a structured body with a
      // tool-call trace — show it like a normal result rather than a
      // generic error when possible.
      const body = err.response?.data;
      if (body && (body.toolCalls || body.status)) {
        setAgentResult(body);
        setModalOpen(true);
      } else {
        setAgentError(body?.message ?? "L'investigation de l'agent a échoué.");
      }
    } finally { setAgentLoading(false); }
  };

  const handleRunHitl = async () => {
    setHitlLoading(true); setHitlError(null);
    try {
      const data = await runHitlReview(ticket.id);
      setHitlResult(data);
      setModalOpen(true);
      if (data?.status === 'FAILED') {
        setHitlError(data.errorMessage ?? "The HITL agent failed.");
      }
    } catch (err) {
      setHitlError(err.response?.data?.message ?? "Could not start the HITL review.");
    } finally { setHitlLoading(false); }
  };

  const handleHitlDecide = async (decision, comment) => {
    if (!hitlResult?.runId) return;
    setHitlDeciding(decision); setHitlError(null);
    try {
      const decisionResponse = await submitHumanDecision(hitlResult.runId, decision, comment);
      if (decision === 'REQUEST_REVISION' && decisionResponse.revisedReview) {
        setHitlResult(decisionResponse.revisedReview);
      } else {
        // APPROVE / REJECT -> reload the authoritative persisted state
        const reloaded = await getLatestHitlReview(ticket.id);
        setHitlResult(reloaded);
      }
    } catch (err) {
      setHitlError(err.response?.data?.message ?? 'The human decision failed.');
    } finally { setHitlDeciding(null); }
  };

  const handleViewTrace = async (runId) => {
    if (!runId) return;
    setTraceLoading(true); setTraceError(null); setTraceResult(null);
    try {
      const data = await getAgentRunTrace(runId);
      setTraceResult(data);
      setTraceModalOpen(true);
    } catch (err) {
      setTraceError(err.response?.data?.message ?? 'Could not load the AI trace.');
      setTraceModalOpen(true);
    } finally {
      setTraceLoading(false);
    }
  };

  const hasResult = {
    'AI Review':  !!aiResult || !!aiError,
    'RAG Review': !!ragResult || !!ragError,
    'Evidence':   !!evidenceResult || !!evidenceError,
    'Agent':      !!agentResult || !!agentError,
    'HITL':       !!hitlResult,
  };

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

        {activeTab === 'AI Review' && (
          <AiReviewPanel
            aiLoading={aiLoading}
            aiError={aiError}
            aiResult={aiResult}
            onRun={handleAiReview}
            onOpen={() => setModalOpen(true)}
          />
        )}

        {activeTab === 'RAG Review' && (
          <RagReviewPanel
            ragLoading={ragLoading}
            ragError={ragError}
            ragResult={ragResult}
            onRun={handleRagReview}
            onOpen={() => setModalOpen(true)}
          />
        )}

        {activeTab === 'Evidence' && (
          <EvidencePanel
            evidenceLoading={evidenceLoading}
            evidenceError={evidenceError}
            evidenceResult={evidenceResult}
            onRun={handleEvidence}
            onOpen={() => setModalOpen(true)}
          />
        )}

        {activeTab === 'Agent' && (
          <>
            <AgentPanel
              agentLoading={agentLoading}
              agentError={agentError}
              agentResult={agentResult}
              onRun={handleAgentInvestigation}
              onOpen={() => setModalOpen(true)}
            />
            {agentResult?.runId && (
              <TraceButton onClick={() => handleViewTrace(agentResult.runId)} loading={traceLoading} />
            )}
          </>
        )}

        {/* Human-in-the-Loop Agent Review (S5) */}
        {activeTab === 'HITL' && (
          <>
            <HitlPanel
              hitlLoading={hitlLoading}
              hitlError={hitlError}
              hitlResult={hitlResult}
              onRun={handleRunHitl}
              onOpen={() => setModalOpen(true)}
            />
            {hitlResult?.runId && (
              <TraceButton onClick={() => handleViewTrace(hitlResult.runId)} loading={traceLoading} />
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
              {activeTab === 'AI Review'  && <AiReviewModalContent  aiResult={aiResult} />}
              {activeTab === 'RAG Review' && <RagReviewModalContent ragResult={ragResult} />}
              {activeTab === 'Evidence'   && <EvidenceModalContent  evidenceResult={evidenceResult} />}
              {activeTab === 'Agent'      && <AgentModalContent     agentResult={agentResult} />}
              {activeTab === 'HITL'       && (
                <HitlModalContent
                  review={hitlResult}
                  error={hitlError}
                  deciding={hitlDeciding}
                  onDecide={handleHitlDecide}
                />
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Modal AI Trace (S6) ── */}
      {traceModalOpen && (
        <div
          onClick={() => setTraceModalOpen(false)}
          style={{
            position: 'fixed', inset: 0, zIndex: 1100,
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
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid rgba(77,124,199,0.1)',
              display: 'flex', justifyContent: 'flex-end', alignItems: 'center',
              flexShrink: 0,
            }}>
              <button
                onClick={() => setTraceModalOpen(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center' }}
              >
                <TbX size={20} />
              </button>
            </div>
            <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              <TraceModalContent trace={traceResult} error={traceError} />
            </div>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </>
  );
}