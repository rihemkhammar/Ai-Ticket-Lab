import { TbChevronRight } from 'react-icons/tb';
import { CATEGORY_CONFIG } from './Articleconstants';

const DEFAULT_CAT = { dot: '#94a3b8', color: '#94a3b8', label: '—' };

export default function ArticleRow({ article, isActive, isLast, onClick }) {
  const cat = CATEGORY_CONFIG[article.category] ?? DEFAULT_CAT;

  return (
    <div
      onClick={onClick}
      style={{
        padding: '16px 24px',
        borderBottom: isLast ? 'none' : '1px solid rgba(77,124,199,0.08)',
        display: 'flex', alignItems: 'center', gap: '16px',
        cursor: 'pointer',
        background: isActive ? 'rgba(77,124,199,0.07)' : 'transparent',
        transition: 'background 0.15s',
      }}
    >
      {/* Dot  */}
      <span style={{
        width: '10px', height: '10px', borderRadius: '50%',
        background: cat.dot ?? cat.color, flexShrink: 0,
      }} />

      {/* Main info */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: '14px', fontWeight: 500, color: 'var(--text-main)',
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          marginBottom: '3px',
        }}>{article.title}</div>

        <div style={{
          fontSize: '12px', color: 'var(--text-muted)',
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>
          {article.content?.slice(0, 100) ?? '—'}
        </div>
      </div>

      {/* Badge catégorie */}
      <span style={{
        padding: '4px 12px', borderRadius: '8px',
        background: 'var(--bg-input)',
        fontSize: '12px', fontWeight: 600, color: cat.color,
        whiteSpace: 'nowrap', flexShrink: 0,
      }}>{cat.label}</span>

      <TbChevronRight
        size={16}
        style={{
          color: 'var(--text-muted)', flexShrink: 0,
          transform: isActive ? 'rotate(90deg)' : 'none',
          transition: 'transform 0.2s',
        }}
      />
    </div>
  );
}