import { Outlet, NavLink } from 'react-router-dom';
import { TbRobot, TbTicket, TbLogout, TbUser } from 'react-icons/tb';
import { MdDashboard, MdOutlineLibraryBooks } from 'react-icons/md';
import { getCurrentUser, logout } from '../../services/api';

const NAV_ITEMS = [
  { to: '/technician/dashboard', label: 'Dashboard',          Icon: MdDashboard,          end: true },
  { to: '/technician/tickets',  label: 'Tickets',            Icon: TbTicket,              end: false },
  { to: '/technician/articles', label: 'Knowledge Articles', Icon: MdOutlineLibraryBooks, end: false },
];

export default function TechnicienLayout() {
  const user     = getCurrentUser();
  const username = user?.username ?? user?.email ?? 'Technician';

  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: 'var(--bg-main)', fontFamily: 'sans-serif' }}>
      {/* Sidebar */}
      <aside style={{
        width: '240px', flexShrink: 0,
        background: 'var(--bg-card)',
        borderRight: '1px solid rgba(77,124,199,0.15)',
        display: 'flex', flexDirection: 'column',
        padding: '24px 16px',
        position: 'sticky', top: 0, height: '100vh',
      }}>
        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '0 8px', marginBottom: '32px' }}>
          <div style={{
            width: '38px', height: '38px', borderRadius: '10px',
            background: 'var(--btn-gradient)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <TbRobot size={20} style={{ color: '#fff' }} />
          </div>
          <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-main)' }}>AI Maintenance</span>
        </div>

        {/* Nav */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
          {NAV_ITEMS.map(({ to, label, Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              style={({ isActive }) => ({
                display: 'flex', alignItems: 'center', gap: '12px',
                padding: '11px 14px', borderRadius: '12px',
                fontSize: '14px', fontWeight: 600, textDecoration: 'none',
                color: isActive ? '#fff' : 'var(--text-muted)',
                background: isActive ? 'var(--btn-gradient)' : 'transparent',
                transition: 'all 0.15s',
              })}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User + Logout */}
        <div style={{ borderTop: '1px solid rgba(77,124,199,0.1)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '0 8px' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%',
              background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <TbUser size={16} style={{ color: 'var(--text-muted)' }} />
            </div>
            <span style={{
              fontSize: '13px', fontWeight: 600, color: 'var(--text-main)',
              whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            }}>
              {username}
            </span>
          </div>

          <button
            onClick={logout}
            style={{
              display: 'flex', alignItems: 'center', gap: '10px',
              padding: '10px 14px', borderRadius: '12px',
              background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.25)',
              color: '#f87171', fontSize: '13px', fontWeight: 600,
              cursor: 'pointer', width: '100%',
            }}
          >
            <TbLogout size={16} /> Log out
          </button>
        </div>
      </aside>

      {/* Page content */}
      <main style={{ flex: 1, minWidth: 0, overflowX: 'auto' }}>
        <Outlet />
      </main>

      <style>{`* { margin: 0; padding: 0; box-sizing: border-box; }`}</style>
    </div>
  );
}