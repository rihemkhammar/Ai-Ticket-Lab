import { TbCheck, TbAlertTriangle } from 'react-icons/tb';

export function ResultPreview({ color, label, confidence, confConfig, needsHuman, onOpen }) {
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

export function Spinner({ color }) {
  return (
    <span style={{
      width: '14px', height: '14px', border: `2px solid ${color}`,
      borderTopColor: 'transparent', borderRadius: '50%',
      display: 'inline-block', animation: 'spin 0.8s linear infinite',
    }} />
  );
}

export function ErrorBox({ children }) {
  return (
    <div style={{ marginTop: '12px', padding: '12px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
      <TbAlertTriangle size={14} color="#f87171" />
      <span style={{ fontSize: '13px', color: '#f87171' }}>{children}</span>
    </div>
  );
}

export function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '16px' }}>
      <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '6px', textTransform: 'uppercase' }}>
        {title}
      </div>
      {children}
    </div>
  );
}