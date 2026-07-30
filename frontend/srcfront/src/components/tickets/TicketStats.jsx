import { TbTicket, TbClockHour4, TbCircleCheck } from 'react-icons/tb';
import {  STAT_CONFIG } from './Ticketconstants';


/**
 * @param {{ tickets: Array }} props
 */
export default function TicketStats({ tickets = [] }) {
  const counts = {
    open:       tickets.filter(t => t.status === 'OPEN').length,
    inProgress: tickets.filter(t => t.status === 'IN_PROGRESS').length,
    closed:     tickets.filter(t => t.status === 'CLOSED').length,
  };

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(3, 1fr)',
      gap: '16px',
      marginBottom: '32px',
    }}>
      {STAT_CONFIG.map(({ key, label, Icon, color, bg, border }) => (
        <div key={key} style={{
          background: 'var(--bg-card)',
          border: `1px solid ${border}`,
          borderRadius: '18px',
          padding: '22px 24px',
          display: 'flex', alignItems: 'center', gap: '16px',
        }}>
          <div style={{
            width: '46px', height: '46px', borderRadius: '12px',
            background: bg,
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <Icon size={22} style={{ color }} />
          </div>
          <div>
            <div style={{ fontSize: '28px', fontWeight: 700, color: 'var(--text-main)', lineHeight: 1 }}>
              {counts[key]}
            </div>
            <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginTop: '4px' }}>{label}</div>
          </div>
        </div>
      ))}
    </div>
  );
}