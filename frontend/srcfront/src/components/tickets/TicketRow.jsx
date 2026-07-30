import { TbChevronRight } from 'react-icons/tb';
import { STATUS_CONFIG } from './Ticketconstants';

/**
 * @param {{
 *   ticket: object,
 *   isActive: boolean,
 *   isLast: boolean,
 *   onClick: () => void
 * }} props
 */
export default function TicketRow({ ticket, isActive, isLast, onClick }) {
 
  const s = STATUS_CONFIG[ticket.status]   ?? STATUS_CONFIG.OPEN;

  return (
    <div
      onClick={onClick}
      style={{
        padding: '16px 24px',
        borderBottom: isLast ? 'none' : '1px solid rgba(77,124,199,0.08)',
        display: 'flex', alignItems: 'center', gap: '16px',
        cursor: 'pointer',
        background: isActive ? 'rgba(77,124,199,0.07)' : 'transparent',
        transition: 'background 0.15s',
      }}
    >
      {/* Status dot */}
      <span style={{
        width: '10px', height: '10px', borderRadius: '50%',
        background: s.dot, flexShrink: 0,
      }} />

      {/* Main info */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
          <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', fontFamily: 'monospace' }}>
            #{ticket.id?.toString().slice(-6) ?? '------'}
          </span>
          
        </div>

        <div style={{
          fontSize: '14px', fontWeight: 500, color: 'var(--text-main)',
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>{ticket.title}</div>

        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '3px' }}>
          {ticket.category ?? 'General'} · {ticket.createdAt
            ? new Date(ticket.createdAt).toLocaleDateString()
            : 'Unknown date'}
        </div>
      </div>

      {/* Status badge */}
      <span style={{
        padding: '4px 12px', borderRadius: '8px',
        background: 'var(--bg-input)',
        fontSize: '12px', fontWeight: 600, color: s.dot,
        whiteSpace: 'nowrap', flexShrink: 0,
      }}>{s.label}</span>

      <TbChevronRight
        size={16}
        style={{
          color: 'var(--text-muted)', flexShrink: 0,
          transform: isActive ? 'rotate(90deg)' : 'none',
          transition: 'transform 0.2s',
        }}
      />
    </div>
  );
}