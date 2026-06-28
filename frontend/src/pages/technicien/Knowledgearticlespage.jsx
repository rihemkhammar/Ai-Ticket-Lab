import { useState, useEffect } from 'react';
import { getAllArticles } from '../../services/api';

import ArticleStats          from '../../components/articles/Articlestats';
import ArticleFilters        from '../../components/articles/Articlefilters';
import ArticleRow            from '../../components/articles/Articlerow';
import ArticleDetailPanel    from '../../components/articles/Articledetailpanel';
import { FILTER_MAP }        from '../../components/articles/Articleconstants';


export default function KnowledgeArticlesPage() {
  const [articles,      setArticles]      = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [error,         setError]         = useState(null);
  const [filter,        setFilter]        = useState('All');
  const [selectedId,    setSelectedId]    = useState(null);
  const [retry,         setRetry]         = useState(0);

  /* ── Fetch ── */
  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getAllArticles();
        const list = Array.isArray(data) ? data : (data?.data ?? data?.content ?? []);
        if (!cancelled) setArticles(list);
      } catch (err) {
        console.error('[KnowledgeArticlesPage] fetchArticles error:', err);
        if (!cancelled) setError('Failed to load knowledge articles.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [retry]);

  const handleRetry = () => setRetry(r => r + 1);

  /* ── Filter ── */
  const filtered = articles.filter(a => {
    const categoryFilter = FILTER_MAP[filter];
    return categoryFilter ? a.category === categoryFilter : true;
  });

  const selectedArticle = selectedId ? articles.find(a => a.id === selectedId) ?? null : null;

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', width: '100%', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Header */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 700, color: 'var(--text-main)', margin: 0, letterSpacing: '-0.3px' }}>
          Knowledge Articles
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--text-muted)', margin: '6px 0 0' }}>
          {loading
            ? 'Loading knowledge articles…'
            : <>{articles.length} article{articles.length !== 1 ? 's' : ''} in total</>
          }
        </p>
      </div>

      {/* Stats */}
      <ArticleStats articles={articles} />

      {/* Error state */}
      {error && (
        <div style={{
          padding: '16px 20px', borderRadius: '12px',
          background: 'rgba(248,113,113,0.08)',
          border: '1px solid rgba(248,113,113,0.3)',
          color: '#f87171', fontSize: '14px', marginBottom: '20px',
        }}>
          {error}{' '}
          <button
            onClick={handleRetry}
            style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', textDecoration: 'underline', fontSize: '14px' }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Article list + detail */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: selectedArticle ? '1fr 380px' : '1fr',
        gap: '20px',
        alignItems: 'start',
      }}>
        <div style={{
          background: 'var(--bg-card)',
          border: '1px solid rgba(77,124,199,0.15)',
          borderRadius: '20px',
          overflow: 'hidden',
        }}>
          <ArticleFilters
            filter={filter}
            count={filtered.length}
            onChange={f => { setFilter(f); setSelectedId(null); }}
          />

          {loading ? (
            <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
              Loading…
            </div>
          ) : filtered.length === 0 ? (
            <div style={{ padding: '48px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
              No articles in this category.
            </div>
          ) : (
            filtered.map((article, i) => (
              <ArticleRow
                key={article.id}
                article={article}
                isActive={selectedId === article.id}
                isLast={i === filtered.length - 1}
                onClick={() => setSelectedId(prev => prev === article.id ? null : article.id)}
              />
            ))
          )}
        </div>

        {selectedArticle && (
          <ArticleDetailPanel
            article={selectedArticle}
            onClose={() => setSelectedId(null)}
          />
        )}
      </div>

      <style>{`
        @media (max-width: 900px) {
          div[style*="grid-template-columns"] { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}