import { useMemo, useState } from 'react';
import axios from 'axios';
import { reviewApi } from '../api/reviewApi';
import { REVIEW_CRITERIA } from '../config/reviewCriteria';
import type { ReviewableAssignment } from '../types/reviewTypes';

type ReviewFormModalProps = {
  assignment: ReviewableAssignment;
  edit?: boolean;
  onClose: () => void;
  onSubmitted: () => void;
};

const MAX_COMMENT = 1000;
const MAX_DISPLAY_NAME = 100;

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function parseScores(criteriaJson: string | null): Record<string, number> {
  if (!criteriaJson) return {};
  try {
    const parsed = JSON.parse(criteriaJson) as { code: string; score: number }[];
    return parsed.reduce<Record<string, number>>((acc, c) => {
      if (c && typeof c.code === 'string' && typeof c.score === 'number') {
        acc[c.code] = c.score;
      }
      return acc;
    }, {});
  } catch {
    return {};
  }
}

export function ReviewFormModal({ assignment, edit, onClose, onSubmitted }: ReviewFormModalProps) {
  const isEdit = Boolean(edit) && assignment.reviewed && assignment.reviewId != null;
  const [scores, setScores] = useState<Record<string, number>>(() =>
    isEdit ? parseScores(assignment.criteriaJson) : {},
  );
  const [comment, setComment] = useState(isEdit ? (assignment.comment ?? '') : '');
  const [anonymous, setAnonymous] = useState(isEdit ? assignment.anonymous : false);
  const [displayName, setDisplayName] = useState(
    isEdit && assignment.anonymous ? (assignment.reviewerDisplayName ?? '') : '',
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const answered = Object.keys(scores).length;
  const total = REVIEW_CRITERIA.length;
  const overall = useMemo(() => {
    if (answered === 0) return 0;
    const sum = Object.values(scores).reduce((a, b) => a + b, 0);
    return Math.round((sum / answered) * 10) / 10;
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
      const payload = {
        comment: comment.trim() || undefined,
        anonymous,
        displayName: anonymous ? displayName.trim() || undefined : undefined,
        criteria: REVIEW_CRITERIA.map((c) => ({
          code: c.code,
          question: c.question,
          score: scores[c.code],
        })),
      };
      if (isEdit) {
        await reviewApi.update(assignment.reviewId as number, payload);
      } else {
        await reviewApi.create({ assignmentId: assignment.assignmentId, ...payload });
      }
      onSubmitted();
    } catch (err) {
      setError(
        extractError(
          err,
          isEdit
            ? 'Cập nhật đánh giá thất bại. Vui lòng thử lại.'
            : 'Gửi đánh giá thất bại. Vui lòng thử lại.',
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="rv-modal-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="rv-modal" onClick={(e) => e.stopPropagation()}>
        <div className="rv-modal__scroll">
        <div className="rv-modal__head">
          <h2 className="rv-modal__title">{isEdit ? 'Chỉnh sửa đánh giá' : 'Đánh giá gia sư'}</h2>
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
          <p className="rv-modal__periodic">
            {isEdit
              ? 'Đang chỉnh sửa đánh giá gần nhất của bạn'
              : `Lượt đánh giá thứ ${assignment.reviewsSubmitted + 1}`}
          </p>
        </div>

        <div className="rv-field">
          <span className="rv-label">Cách hiển thị đánh giá</span>
          <div className="rv-visibility" role="radiogroup" aria-label="Cách hiển thị đánh giá">
            <button
              type="button"
              role="radio"
              aria-checked={!anonymous}
              className={`rv-visibility__opt${!anonymous ? ' rv-visibility__opt--on' : ''}`}
              onClick={() => setAnonymous(false)}
            >
              <span className="rv-visibility__title">Công khai tên của tôi</span>
              <span className="rv-visibility__desc">Hiển thị tên thật của bạn cùng đánh giá</span>
            </button>
            <button
              type="button"
              role="radio"
              aria-checked={anonymous}
              className={`rv-visibility__opt${anonymous ? ' rv-visibility__opt--on' : ''}`}
              onClick={() => setAnonymous(true)}
            >
              <span className="rv-visibility__title">Ẩn danh</span>
              <span className="rv-visibility__desc">Không hiển thị tên thật của bạn</span>
            </button>
          </div>
          {anonymous ? (
            <div className="rv-field rv-field--nested">
              <label className="rv-label" htmlFor="rv-display-name">
                Tên hiển thị ẩn danh (không bắt buộc)
              </label>
              <input
                id="rv-display-name"
                className="rv-input"
                type="text"
                maxLength={MAX_DISPLAY_NAME}
                placeholder="Người dùng ẩn danh"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
              />
              <span className="rv-hint">
                Để trống sẽ hiển thị là “Người dùng ẩn danh”.
              </span>
            </div>
          ) : null}
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
              Điểm tổng: <strong>{overall.toFixed(1)}</strong>/5 ★
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
            {submitting
              ? 'Đang lưu...'
              : isEdit
                ? 'Lưu thay đổi'
                : 'Gửi đánh giá'}
          </button>
        </div>
        </div>
      </div>
    </div>
  );
}
