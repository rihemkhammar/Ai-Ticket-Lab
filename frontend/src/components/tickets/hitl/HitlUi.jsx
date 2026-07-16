import { TbAlertTriangle } from 'react-icons/tb';

// Small shared UI helpers duplicated locally (Spinner/ErrorBox/Section already
// exist in TicketDetailPanel.jsx but aren't exported there). Keeping them
// here avoids touching the large existing file just for an export.

export function Spinner({ color }) {
  return (
    <span style={{
      width: '14px', height: '14px', border: `2px solid ${color}`,
      borderTopColor: 'transparent', borderRadius: '50%',
      display: 'inline-block', animation: 'hitl-spin 0.8s linear infinite',
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

export function Badge({ color, children }) {
  return (
    <span style={{
      fontSize: '11px', fontWeight: 600, padding: '2px 10px', borderRadius: '20px',
      color, background: `${color}18`, border: `1px solid ${color}33`,
    }}>
      {children}
    </span>
  );
}

export function SpinKeyframes() {
  return <style>{`@keyframes hitl-spin { to { transform: rotate(360deg); } }`}</style>;
}
