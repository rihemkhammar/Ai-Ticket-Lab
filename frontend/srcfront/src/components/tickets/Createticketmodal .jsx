import { useState } from 'react';
import { TbX, TbTicket } from 'react-icons/tb';
import { createTicket } from '../../services/api';

export default function CreateTicketModal({ onClose, onCreated }) {
  const [title,       setTitle]       = useState('');
  const [description, setDescription] = useState('');
  const [submitting,  setSubmitting]  = useState(false);
  const [error,       setError]       = useState(null);

  const handleSubmit = async () => {
    if (!title.trim() || !description.trim()) {
      setError('Title and description are required.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const created = await createTicket({ title: title.trim(), description: description.trim() });
      onCreated(created);
      onClose();
    } catch (err) {
      console.error('[CreateTicketModal] error:', err);
      setError(err?.response?.data?.message ?? 'Failed to create ticket.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(6,11,25,0.7)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 500 }}>
      <div onClick={e => e.stopPropagation()} style={{ width: '100%', maxWidth: '480px', background: 'var(--bg-card)', border: '1px solid rgba(77,124,199,0.2)', borderRadius: '20px', padding: '28px', boxShadow: '0 24px 64px rgba(6,11,25,0.6)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ width: '36px', height: '36px', borderRadius: '10px', background: 'var(--btn-gradient)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <TbTicket size={18} style={{ color: '#fff' }} />
            </div>
            <span style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-main)' }}>New Ticket</span>
          </div>
          <button onClick={onClose} style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: 'var(--text-muted)' }}>
            <TbX size={16} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '20px' }}>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }}>TITLE</label>
            <input type="text" value={title} onChange={e => setTitle(e.target.value)} placeholder="Short description of the issue" style={{ width: '100%', padding: '11px 14px', background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)', borderRadius: '10px', color: 'var(--text-main)', fontSize: '14px', outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }}>DESCRIPTION</label>
            <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder="Describe the problem in detail…" rows={4} style={{ width: '100%', padding: '11px 14px', background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)', borderRadius: '10px', color: 'var(--text-main)', fontSize: '14px', outline: 'none', resize: 'vertical', boxSizing: 'border-box', fontFamily: 'sans-serif' }} />
          </div>
        </div>

        {error && <div style={{ padding: '10px 14px', borderRadius: '10px', background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.3)', color: '#f87171', fontSize: '13px', marginBottom: '16px' }}>{error}</div>}

        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={onClose} style={{ flex: 1, padding: '12px', background: 'var(--bg-input)', border: '1px solid rgba(77,124,199,0.2)', borderRadius: '12px', color: 'var(--text-muted)', fontSize: '14px', fontWeight: 600, cursor: 'pointer' }}>Cancel</button>
          <button onClick={handleSubmit} disabled={submitting} style={{ flex: 2, padding: '12px', background: submitting ? 'rgba(77,124,199,0.3)' : 'var(--btn-gradient)', border: 'none', borderRadius: '12px', color: '#fff', fontSize: '14px', fontWeight: 600, cursor: submitting ? 'not-allowed' : 'pointer' }}>
            {submitting ? 'Creating…' : 'Create Ticket'}
          </button>
        </div>
      </div>
    </div>
  );
}