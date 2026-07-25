import { useMemo, useState } from 'react';
import axios from 'axios';
import { reviewApi } from '../api/reviewApi';
import { REVIEW_CRITERIA } from '../config/reviewCriteria';
import type { ReviewableAssignment } from '../types/reviewTypes';

type ReviewFormModalProps = {
  assignment: ReviewableAssignment;
  onClose: () => void;
  onSubmitted: () => void;
};

const MAX_COMMENT = 1000;

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export function ReviewFormModal({ assignment, onClose, onSubmitted }: ReviewFormModalProps) {
  // code tieu chi -> so sao da chon
  const [scores, setScores] = useState<Record<string, number>>({});
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const answered = Object.keys(scores).length;
  const total = REVIEW_CRITERIA.length;
  const overall = useMemo(() => {
    if (answered === 0) return 0;
    const sum = Object.values(scores).reduce((a, b) => a + b, 0);
    return Math.round(sum / answered);
  }, [scores, answered]);

  function pick(code: string, score: number) {
    setScores((prev) => ({ ...prev, [code]: score }));
  }

  async function handleSubmit() {
    setError('');
    if (answered < total) {
      setError('Vui lòng đánh giá tất cả các tiêu chí');
      return;
    }
    setSubmitting(true);
    try {
      await reviewApi.create({
        assignmentId: assignment.assignmentId,
        comment: comment.trim() || undefined,
        criteria: REVIEW_CRITERIA.map((c) => ({
          code: c.code,
          question: c.question,
          score: scores[c.code],
        })),
      });
      onSubmitted();
    } catch (err) {
      setError(extractError(err, 'Gửi đánh giá thất bại. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="rv-modal-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="rv-modal" onClick={(e) => e.stopPropagation()}>
        <div className="rv-modal__head">
          <h2 className="rv-modal__title">Đánh giá gia sư</h2>
          <button type="button" className="rv-modal__close" aria-label="Đóng" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="rv-modal__meta">
          <p className="rv-modal__tutor">{assignment.tutorName}</p>
          <p className="rv-modal__class">
            {assignment.classTitle}
            {assignment.subjectName ? ` · ${assignment.subjectName}` : ''}
          </p>
        </div>

        <div className="rv-modal__body">
          {REVIEW_CRITERIA.map((criterion) => (
            <fieldset key={criterion.code} className="rv-criterion">
              <legend className="rv-criterion__q">{criterion.question}</legend>
              <div className="rv-levels" role="radiogroup" aria-label={criterion.question}>
                {criterion.levels.map((level) => {
                  const selected = scores[criterion.code] === level.score;
                  return (
                    <button
                      key={level.score}
                      type="button"
                      role="radio"
                      aria-checked={selected}
                      className={`rv-level${selected ? ' rv-level--on' : ''}`}
                      onClick={() => pick(criterion.code, level.score)}
                    >
                      <span className="rv-level__score">{level.score}</span>
                      <span className="rv-level__label">{level.label}</span>
                    </button>
                  );
                })}
              </div>
            </fieldset>
          ))}
        </div>

        <div className="rv-field">
          <label className="rv-label" htmlFor="rv-comment">
            Nhận xét thêm (không bắt buộc)
          </label>
          <textarea
            id="rv-comment"
            className="rv-textarea"
            rows={3}
            maxLength={MAX_COMMENT}
            placeholder="Chia sẻ thêm trải nghiệm học tập của bạn với gia sư này..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
          <span className="rv-counter">
            {comment.length}/{MAX_COMMENT}
          </span>
        </div>

        {answered > 0 ? (
          <div className="rv-summary">
            <span className="rv-summary__score">
              Điểm tổng: <strong>{overall}</strong>/5 ★
            </span>
          </div>
        ) : null}

        {error ? <p className="rv-error">{error}</p> : null}

        <div className="rv-modal__actions">
          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose} disabled={submitting}>
            Hủy
          </button>
          <button
            type="button"
            className="tcs-btn tcs-btn--market"
            onClick={handleSubmit}
            disabled={submitting || answered < total}
          >
            {submitting ? 'Đang gửi...' : 'Gửi đánh giá'}
          </button>
        </div>
      </div>
    </div>
  );
}
