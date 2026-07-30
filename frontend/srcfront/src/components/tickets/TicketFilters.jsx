import { MdOutlinePendingActions } from 'react-icons/md';

const FILTERS = ['All', 'Open', 'In Progress', 'Closed'];

/**
 * @param {{ filter: string, count: number, onChange: (f: string) => void }} props
 */
export default function TicketFilters({ filter, count, onChange }) {
  return (
    <div style={{
      padding: '20px 24px',
      borderBottom: '1px solid rgba(77,124,199,0.1)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <MdOutlinePendingActions size={20} style={{ color: 'var(--text-muted)' }} />
        <span style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>My Tickets</span>
        <span style={{
          padding: '2px 8px', borderRadius: '20px',
          background: 'rgba(77,124,199,0.12)',
          fontSize: '12px', fontWeight: 600, color: '#60a5fa',
        }}>{count}</span>
      </div>

      <div style={{ display: 'flex', gap: '6px' }}>
        {FILTERS.map(f => (
          <button
            key={f}
            onClick={() => onChange(f)}
            style={{
              padding: '6px 14px',
              borderRadius: '8px',
              background: filter === f ? 'var(--btn-gradient)' : 'var(--bg-input)',
              border: `1px solid ${filter === f ? 'transparent' : 'rgba(77,124,199,0.2)'}`,
              color: filter === f ? '#fff' : 'var(--text-muted)',
              fontSize: '12px', fontWeight: 600, cursor: 'pointer',
              transition: 'all 0.15s',
            }}
          >{f}</button>
        ))}
      </div>
    </div>
  );
}

