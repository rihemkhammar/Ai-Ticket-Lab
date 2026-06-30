import { ARTICLE_STAT_CONFIG } from './Articleconstants';

/**
 * @param {{ articles: Array, totalChunks?: number }} props
 */
export default function ArticleStats({ articles = [], totalChunks = 0 }) {
  const counts = {
    total:  articles.length,
    chunks: totalChunks,
    safety: articles.filter(a => a.category === 'SAFETY').length,
  };

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(3, 1fr)',
      gap: '16px',
      marginBottom: '32px',
    }}>
      {ARTICLE_STAT_CONFIG.map(({ key, label, Icon, color, bg, border }) => (
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