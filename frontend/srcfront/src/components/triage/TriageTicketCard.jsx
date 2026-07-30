import { useState } from 'react';
import { TbChevronDown, TbChevronUp, TbX, TbAlertTriangle } from 'react-icons/tb';

import { getAgentRunTrace, getHitlReview, submitHumanDecision } from '../../services/api';
import { CRITICALITY_CONFIG, OUTCOME_CONFIG, isPendingHumanReview } from './Triageconstants';
import { RUN_STATUS_CONFIG as HITL_RUN_STATUS_CONFIG } from '../tickets/hitl/Hitlconstants';
import TriagePipelineSteps from './TriagePipelineSteps';

import { ErrorBox, Spinner } from '../tickets/hitl/HitlUi';
import { HitlModalContent } from '../tickets/hitl/Hitlmodalcontent';
import TraceButton from '../tickets/trace/TraceButton';
import TraceModalContent from '../tickets/trace/TraceModalContent';

function Badge({ label, color }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '6px',
      padding: '3px 10px', borderRadius: '999px',
      background: `${color}1a`, border: `1px solid ${color}40`,
      color, fontSize: '12px', fontWeight: 600,
    }}>
      <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: color }} />
      {label}
    </span>
  );
}

/** Overlay + fenêtre modale réutilisée depuis TicketDetailPanel (même look). */
function SimpleModal({ title, onClose, children }) {
  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, zIndex: 1200,
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
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          flexShrink: 0,
        }}>
          <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-main)' }}>{title}</span>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center' }}
          >
            <TbX size={20} />
          </button>
        </div>
        <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
          {children}
        </div>
      </div>
    </div>
  );
}

/**
 * Carte pour un ticket traité par le pipeline de triage (item de
 * TriageRunResponse.treated). Affiche l'avancement multi-agent, et donne
 * accès à la trace de l'agent et — si le pipeline s'est arrêté sur un
 * checkpoint HITL pour ce ticket — au formulaire de décision humaine.
 */
export default function TriageTicketCard({ item }) {
  const [expanded, setExpanded] = useState(false);

  const [traceResult,  setTraceResult]  = useState(null);
  const [traceError,   setTraceError]   = useState(null);
  const [traceLoading, setTraceLoading] = useState(false);
  const [traceOpen,    setTraceOpen]    = useState(false);

  const [hitlResult,   setHitlResult]   = useState(null);
  const [hitlError,    setHitlError]    = useState(null);
  const [hitlLoading,  setHitlLoading]  = useState(false);
  const [hitlDeciding, setHitlDeciding] = useState(null);
  const [hitlOpen,     setHitlOpen]     = useState(false);

  const pending = isPendingHumanReview(item);

  const handleViewTrace = async () => {
    if (!item.agentRunId) return;
    setTraceLoading(true); setTraceError(null); setTraceResult(null);
    try {
      const data = await getAgentRunTrace(item.agentRunId);
      setTraceResult(data);
      setTraceOpen(true);
    } catch (err) {
      setTraceError(err.response?.data?.message ?? 'Impossible de charger la trace.');
      setTraceOpen(true);
    } finally {
      setTraceLoading(false);
    }
  };

  const handleOpenHitl = async () => {
    if (!item.agentRunId) return;
    setHitlLoading(true); setHitlError(null);
    try {
      const data = await getHitlReview(item.ticketId, item.agentRunId);
      setHitlResult(data);
      setHitlOpen(true);
    } catch (err) {
      setHitlError(err.response?.data?.message ?? 'Impossible de charger la revue HITL.');
      setHitlOpen(true);
    } finally {
      setHitlLoading(false);
    }
  };

  const handleHitlDecide = async (decision, comment) => {
    if (!hitlResult?.runId) return;
    setHitlDeciding(decision); setHitlError(null);
    try {
      const decisionResponse = await submitHumanDecision(hitlResult.runId, decision, comment);
      if (decision === 'REQUEST_REVISION' && decisionResponse.revisedReview) {
        setHitlResult(decisionResponse.revisedReview);
      } else {
        const reloaded = await getHitlReview(item.ticketId, hitlResult.runId);
        setHitlResult(reloaded);
      }
    } catch (err) {
      setHitlError(err.response?.data?.message ?? 'La décision humaine a échoué.');
    } finally {
      setHitlDeciding(null);
    }
  };

  return (
    <div style={{
      borderRadius: '12px',
      background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.12)',
      overflow: 'hidden',
    }}>
      {/* Header — toujours visible */}
      <button
        onClick={() => setExpanded(e => !e)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '10px 14px', background: 'transparent', border: 'none', cursor: 'pointer',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-main)' }}>#{item.ticketId}</span>
          {pending && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: '#facc15' }}>
              <TbAlertTriangle size={12} /> Revue requise
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {item.criticality && (
            <Badge
              label={CRITICALITY_CONFIG[item.criticality]?.label ?? item.criticality}
              color={CRITICALITY_CONFIG[item.criticality]?.color ?? '#94a3b8'}
            />
          )}
          <Badge
            label={OUTCOME_CONFIG[item.outcome]?.label ?? item.outcome}
            color={OUTCOME_CONFIG[item.outcome]?.color ?? '#94a3b8'}
          />
          {expanded ? <TbChevronUp size={16} color="var(--text-muted)" /> : <TbChevronDown size={16} color="var(--text-muted)" />}
        </div>
      </button>

      {/* Contenu déplié */}
      {expanded && (
        <div style={{ padding: '4px 14px 14px' }}>
          <div style={{ marginBottom: '12px' }}>
            <TriagePipelineSteps
              criticality={item.criticality}
              outcome={item.outcome}
            />
          </div>

          {item.errorMessage && <ErrorBox>{item.errorMessage}</ErrorBox>}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '10px' }}>
            {item.agentRunId && (
              <TraceButton onClick={handleViewTrace} loading={traceLoading} />
            )}

            {pending && item.agentRunId && (
              <button onClick={handleOpenHitl} disabled={hitlLoading} style={{
                width: '100%', padding: '11px',
                background: hitlLoading ? 'rgba(250,204,21,0.05)' : 'rgba(250,204,21,0.10)',
                border: '1px solid rgba(250,204,21,0.3)', borderRadius: '10px',
                color: '#facc15', fontSize: '13px', fontWeight: 600,
                cursor: hitlLoading ? 'not-allowed' : 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
              }}>
                {hitlLoading ? <Spinner color="#facc15" /> : null}
                {hitlLoading ? 'Chargement…' : `Ouvrir la revue humaine — ${HITL_RUN_STATUS_CONFIG.WAITING_FOR_HUMAN.label}`}
              </button>
            )}
          </div>
        </div>
      )}

      {traceOpen && (
        <SimpleModal title="AI TRACE" onClose={() => setTraceOpen(false)}>
          <TraceModalContent trace={traceResult} error={traceError} />
        </SimpleModal>
      )}

      {hitlOpen && (
        <SimpleModal title={`HITL — Ticket #${item.ticketId}`} onClose={() => setHitlOpen(false)}>
          <HitlModalContent
            review={hitlResult}
            error={hitlError}
            deciding={hitlDeciding}
            onDecide={handleHitlDecide}
          />
        </SimpleModal>
      )}
    </div>
  );
}