import { useState, useEffect, useRef } from 'react';
import { TbPlayerPlay, TbRefresh, TbSearch, TbCheck, TbX } from 'react-icons/tb';

import { getAllTickets, startTriageBatch, getTriageBatch } from '../../services/api';
import { MAX_BATCH_SIZE, RUN_STATUS_CONFIG, CRITICALITY_CONFIG } from '../../components/triage/Triageconstants';
import TriageTicketCard from '../../components/triage/TriageTicketCard';

// The pipeline (Classify -> Order -> Dispatch -> Investigation -> Review ->
// Rules -> HITL -> Observation) now runs SYNCHRONOUSLY on the backend:
// startTriageBatch() only responds once the whole batch has been processed.
// The polling below is only kept as a safety net (e.g. if the backend ever
// becomes asynchronous again, or for the "Load a run by ID" lookup).
const POLL_INTERVAL_MS = 3000;

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

/** Styled checkbox — replaces the native browser checkbox (no more white/red squares). */
function Checkbox({ checked, disabled }) {
  return (
    <span
      aria-hidden="true"
      style={{
        width: '19px', height: '19px', borderRadius: '6px', flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        border: checked ? '1px solid transparent' : '1px solid rgba(77,124,199,0.35)',
        background: checked ? 'var(--btn-gradient)' : 'var(--bg-input)',
        opacity: disabled ? 0.4 : 1,
        transition: 'background 0.15s ease, border-color 0.15s ease',
        boxSizing: 'border-box',
      }}
    >
      {checked && <TbCheck size={13} color="#fff" strokeWidth={3} />}
    </span>
  );
}

/** Simple centered modal overlay used for the "load run by ID" popup. */
function Modal({ title, onClose, children }) {
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
          borderRadius: '20px',
          width: '100%', maxWidth: '420px',
          overflow: 'hidden',
        }}
      >
        <div style={{
          padding: '18px 22px',
          borderBottom: '1px solid rgba(77,124,199,0.1)',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)' }}>{title}</span>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center' }}
          >
            <TbX size={20} />
          </button>
        </div>
        <div style={{ padding: '22px' }}>
          {children}
        </div>
      </div>
    </div>
  );
}

export default function TriagePage() {
  const [tickets,        setTickets]        = useState([]);
  const [loadingTickets, setLoadingTickets]  = useState(true);
  const [ticketsError,   setTicketsError]    = useState(null);

  const [selectedIds,        setSelectedIds]        = useState(new Set());
  const [includeAllOpen,     setIncludeAllOpen]      = useState(false);

  const [run,          setRun]          = useState(null);
  const [starting,     setStarting]     = useState(false);
  const [runError,     setRunError]     = useState(null);

  const [lookupRunId,  setLookupRunId]  = useState('');
  const [lookupError,  setLookupError]  = useState(null);
  const [lookupOpen,   setLookupOpen]   = useState(false);
  const [lookupLoading, setLookupLoading] = useState(false);

  const pollRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoadingTickets(true);
      setTicketsError(null);
      try {
        const data = await getAllTickets();
        const list = Array.isArray(data) ? data : (data?.data ?? data?.content ?? []);
        if (!cancelled) setTickets(list.filter(t => t.status === 'OPEN'));
      } catch (err) {
        console.error('[TriagePage] getAllTickets error:', err);
        if (!cancelled) setTicketsError('Unable to load tickets.');
      } finally {
        if (!cancelled) setLoadingTickets(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, []);

  // Filet de securite : ne poll que si un run chargé (via lookup, ou en
  // théorie renvoyé par le backend) n'est pas encore dans un état terminal.
  // Avec le pipeline synchrone, ce cas ne devrait plus se produire au retour
  // de startTriageBatch, mais on le garde pour le lookup par ID.
  useEffect(() => {
    if (pollRef.current) clearInterval(pollRef.current);
    if (!run || run.status === 'COMPLETED' || run.status === 'FAILED') return undefined;

    pollRef.current = setInterval(async () => {
      try {
        const updated = await getTriageBatch(run.runId);
        setRun(updated);
      } catch (err) {
        console.error('[TriagePage] poll getTriageBatch error:', err);
      }
    }, POLL_INTERVAL_MS);

    return () => clearInterval(pollRef.current);
  }, [run?.runId, run?.status]);

  const toggleTicket = (id) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else if (next.size < MAX_BATCH_SIZE) {
        next.add(id);
      }
      return next;
    });
  };

  const handleStartBatch = async () => {
    setStarting(true);
    setRunError(null);
    try {
      // Requête potentiellement longue : le backend exécute les 8 étapes
      // du pipeline pour chaque ticket du batch avant de répondre.
      const response = await startTriageBatch({
        ticketIds: includeAllOpen ? [] : Array.from(selectedIds),
        includeAllOpenTickets: includeAllOpen,
      });
      setRun(response);
      setSelectedIds(new Set());
    } catch (err) {
      console.error('[TriagePage] startTriageBatch error:', err);
      setRunError(err.response?.data?.message ?? "Unable to start the triage batch.");
    } finally {
      setStarting(false);
    }
  };

  const handleLookupRun = async () => {
    const id = Number(lookupRunId);
    if (!id) return;
    setLookupError(null);
    setLookupLoading(true);
    try {
      const response = await getTriageBatch(id);
      setRun(response);
      setLookupOpen(false);
      setLookupRunId('');
    } catch (err) {
      console.error('[TriagePage] getTriageBatch (lookup) error:', err);
      setLookupError(`Run #${id} not found.`);
    } finally {
      setLookupLoading(false);
    }
  };

  const canStart = includeAllOpen ? tickets.length > 0 : selectedIds.size > 0;

  const CRITICALITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  const classifiedTickets = Object.values(run?.classifications ?? {})
    .sort((a, b) => {
      const diff = CRITICALITY_ORDER.indexOf(a.criticality) - CRITICALITY_ORDER.indexOf(b.criticality);
      return diff !== 0 ? diff : a.ticketId - b.ticketId;
    });

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', width: '100%', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Header */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 700, color: 'var(--text-main)', margin: 0, letterSpacing: '-0.3px' }}>
          Triage
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
          Select up to {MAX_BATCH_SIZE} open tickets and launch the multi-agent triage pipeline
          (classification, dispatch, investigation, review, rules, human review).
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', alignItems: 'start' }}>

        {/* Colonne gauche : sélection des tickets */}
        <div style={{
          background: 'var(--bg-card)',
          border: '1px solid rgba(77,124,199,0.15)',
          borderRadius: '20px',
          overflow: 'hidden',
        }}>
          <div style={{ padding: '18px 20px', borderBottom: '1px solid rgba(77,124,199,0.1)' }}>
            <div
              role="checkbox"
              aria-checked={includeAllOpen}
              tabIndex={0}
              onClick={() => { setIncludeAllOpen(v => { const next = !v; setSelectedIds(new Set()); return next; }); }}
              onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setIncludeAllOpen(v => { const next = !v; setSelectedIds(new Set()); return next; }); } }}
              style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', color: 'var(--text-main)', fontWeight: 600, cursor: 'pointer' }}
            >
              <Checkbox checked={includeAllOpen} />
              Include all open tickets ({tickets.length})
            </div>
            {!includeAllOpen && (
              <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
                {selectedIds.size} / {MAX_BATCH_SIZE} selected
              </p>
            )}
          </div>

          <div style={{ maxHeight: '420px', overflowY: 'auto' }}>
            {loadingTickets ? (
              <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>Loading…</div>
            ) : ticketsError ? (
              <div style={{ padding: '24px', color: '#f87171', fontSize: '14px' }}>{ticketsError}</div>
            ) : tickets.length === 0 ? (
              <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>No open tickets.</div>
            ) : (
              tickets.map(t => {
                const disabled = includeAllOpen || (!selectedIds.has(t.id) && selectedIds.size >= MAX_BATCH_SIZE);
                const checked = includeAllOpen || selectedIds.has(t.id);
                return (
                  <div
                    key={t.id}
                    role="checkbox"
                    aria-checked={checked}
                    aria-disabled={disabled}
                    tabIndex={disabled ? -1 : 0}
                    onClick={() => { if (!disabled) toggleTicket(t.id); }}
                    onKeyDown={e => { if (!disabled && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); toggleTicket(t.id); } }}
                    style={{
                      display: 'flex', alignItems: 'center', gap: '12px',
                      padding: '12px 20px', cursor: includeAllOpen ? 'default' : (disabled ? 'not-allowed' : 'pointer'),
                      borderBottom: '1px solid rgba(77,124,199,0.08)',
                      opacity: includeAllOpen ? 0.5 : 1,
                    }}
                  >
                    <Checkbox checked={checked} disabled={disabled} />
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)', width: '48px', flexShrink: 0 }}>#{t.id}</span>
                    <span style={{ fontSize: '14px', color: 'var(--text-main)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {t.title}
                    </span>
                  </div>
                );
              })
            )}
          </div>

          <div style={{ padding: '16px 20px', borderTop: '1px solid rgba(77,124,199,0.1)' }}>
            {runError && (
              <p style={{ fontSize: '13px', color: '#f87171', margin: '0 0 10px' }}>{runError}</p>
            )}
            <button
              onClick={handleStartBatch}
              disabled={!canStart || starting}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
                width: '100%', padding: '11px 18px',
                background: canStart ? 'var(--btn-gradient)' : 'var(--bg-input)',
                border: 'none', borderRadius: '12px',
                color: canStart ? '#fff' : 'var(--text-muted)', fontSize: '14px', fontWeight: 600,
                cursor: canStart && !starting ? 'pointer' : 'not-allowed',
              }}
            >
              <TbPlayerPlay size={16} />
              {starting ? 'Pipeline running (classification → … → human review)…' : 'Start triage'}
            </button>
          </div>
        </div>

        {/* Colonne droite : résultat du run */}
        <div style={{
          background: 'var(--bg-card)',
          border: '1px solid rgba(77,124,199,0.15)',
          borderRadius: '20px',
          padding: '20px',
        }}>
          {/* Load an existing run by ID */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
            <button
              onClick={() => { setLookupError(null); setLookupOpen(true); }}
              style={{
                display: 'flex', alignItems: 'center', gap: '6px',
                padding: '9px 14px', borderRadius: '10px',
                border: '1px solid rgba(77,124,199,0.2)', background: 'var(--bg-input)',
                color: 'var(--text-main)', fontSize: '13px', fontWeight: 600, cursor: 'pointer',
              }}
            >
              <TbSearch size={14} /> Load a run by ID
            </button>
          </div>

          {!run ? (
            <div style={{ padding: '48px 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
              No run selected.
            </div>
          ) : (
            <>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-main)', margin: 0 }}>
                    Run #{run.runId}
                  </h3>
                  <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '4px 0 0' }}>
                    {run.promptVersion}
                  </p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Badge
                    label={RUN_STATUS_CONFIG[run.status]?.label ?? run.status}
                    color={RUN_STATUS_CONFIG[run.status]?.dot ?? '#94a3b8'}
                  />
                  <button
                    onClick={async () => setRun(await getTriageBatch(run.runId))}
                    title="Refresh"
                    style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      width: '30px', height: '30px', borderRadius: '8px',
                      border: '1px solid rgba(77,124,199,0.2)', background: 'var(--bg-input)',
                      color: 'var(--text-muted)', cursor: 'pointer',
                    }}
                  >
                    <TbRefresh size={15} />
                  </button>
                </div>
              </div>

              {run.errorMessage && (
                <p style={{ fontSize: '13px', color: '#f87171', margin: '0 0 16px' }}>{run.errorMessage}</p>
              )}

              {/* Classification (Agent 1) de tous les tickets du batch sélectionné —
                  même forme que l'ancienne "Remaining queue" (pills en flex-wrap),
                  mais chaque pill montre désormais la criticité au lieu d'un id nu. */}
              <div style={{ marginBottom: '18px' }}>
                <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', margin: '0 0 8px', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                  Classification ({classifiedTickets.length})
                </p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                  {classifiedTickets.length === 0 ? (
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Empty</span>
                  ) : (
                    classifiedTickets.map(c => {
                      const color = CRITICALITY_CONFIG[c.criticality]?.color ?? '#94a3b8';
                      const label = CRITICALITY_CONFIG[c.criticality]?.label ?? c.criticality;
                      return (
                        <span key={c.ticketId} title={c.rationale ?? undefined} style={{
                          display: 'inline-flex', alignItems: 'center', gap: '6px',
                          padding: '3px 10px', borderRadius: '999px',
                          background: `${color}1a`, border: `1px solid ${color}40`,
                          fontSize: '12px', color: 'var(--text-main)',
                        }}>
                          #{c.ticketId}
                          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: color }} />
                          {label}
                        </span>
                      );
                    })
                  )}
                </div>
              </div>

              {/* Tickets traités — pipeline multi-agent par ticket */}
              <div>
                <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', margin: '0 0 8px', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                  Processed ({run.treated?.length ?? 0})
                </p>
                {(run.treated ?? []).length === 0 ? (
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>No tickets processed yet.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {run.treated.map((item, i) => (
                      <TriageTicketCard key={`${item.ticketId}-${i}`} item={item} />
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {lookupOpen && (
        <Modal title="Load a run by ID" onClose={() => setLookupOpen(false)}>
          <div style={{ display: 'flex', gap: '8px' }}>
            <input
              type="number"
              autoFocus
              placeholder="Run ID…"
              value={lookupRunId}
              onChange={e => setLookupRunId(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleLookupRun(); }}
              style={{
                flex: 1, padding: '10px 12px', borderRadius: '10px',
                border: '1px solid rgba(77,124,199,0.2)', background: 'var(--bg-input)',
                color: 'var(--text-main)', fontSize: '13px',
              }}
            />
            <button
              onClick={handleLookupRun}
              disabled={!lookupRunId || lookupLoading}
              style={{
                display: 'flex', alignItems: 'center', gap: '6px',
                padding: '10px 16px', borderRadius: '10px',
                border: 'none', background: lookupRunId ? 'var(--btn-gradient)' : 'var(--bg-input)',
                color: lookupRunId ? '#fff' : 'var(--text-muted)', fontSize: '13px', fontWeight: 600,
                cursor: lookupRunId && !lookupLoading ? 'pointer' : 'not-allowed',
              }}
            >
              <TbSearch size={14} /> {lookupLoading ? 'Loading…' : 'Load'}
            </button>
          </div>
          {lookupError && <p style={{ fontSize: '13px', color: '#f87171', margin: '12px 0 0' }}>{lookupError}</p>}
        </Modal>
      )}

      <style>{`
        @media (max-width: 900px) {
          div[style*="grid-template-columns: 1fr 1fr"] { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}