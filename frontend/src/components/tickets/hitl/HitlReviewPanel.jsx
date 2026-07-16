import { useState, useEffect } from 'react';
import { TbRobot } from 'react-icons/tb';
import { runHitlReview, getLatestHitlReview, submitHumanDecision } from '../../../services/api';
import { Spinner, ErrorBox, SpinKeyframes } from './HitlUi';
import HitlPendingReview from './HitlPendingReview';
import HitlFinalizedResult from './HitlFinalizedResult';
import HitlRejectedResult from './HitlRejectedResult';

/**
 * Human-in-the-Loop Agent Review panel (S5).
 *
 * Owns the full client-side state machine for a ticket's HITL run:
 *  - no run yet            -> "Run HITL Agent Review" button
 *  - WAITING_FOR_HUMAN      -> HitlPendingReview (draft + decision form)
 *  - REVISING (transient)   -> spinner
 *  - FINALIZED              -> HitlFinalizedResult
 *  - REJECTED                -> HitlRejectedResult
 *  - FAILED                  -> ErrorBox
 *
 * On mount (and whenever `ticketId` changes), it reloads the latest stored
 * HITL run from the backend so a pending review survives a page refresh
 * (S5 §2.7) — it never relies on frontend-only memory.
 */
export default function HitlReviewPanel({ ticketId }) {
  const [review, setReview] = useState(null);       // latest HitlReviewResponse from backend
  const [loading, setLoading] = useState(false);     // running the initial agent investigation
  const [deciding, setDeciding] = useState(null);    // 'APPROVE' | 'REJECT' | 'REQUEST_REVISION' | null
  const [error, setError] = useState(null);
  const [reloading, setReloading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setReview(null);
    setError(null);
    setReloading(true);

    if (!ticketId) {
      setReloading(false);
      return;
    }

    getLatestHitlReview(ticketId)
      .then(data => { if (!cancelled) setReview(data); })
      .catch(() => { if (!cancelled) setReview(null); })
      .finally(() => { if (!cancelled) setReloading(false); });

    return () => { cancelled = true; };
  }, [ticketId]);

  const handleRun = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await runHitlReview(ticketId);
      setReview(data);
      if (data?.status === 'FAILED') {
        setError(data.errorMessage ?? "The HITL agent failed.");
      }
    } catch (e) {
      setError(e.response?.data?.message ?? "Could not start the HITL review.");
    } finally {
      setLoading(false);
    }
  };

  const handleDecide = async (decision, comment) => {
    if (!review?.runId) return;
    setDeciding(decision);
    setError(null);
    try {
      const decisionResponse = await submitHumanDecision(review.runId, decision, comment);

      if (decision === 'REQUEST_REVISION' && decisionResponse.revisedReview) {
        setReview(decisionResponse.revisedReview);
      } else {
        // APPROVE / REJECT -> reload the authoritative persisted state
        const reloaded = await getLatestHitlReview(ticketId);
        setReview(reloaded);
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'The human decision failed.');
    } finally {
      setDeciding(null);
    }
  };

  return (
    <>
      <SpinKeyframes />

      {reloading && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
          <Spinner color="#facc15" /> Loading HITL review...
        </div>
      )}

      {!reloading && !review && (
        <button onClick={handleRun} disabled={loading} style={{
          width: '100%', padding: '13px',
          background: loading ? 'rgba(250,204,21,0.05)' : 'rgba(250,204,21,0.10)',
          border: '1px solid rgba(250,204,21,0.3)', borderRadius: '12px',
          color: '#facc15', fontSize: '14px', fontWeight: 600,
          cursor: loading ? 'not-allowed' : 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
        }}>
          {loading ? <Spinner color="#facc15" /> : <TbRobot size={16} />}
          {loading ? "Running agent investigation..." : 'Run HITL Agent Review'}
        </button>
      )}

      {error && <ErrorBox>{error}</ErrorBox>}

      {!reloading && review && review.status === 'FAILED' && !error && (
        <ErrorBox>{review.errorMessage ?? "The HITL agent failed."}</ErrorBox>
      )}

      {!reloading && review && review.status === 'REVISING' && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)', marginTop: '12px' }}>
          <Spinner color="#a78bfa" /> Generating revised draft...
        </div>
      )}

      {!reloading && review && review.status === 'WAITING_FOR_HUMAN' && (
        <div style={{ marginTop: '12px' }}>
          <HitlPendingReview review={review} onDecide={handleDecide} deciding={deciding} />
        </div>
      )}

      {!reloading && review && review.status === 'FINALIZED' && (
        <div style={{ marginTop: '12px' }}>
          <HitlFinalizedResult
            result={review.finalReviewedResult}
            humanComment={review.humanComment}
            finalizedAt={review.createdAt}
          />
        </div>
      )}

      {!reloading && review && review.status === 'REJECTED' && (
        <div style={{ marginTop: '12px' }}>
          <HitlRejectedResult review={review} />
        </div>
      )}
    </>
  );
}
