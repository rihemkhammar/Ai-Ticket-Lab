import { useState, useEffect, useRef } from 'react';
import { TbPlayerPlay, TbRefresh, TbSearch } from 'react-icons/tb';

import { getAllTickets, startTriageBatch, getTriageBatch } from '../../services/api';
import { MAX_BATCH_SIZE, RUN_STATUS_CONFIG } from '../../components/triage/Triageconstants';
import TriageTicketCard from '../../components/triage/TriageTicketCard';

// Le pipeline (Classify -> Order -> Dispatch -> Investigation -> Review ->
// Rules -> HITL -> Observation) tourne desormais de facon SYNCHRONE cote
// backend : startTriageBatch() ne repond qu'une fois tout le batch traite.
// Le polling ci-dessous n'est garde que comme filet de securite (ex. si un
// jour le backend redevient asynchrone, ou pour le "Charger un run par ID").
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
        if (!cancelled) setTicketsError('Impossible de charger les tickets.');
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
      setRunError(err.response?.data?.message ?? "Impossible de démarrer le batch de triage.");
    } finally {
      setStarting(false);
    }
  };

  const handleLookupRun = async () => {
    const id = Number(lookupRunId);
    if (!id) return;
    setLookupError(null);
    try {
      const response = await getTriageBatch(id);
      setRun(response);
    } catch (err) {
      console.error('[TriagePage] getTriageBatch (lookup) error:', err);
      setLookupError(`Run #${id} introuvable.`);
    }
  };

  const canStart = includeAllOpen ? tickets.length > 0 : selectedIds.size > 0;

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', width: '100%', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Header */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 700, color: 'var(--text-main)', margin: 0, letterSpacing: '-0.3px' }}>
          Triage
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
          Sélectionnez jusqu'à {MAX_BATCH_SIZE} tickets ouverts et lancez le pipeline de triage multi-agent
          (classification, dispatch, investigation, review, règles, revue humaine).
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
            <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', color: 'var(--text-main)', fontWeight: 600, cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={includeAllOpen}
                onChange={e => { setIncludeAllOpen(e.target.checked); setSelectedIds(new Set()); }}
              />
              Inclure tous les tickets ouverts ({tickets.length})
            </label>
            {!includeAllOpen && (
              <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
                {selectedIds.size} / {MAX_BATCH_SIZE} sélectionné{selectedIds.size !== 1 ? 's' : ''}
              </p>
            )}
          </div>

          <div style={{ maxHeight: '420px', overflowY: 'auto' }}>
            {loadingTickets ? (
              <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>Chargement…</div>
            ) : ticketsError ? (
              <div style={{ padding: '24px', color: '#f87171', fontSize: '14px' }}>{ticketsError}</div>
            ) : tickets.length === 0 ? (
              <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>Aucun ticket ouvert.</div>
            ) : (
              tickets.map(t => (
                <label
                  key={t.id}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '12px',
                    padding: '12px 20px', cursor: includeAllOpen ? 'default' : 'pointer',
                    borderBottom: '1px solid rgba(77,124,199,0.08)',
                    opacity: includeAllOpen ? 0.5 : 1,
                  }}
                >
                  <input
                    type="checkbox"
                    disabled={includeAllOpen || (!selectedIds.has(t.id) && selectedIds.size >= MAX_BATCH_SIZE)}
                    checked={includeAllOpen || selectedIds.has(t.id)}
                    onChange={() => toggleTicket(t.id)}
                  />
                  <span style={{ fontSize: '13px', color: 'var(--text-muted)', width: '48px', flexShrink: 0 }}>#{t.id}</span>
                  <span style={{ fontSize: '14px', color: 'var(--text-main)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {t.title}
                  </span>
                </label>
              ))
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
              {starting ? 'Pipeline en cours (classification → …→ observation)…' : 'Lancer le triage'}
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
          {/* Charger un run existant */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
            <input
              type="number"
              placeholder="Charger un run par ID…"
              value={lookupRunId}
              onChange={e => setLookupRunId(e.target.value)}
              style={{
                flex: 1, padding: '9px 12px', borderRadius: '10px',
                border: '1px solid rgba(77,124,199,0.2)', background: 'var(--bg-input)',
                color: 'var(--text-main)', fontSize: '13px',
              }}
            />
            <button
              onClick={handleLookupRun}
              style={{
                display: 'flex', alignItems: 'center', gap: '6px',
                padding: '9px 14px', borderRadius: '10px',
                border: '1px solid rgba(77,124,199,0.2)', background: 'var(--bg-input)',
                color: 'var(--text-main)', fontSize: '13px', fontWeight: 600, cursor: 'pointer',
              }}
            >
              <TbSearch size={14} /> Charger
            </button>
          </div>
          {lookupError && <p style={{ fontSize: '13px', color: '#f87171', margin: '-12px 0 16px' }}>{lookupError}</p>}

          {!run ? (
            <div style={{ padding: '48px 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
              Aucun run sélectionné.
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
                    title="Rafraîchir"
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

              {/* File restante */}
              <div style={{ marginBottom: '18px' }}>
                <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', margin: '0 0 8px', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                  File restante ({run.ticketQueue?.length ?? 0})
                </p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                  {(run.ticketQueue ?? []).length === 0 ? (
                    <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Vide</span>
                  ) : (
                    run.ticketQueue.map(id => (
                      <span key={id} style={{
                        padding: '3px 10px', borderRadius: '999px',
                        background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)',
                        fontSize: '12px', color: 'var(--text-main)',
                      }}>
                        #{id}
                      </span>
                    ))
                  )}
                </div>
              </div>

              {/* Tickets traités — pipeline multi-agent par ticket */}
              <div>
                <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', margin: '0 0 8px', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                  Traités ({run.treated?.length ?? 0})
                </p>
                {(run.treated ?? []).length === 0 ? (
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>Aucun ticket traité pour l'instant.</p>
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

      <style>{`
        @media (max-width: 900px) {
          div[style*="grid-template-columns: 1fr 1fr"] { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
