import { TbBook2 } from 'react-icons/tb';

const FILTERS = ['All', 'Conveyor', 'Motor', 'Pump', 'Sensor', 'Safety'];

/**
 * @param {{ filter: string, count: number, onChange: (f: string) => void }} props
 */
export default function ArticleFilters({ filter, count, onChange }) {
  return (
    <div style={{
      padding: '20px 24px',
      borderBottom: '1px solid rgba(77,124,199,0.1)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <TbBook2 size={20} style={{ color: 'var(--text-muted)' }} />
        <span style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-main)' }}>Knowledge Articles</span>
        <span style={{
          padding: '2px 8px', borderRadius: '20px',
          background: 'rgba(77,124,199,0.12)',
          fontSize: '12px', fontWeight: 600, color: '#60a5fa',
        }}>{count}</span>
      </div>

      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
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