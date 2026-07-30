import { Spinner, ErrorBox, SpinKeyframes } from './HitlUi';
import HitlPendingReview from './HitlPendingReview';
import HitlFinalizedResult from './HitlFinalizedResult';
import HitlRejectedResult from './HitlRejectedResult';

/**
 * Full HITL content shown in the modal ("Voir →" from HitlPanel): the
 * pending decision form, or the finalized/rejected/failed result — the
 * counterpart of AiReviewModalContent / RagReviewModalContent / etc.
 */
export function HitlModalContent({ review, error, deciding, onDecide }) {
  if (!review) return null;

  return (
    <>
      <SpinKeyframes />

      {error && <ErrorBox>{error}</ErrorBox>}

      {review.status === 'FAILED' && !error && (
        <ErrorBox>{review.errorMessage ?? "The HITL agent failed."}</ErrorBox>
      )}

      {review.status === 'REVISING' && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-muted)' }}>
          <Spinner color="#a78bfa" /> Generating revised draft...
        </div>
      )}

      {review.status === 'WAITING_FOR_HUMAN' && (
        <HitlPendingReview review={review} onDecide={onDecide} deciding={deciding} />
      )}

      {review.status === 'FINALIZED' && (
        <HitlFinalizedResult
          result={review.finalReviewedResult}
          humanComment={review.humanComment}
          finalizedAt={review.finalizedAt}
        />
      )}

      {review.status === 'REJECTED' && (
        <HitlRejectedResult review={review} />
      )}
    </>
  );
}