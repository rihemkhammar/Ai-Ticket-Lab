import { useState } from 'react';
import { TbRobot, TbBell, TbLogout } from 'react-icons/tb';
import { logout, getCurrentUser } from '../services/api';

/**
 * @param {{ role?: string, notifications?: Array<{text: string, time: string, dot: string}> }} props
 */
export default function TopBar({ role = 'Technician', notifications = [] }) {
  const user     = getCurrentUser();
  const username = user?.username ?? user?.email ?? role;
  const [notifOpen, setNotifOpen] = useState(false);

  return (
    <header style={{
      height: '64px',
      background: 'var(--bg-card)',
      borderBottom: '1px solid rgba(77,124,199,0.15)',
      display: 'flex', alignItems: 'center',
      padding: '0 32px', gap: '16px',
      position: 'sticky', top: 0, zIndex: 100,
    }}>
      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginRight: 'auto' }}>
        <div style={{
          width: '36px', height: '36px', borderRadius: '10px',
          background: 'var(--btn-gradient)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <TbRobot style={{ width: '20px', height: '20px', color: '#fff' }} />
        </div>
        <span style={{ fontSize: '17px', fontWeight: 700, color: 'var(--text-main)', letterSpacing: '-0.2px' }}>
          AI Ticket{' '}
          <span style={{ background: 'var(--btn-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
            Lab
          </span>
        </span>
        <span style={{
          marginLeft: '8px', fontSize: '11px', fontWeight: 600,
          letterSpacing: '2px', textTransform: 'uppercase',
          color: 'var(--text-muted)', opacity: 0.6,
          borderLeft: '1px solid rgba(77,124,199,0.2)', paddingLeft: '12px',
        }}>{role}</span>
      </div>

      {/* Notification bell */}
      <div style={{ position: 'relative' }}>
        <button
          onClick={() => setNotifOpen(o => !o)}
          style={{
            width: '38px', height: '38px', borderRadius: '10px',
            background: notifOpen ? 'rgba(77,124,199,0.15)' : 'var(--bg-input)',
            border: '1px solid rgba(77,124,199,0.2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer', color: 'var(--text-muted)', position: 'relative',
          }}
        >
          <TbBell size={18} />
          {notifications.length > 0 && (
            <span style={{
              position: 'absolute', top: '6px', right: '6px',
              width: '8px', height: '8px', borderRadius: '50%',
              background: '#f87171', border: '2px solid var(--bg-card)',
            }} />
          )}
        </button>

        {notifOpen && (
          <div style={{
            position: 'absolute', top: '46px', right: 0,
            width: '300px',
            background: 'var(--bg-card)',
            border: '1px solid rgba(77,124,199,0.2)',
            borderRadius: '14px',
            boxShadow: '0 8px 32px rgba(6,11,25,0.5)',
            padding: '16px',
            zIndex: 200,
          }}>
            <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '12px', letterSpacing: '0.5px' }}>
              NOTIFICATIONS
            </div>
            {notifications.length === 0 && (
              <div style={{ fontSize: '13px', color: 'var(--text-muted)', textAlign: 'center', padding: '12px 0' }}>
                No notifications
              </div>
            )}
            {notifications.map((n, i) => (
              <div key={i} style={{
                display: 'flex', gap: '10px', alignItems: 'flex-start',
                padding: '10px 0',
                borderBottom: i < notifications.length - 1 ? '1px solid rgba(77,124,199,0.1)' : 'none',
              }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: n.dot, flexShrink: 0, marginTop: '5px' }} />
                <div>
                  <div style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.4 }}>{n.text}</div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>{n.time}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Avatar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <div style={{
          width: '36px', height: '36px', borderRadius: '10px',
          background: 'var(--btn-gradient)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '14px', fontWeight: 700, color: '#fff',
        }}>
          {username.charAt(0).toUpperCase()}
        </div>
        <span style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text-main)' }}>{username}</span>
      </div>

      {/* Logout */}
      <button
        onClick={() => logout()}
        style={{
          display: 'flex', alignItems: 'center', gap: '6px',
          padding: '8px 14px',
          background: 'rgba(248,113,113,0.08)',
          border: '1px solid rgba(248,113,113,0.2)',
          borderRadius: '10px',
          color: '#f87171', fontSize: '13px', fontWeight: 600,
          cursor: 'pointer',
        }}
      >
        <TbLogout size={16} /> Sign out
      </button>
    </header>
  );
}