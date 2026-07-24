import { TbFingerprint, TbClock, TbTool, TbClipboardCheck, TbLock, TbAlertTriangle } from 'react-icons/tb';
import { Section, Badge, ErrorBox } from '../hitl/HitlUi';
import { RUN_TYPE_LABELS, TRACE_STATUS_CONFIG, CHECKPOINT_TRACE_STATUS_CONFIG } from './Traceconstants';

/**
 * AI Trace debug panel : run metadata, prompt/model metadata,
 * tool-call trace, checkpoint/human decision trace, and safety flags.
 * Never displays hidden chain-of-thought .
 */
export default function TraceModalContent({ trace, error }) {
  if (error) return <ErrorBox>{error}</ErrorBox>;
  if (!trace) return null;

  const statusConfig = TRACE_STATUS_CONFIG[trace.status] ?? { label: trace.status, color: '#94a3b8' };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#94a3b8', letterSpacing: '0.5px', marginBottom: '12px' }}>
        <TbFingerprint size={14} /> AI TRACE
      </div>

      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        <Badge color={statusConfig.color}>{statusConfig.label}</Badge>
        {trace.runType && <Badge color="#60a5fa">{RUN_TYPE_LABELS[trace.runType] ?? trace.runType}</Badge>}
      </div>

      {trace.errorMessage && <ErrorBox>{trace.errorMessage}</ErrorBox>}

      <Section title="AI Metadata">
        <TraceRow label="Trace ID" value={trace.traceId} mono />
        <TraceRow label="Run ID" value={trace.runId} mono />
        <TraceRow label="Ticket ID" value={trace.ticketId} mono />
        <TraceRow label="Prompt Version" value={trace.promptVersion} mono />
        <TraceRow label="Model Name" value={trace.modelName} mono />
      </Section>

      <Section title="Timing">
        <TraceRow label="Started" value={trace.startedAt ? new Date(trace.startedAt).toLocaleString() : '—'} />
        <TraceRow label="Completed" value={trace.completedAt ? new Date(trace.completedAt).toLocaleString() : '—'} />
        <TraceRow label="Duration" value={trace.durationMs != null ? `${trace.durationMs} ms` : '—'} />
      </Section>

      {trace.toolCalls?.length > 0 && (
        <Section title="Tool Calls">
          {trace.toolCalls.map((tc, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '8px 12px', borderRadius: '10px', marginBottom: '6px',
              background: tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.06)' : 'rgba(248,113,113,0.06)',
              border: `1px solid ${tc.status === 'SUCCESS' ? 'rgba(74,222,128,0.25)' : 'rgba(248,113,113,0.25)'}`,
            }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', fontFamily: 'monospace' }}>
                <TbTool size={12} style={{ marginRight: '6px', verticalAlign: 'middle' }} />{tc.toolName}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                {tc.durationMs != null && (
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '3px' }}>
                    <TbClock size={11} /> {tc.durationMs} ms
                  </span>
                )}
                <span style={{ fontSize: '11px', fontWeight: 700, color: tc.status === 'SUCCESS' ? '#4ade80' : '#f87171' }}>
                  {tc.status}
                </span>
              </span>
            </div>
          ))}
        </Section>
      )}

      {trace.checkpoints?.length > 0 && (
        <Section title="Human Review Checkpoints">
          {trace.checkpoints.map((cp, i) => {
            const cfg = CHECKPOINT_TRACE_STATUS_CONFIG[cp.status] ?? { label: cp.status, color: '#94a3b8' };
            return (
              <div key={i} style={{
                padding: '10px 12px', borderRadius: '10px', marginBottom: '6px',
                background: 'rgba(250,204,21,0.05)', border: '1px solid rgba(250,204,21,0.15)',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '5px' }}>
                    <TbClipboardCheck size={13} /> Checkpoint #{cp.checkpointNumber}
                  </span>
                  <Badge color={cfg.color}>{cfg.label}</Badge>
                </div>
                {cp.humanDecision && (
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Decision: <strong style={{ color: 'var(--text-main)' }}>{cp.humanDecision}</strong>
                  </div>
                )}
                {cp.humanComment && (
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px', fontStyle: 'italic' }}>
                    "{cp.humanComment}"
                  </div>
                )}
                {cp.completedAt && (
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
                    Completed {new Date(cp.completedAt).toLocaleString()}
                  </div>
                )}
              </div>
            );
          })}
        </Section>
      )}

      <div style={{ marginTop: '4px', padding: '12px', background: 'rgba(148,163,184,0.06)', border: '1px solid rgba(148,163,184,0.2)', borderRadius: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
          <TbLock size={14} color="#94a3b8" />
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>
            officialActionExecuted = {String(trace.officialActionExecuted)}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <TbLock size={14} color="#94a3b8" />
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>
            ticketStatusChanged = {String(trace.ticketStatusChanged)}
          </span>
        </div>
      </div>

      <p style={{ fontSize: '11px', color: 'var(--text-muted)', lineHeight: 1.6, marginTop: '12px', display: 'flex', alignItems: 'center', gap: '5px' }}>
        <TbAlertTriangle size={12} /> This trace shows operational metadata only — no hidden model reasoning is displayed.
      </p>
    </>
  );
}

function TraceRow({ label, value, mono }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
      <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{label}</span>
      <span style={{ fontSize: '12px', color: 'var(--text-main)', fontFamily: mono ? 'monospace' : 'inherit' }}>
        {value ?? '—'}
      </span>
    </div>
  );
}