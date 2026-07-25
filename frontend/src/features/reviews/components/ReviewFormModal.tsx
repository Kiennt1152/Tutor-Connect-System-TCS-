import { useState } from 'react';
import axios from 'axios';
import { reviewApi } from '../api/reviewApi';
import type { ReviewableAssignment } from '../types/reviewTypes';
import { StarRating } from './StarRating';

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
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit() {
    setError('');
    if (rating < 1) {
      setError('Vui lòng chọn số sao đánh giá');
      return;
    }
    setSubmitting(true);
    try {
      await reviewApi.create({
        assignmentId: assignment.assignmentId,
        rating,
        comment: comment.trim() || undefined,
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

        <div className="rv-field">
          <label className="rv-label">Chất lượng giảng dạy</label>
          <StarRating value={rating} onChange={setRating} />
        </div>

        <div className="rv-field">
          <label className="rv-label" htmlFor="rv-comment">
            Phản hồi (không bắt buộc)
          </label>
          <textarea
            id="rv-comment"
            className="rv-textarea"
            rows={5}
            maxLength={MAX_COMMENT}
            placeholder="Chia sẻ trải nghiệm học tập của bạn với gia sư này..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
          <span className="rv-counter">
            {comment.length}/{MAX_COMMENT}
          </span>
        </div>

        {error ? <p className="rv-error">{error}</p> : null}

        <div className="rv-modal__actions">
          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose} disabled={submitting}>
            Hủy
          </button>
          <button
            type="button"
            className="tcs-btn tcs-btn--market"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? 'Đang gửi...' : 'Gửi đánh giá'}
          </button>
        </div>
      </div>
    </div>
  );
}
