import { useState, useEffect } from 'react';
import { getAllTickets } from '../../services/api';
import { TbPlus } from 'react-icons/tb';

import TicketStats        from '../../components/tickets/TicketStats';
import TicketFilters      from '../../components/tickets/TicketFilters';
import TicketRow          from '../../components/tickets/TicketRow';
import TicketDetailPanel  from '../../components/tickets/TicketDetailPanel';
import CreateTicketModal  from '../../components/tickets/Createticketmodal ';

import { FILTER_MAP }     from '../../components/tickets/Ticketconstants';

export default function TicketPage() {
  const [tickets,     setTickets]     = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState(null);
  const [filter,      setFilter]      = useState('All');
  const [selectedId,  setSelectedId]  = useState(null);
  const [showModal,   setShowModal]   = useState(false);
  const [retry,       setRetry]       = useState(0);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getAllTickets();
        const list = Array.isArray(data) ? data : (data?.data ?? data?.content ?? []);
        if (!cancelled) setTickets(list);
      } catch (err) {
        console.error('[TicketPage] fetchTickets error:', err);
        if (!cancelled) setError('Failed to load tickets.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [retry]);

  const handleRetry = () => setRetry(r => r + 1);

  const handleTicketCreated = (created) => {
    setTickets(prev => [created, ...prev]);
  };

  const handleStatusUpdated = (updated) => {
    setTickets(prev => prev.map(t => t.id === updated.id ? updated : t));
  };

  const filtered = tickets.filter(t => {
    const statusFilter = FILTER_MAP[filter];
    return statusFilter ? t.status === statusFilter : true;
  });

  const selectedTicket = selectedId ? tickets.find(t => t.id === selectedId) ?? null : null;

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', width: '100%', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: 700, color: 'var(--text-main)', margin: 0, letterSpacing: '-0.3px' }}>
            Tickets
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
            {loading
              ? 'Loading your tickets…'
              : <>{tickets.length} ticket{tickets.length !== 1 ? 's' : ''} in total</>
            }
          </p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            padding: '10px 18px', background: 'var(--btn-gradient)',
            border: 'none', borderRadius: '12px',
            color: '#fff', fontSize: '14px', fontWeight: 600,
            cursor: 'pointer', flexShrink: 0,
            boxShadow: '0 4px 14px rgba(77,124,199,0.35)',
          }}
        >
          <TbPlus size={18} /> New Ticket
        </button>
      </div>

      {/* Stats */}
      <TicketStats tickets={tickets} />

      {/* Error */}
      {error && (
        <div style={{
          padding: '16px 20px', borderRadius: '12px',
          background: 'rgba(248,113,113,0.08)',
          border: '1px solid rgba(248,113,113,0.3)',
          color: '#f87171', fontSize: '14px', marginBottom: '20px',
        }}>
          {error}{' '}
          <button onClick={handleRetry} style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', textDecoration: 'underline', fontSize: '14px' }}>
            Retry
          </button>
        </div>
      )}

      {/* Ticket list + detail */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: selectedTicket ? '1fr 380px' : '1fr',
        gap: '20px',
        alignItems: 'start',
      }}>
        <div style={{
          background: 'var(--bg-card)',
          border: '1px solid rgba(77,124,199,0.15)',
          borderRadius: '20px',
          overflow: 'hidden',
        }}>
          <TicketFilters
            filter={filter}
            count={filtered.length}
            onChange={f => { setFilter(f); setSelectedId(null); }}
          />
          {loading ? (
            <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>Loading…</div>
          ) : filtered.length === 0 ? (
            <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>No tickets in this category.</div>
          ) : (
            filtered.map((ticket, i) => (
              <TicketRow
                key={ticket.id}
                ticket={ticket}
                isActive={selectedId === ticket.id}
                isLast={i === filtered.length - 1}
                onClick={() => setSelectedId(prev => prev === ticket.id ? null : ticket.id)}
              />
            ))
          )}
        </div>

        {selectedTicket && (
          <TicketDetailPanel
            ticket={selectedTicket}
            onClose={() => setSelectedId(null)}
            onStatusUpdated={handleStatusUpdated}
          />
        )}
      </div>

      {showModal && (
        <CreateTicketModal
          onClose={() => setShowModal(false)}
          onCreated={handleTicketCreated}
        />
      )}

      <style>{`
        @media (max-width: 900px) {
          div[style*="grid-template-columns"] { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}