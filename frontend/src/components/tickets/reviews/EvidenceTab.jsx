import { TbSearch } from 'react-icons/tb';
import { Spinner, ErrorBox, ResultPreview } from '../TicketDetailUi';

/** Bouton "Get Evidence" + aperçu du résultat, affiché dans le panneau latéral. */
export function EvidencePanel({ evidenceLoading, evidenceError, evidenceResult, onRun, onOpen }) {
  return (
    <>
      <button onClick={onRun} disabled={evidenceLoading} style={{
        width: '100%', padding: '13px',
        background: evidenceLoading ? 'rgba(96,165,250,0.05)' : 'rgba(96,165,250,0.10)',
        border: '1px solid rgba(96,165,250,0.3)', borderRadius: '12px',
        color: '#60a5fa', fontSize: '14px', fontWeight: 600,
        cursor: evidenceLoading ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
      }}>
        {evidenceLoading ? <Spinner color="#60a5fa" /> : <TbSearch size={16} />}
        {evidenceLoading ? 'Fetching Evidence...' : 'Get Evidence'}
      </button>
      {evidenceError && <ErrorBox>{evidenceError}</ErrorBox>}
      {evidenceResult && (
        <ResultPreview
          color="#60a5fa"
          label={`Evidence — ${evidenceResult.evidence?.length ?? 0} chunks`}
          onOpen={onOpen}
        />
      )}
    </>
  );
}

/** Contenu détaillé affiché dans la modale plein écran. */
export function EvidenceModalContent({ evidenceResult }) {
  if (!evidenceResult) return null;
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', fontWeight: 700, color: '#60a5fa', letterSpacing: '0.5px', marginBottom: '16px' }}>
        <TbSearch size={14} /> EVIDENCE — {evidenceResult.evidence?.length ?? 0} chunks
      </div>
      {(evidenceResult.evidence ?? []).length === 0 ? (
        <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Aucun chunk trouvé.</p>
      ) : (
        (evidenceResult.evidence ?? []).map((chunk, i) => (
          <div key={i} style={{
            marginBottom: '12px', padding: '12px 14px', borderRadius: '12px',
            background: 'rgba(96,165,250,0.05)', border: '1px solid rgba(96,165,250,0.15)',
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ fontSize: '11px', fontWeight: 600, color: '#60a5fa' }}>Chunk #{i + 1}</span>
              {chunk.score !== undefined && (
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  score: {typeof chunk.score === 'number' ? chunk.score.toFixed(4) : chunk.score}
                </span>
              )}
            </div>
            {chunk.articleTitle && (
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>{chunk.articleTitle}</div>
            )}
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: 0, lineHeight: 1.6 }}>
              {chunk.content ?? chunk.text ?? JSON.stringify(chunk)}
            </p>
          </div>
        ))
      )}
    </>
  );
}