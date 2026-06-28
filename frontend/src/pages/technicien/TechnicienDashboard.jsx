import { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { getCurrentUser, getAllTickets, getAllArticles } from '../../services/api';
import { TbArrowRight } from 'react-icons/tb';

import TicketStats  from '../../components/tickets/TicketStats';
import ArticleStats from '../../components/articles/Articlestats';
import { STATUS_CONFIG } from '../../components/tickets/Ticketconstants';

import { CATEGORY_CONFIG } from '../../components/articles/Articleconstants';




const DEFAULT_CATEGORY = { dot: '#94a3b8', bg: 'rgba(148,163,184,0.12)', text: '#94a3b8' };

export default function TechnicianDashboard() {
  const user     = getCurrentUser();
  const username = user?.username ?? 'Technician';

  const [tickets,  setTickets]  = useState([]);
  const [articles, setArticles] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);

  /* ── Fetch ── */
  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [ticketData, articleData] = await Promise.all([getAllTickets(), getAllArticles()]);
        const ticketList  = Array.isArray(ticketData)  ? ticketData  : (ticketData?.data  ?? ticketData?.content  ?? []);
        const articleList = Array.isArray(articleData) ? articleData : (articleData?.data ?? articleData?.content ?? []);
        if (!cancelled) {
          setTickets(ticketList);
          setArticles(articleList);
        }
      } catch (err) {
        console.error('[TechnicianDashboard] load error:', err);
        if (!cancelled) setError('Failed to load dashboard data.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, []);

  /* ── Derived lists ── */
  const recentTickets = [...tickets]
    .sort((a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0))
    .slice(0, 5);

  const recentArticles = [...articles]
    .sort((a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0))
    .slice(0, 5);

  /* ── Shared card style ── */
  const listCard = {
    background: 'var(--bg-card)',
    border: '1px solid rgba(96,165,250,0.1)',
    borderRadius: '20px',
    overflow: 'hidden',
    marginBottom: '32px',
  };

  const rowBase = (isLast) => ({
    padding: '14px 24px',
    borderBottom: isLast ? 'none' : '1px solid rgba(77,124,199,0.08)',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  });

  const emptyState = (
    <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
      {loading ? 'Loading…' : 'No items yet.'}
    </div>
  );

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', width: '100%', margin: '0 auto', boxSizing: 'border-box' }}>

      {/* Welcome */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 700, color: 'var(--text-main)', margin: 0, letterSpacing: '-0.3px' }}>
          Good morning, {username} 👋
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
          {loading
            ? 'Loading your workspace…'
            : `${tickets.length} ticket${tickets.length !== 1 ? 's' : ''} · ${articles.length} knowledge article${articles.length !== 1 ? 's' : ''}`
          }
        </p>
      </div>

      {/* Error */}
      {error && (
        <div style={{
          padding: '16px 20px', borderRadius: '12px',
          background: 'rgba(248,113,113,0.08)',
          border: '1px solid rgba(248,113,113,0.3)',
          color: '#f87171', fontSize: '14px', marginBottom: '20px',
        }}>
          {error}
        </div>
      )}

      {/* ── Tickets overview ── */}
      <SectionHeader title="Tickets overview" to="/technician/tickets" />
      <TicketStats tickets={tickets} />

      <div style={listCard}>
        {loading || recentTickets.length === 0 ? emptyState : recentTickets.map((t, i) => {
          const s = STATUS_CONFIG[t.status] ?? STATUS_CONFIG.OPEN;
          return (
            <div key={t.id} style={rowBase(i === recentTickets.length - 1)}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: s.dot, flexShrink: 0 }} />
              <span style={{
                flex: 1, fontSize: '14px', color: 'var(--text-main)',
                whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              }}>
                {t.title}
              </span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{s.label}</span>
            </div>
          );
        })}
      </div>
      {/* ── Knowledge articles ── */}
      <SectionHeader title="Knowledge articles" to="/technician/articles" />
       {/* Article stats */}
      <ArticleStats articles={articles} />

      

      <div style={listCard}>
        {loading || recentArticles.length === 0 ? emptyState : recentArticles.map((a, i) => {
          const c = CATEGORY_CONFIG[a.category] ?? DEFAULT_CATEGORY;
          return (
            <div key={a.id} style={rowBase(i === recentArticles.length - 1)}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
              <span style={{
                flex: 1, fontSize: '14px', color: 'var(--text-main)',
                whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              }}>
                {a.title}
              </span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                {a.category}
              </span>
            </div>
          );
        })}
      </div>

     
    </div>
  );
}

/* ── Section header (unchanged) ── */
function SectionHeader({ title, to }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
      <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-main)', margin: 0 }}>{title}</h2>
      <NavLink
        to={to}
        style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', fontWeight: 600, color: '#60a5fa', textDecoration: 'none' }}
      >
        View all <TbArrowRight size={14} />
      </NavLink>
    </div>
  );
}