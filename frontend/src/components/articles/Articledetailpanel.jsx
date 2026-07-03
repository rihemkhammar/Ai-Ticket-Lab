import { useState, useEffect } from 'react';
import { getArticleById } from '../../services/api';
import { CATEGORY_CONFIG } from './Articleconstants';

export default function ArticleDetailPanel({ article, onClose }) {
  const [detail,  setDetail]  = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);

  useEffect(() => {
    if (!article?.id) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setDetail(null);

    getArticleById(article.id)
      .then(data => { if (!cancelled) setDetail(data); })
      .catch(err => {
        console.error('[ArticleDetailPanel] getArticleById error:', err);
        if (!cancelled) setError("Impossible de charger l'article.");
      })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [article?.id]);

  const cat  = CATEGORY_CONFIG[article.category] ?? { color: '#94a3b8', label: article.category ?? '—' };
  const data = detail ?? article;

  return (
    <div style={{
      background: 'var(--bg-card)',
      border: '1px solid rgba(77,124,199,0.15)',
      borderRadius: '20px',
      padding: '24px',
      position: 'sticky', top: '80px',
    }}>
      {/* Header  */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
        <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-muted)', fontFamily: 'monospace' }}>
          #{data.id?.toString().slice(-6) ?? '------'}
        </span>
        <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '20px', lineHeight: 1 }}>×</button>
      </div>

      <h2 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--text-main)', margin: '0 0 20px', lineHeight: 1.4 }}>
        {data.title}
      </h2>

      {loading ? (
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px', padding: '32px 0' }}>
          Loading details…
        </div>
      ) : error ? (
        <div style={{ color: '#f87171', fontSize: '13px' }}>{error}</div>
      ) : (
        <>
          {/* Meta — même style que Status / Category / Opened côté Ticket */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
            {[
              { label: 'Category', val: (
                <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: cat.color, display: 'inline-block' }} />
                  {cat.label}
                </span>
              )},
              { label: 'Created', val: (data.created_at || data.createdAt)
                ? new Date(data.created_at ?? data.createdAt).toLocaleDateString('fr-BE', { day: '2-digit', month: 'short', year: 'numeric' })
                : '—' },
              { label: 'Updated', val: (data.updated_at || data.updatedAt)
                ? new Date(data.updated_at ?? data.updatedAt).toLocaleDateString('fr-BE', { day: '2-digit', month: 'short', year: 'numeric' })
                : '—' },
            ].map(({ label, val }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{label}</span>
                <span style={{ fontSize: '13px', color: 'var(--text-main)', fontWeight: 500 }}>{val}</span>
              </div>
            ))}
          </div>

          <div style={{ borderTop: '1px solid rgba(77,124,199,0.1)', marginBottom: '20px' }} />

          {/* Summary */}
          {data.summary && (
            <div style={{ marginBottom: '24px' }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '10px' }}>
                SUMMARY
              </div>
              <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, opacity: 0.7 }}>
                {data.summary}
              </p>
            </div>
          )}

          {/* Content  */}
          <div style={{ marginBottom: '24px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', marginBottom: '10px' }}>
              CONTENT
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-main)', lineHeight: 1.7, margin: 0, opacity: 0.7, whiteSpace: 'pre-wrap' }}>
              {data.content || 'No content provided.'}
            </p>
          </div>
        </>
      )}
    </div>
  );
}