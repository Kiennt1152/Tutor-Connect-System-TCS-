import { useState } from 'react';
import axios from 'axios';
import {
  reportApi,
  REPORT_CATEGORY_OPTIONS,
  type ReportCategory,
  type ReportCategoryOption,
} from '../api/reportApi';
import './ReportReviewModal.css';

type ReportReviewModalProps = {
  reviewId: number;
  subtitle?: string;
  title?: string;
  intro?: string;
  options?: ReportCategoryOption[];
  onClose: () => void;
  onReported: () => void;
};

const DEFAULT_TITLE = 'Báo cáo đánh giá';
const DEFAULT_DESCRIPTION =
  'Báo cáo sẽ được gửi tới quản trị viên để kiểm duyệt xem đánh giá này đúng hay sai.';

const MAX_DESCRIPTION = 1000;

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export function ReportReviewModal({
  reviewId,
  subtitle,
  title = DEFAULT_TITLE,
  intro = DEFAULT_DESCRIPTION,
  options = REPORT_CATEGORY_OPTIONS,
  onClose,
  onReported,
}: ReportReviewModalProps) {
  const [category, setCategory] = useState<ReportCategory | null>(null);
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit() {
    setError('');
    if (!category) {
      setError('Vui lòng chọn lý do báo cáo.');
      return;
    }
    setSubmitting(true);
    try {
      await reportApi.reportReview(reviewId, category, description);
      onReported();
    } catch (err) {
      setError(extractError(err, 'Gửi báo cáo thất bại. Vui lòng thử lại.'));
      setSubmitting(false);
    }
  }

  return (
    <div className="rp-modal-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="rp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="rp-modal__body">
        <div className="rp-modal__head">
          <h2 className="rp-modal__title">{title}</h2>
          <button type="button" className="rp-modal__close" aria-label="Đóng" onClick={onClose}>
            ×
          </button>
        </div>

        <p className="rp-modal__desc">{intro}</p>
        {subtitle ? <p className="rp-modal__subtitle">{subtitle}</p> : null}

        <div className="rp-field">
          <span className="rp-label">Lý do báo cáo</span>
          <div className="rp-options" role="radiogroup" aria-label="Lý do báo cáo">
            {options.map((opt) => (
              <button
                key={opt.value}
                type="button"
                role="radio"
                aria-checked={category === opt.value}
                className={`rp-option${category === opt.value ? ' rp-option--on' : ''}`}
                onClick={() => setCategory(opt.value)}
              >
                <span className="rp-option__label">{opt.label}</span>
                <span className="rp-option__hint">{opt.hint}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="rp-field">
          <label className="rp-label" htmlFor="rp-desc">
            Mô tả thêm (không bắt buộc)
          </label>
          <textarea
            id="rp-desc"
            className="rp-textarea"
            rows={3}
            maxLength={MAX_DESCRIPTION}
            placeholder="Mô tả chi tiết vì sao bạn cho rằng đánh giá này sai hoặc không phù hợp…"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <span className="rp-counter">
            {description.length}/{MAX_DESCRIPTION}
          </span>
        </div>

        {error ? <p className="rp-error">{error}</p> : null}

        <div className="rp-modal__actions">
          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose} disabled={submitting}>
            Hủy
          </button>
          <button
            type="button"
            className="tcs-btn tcs-btn--market"
            onClick={handleSubmit}
            disabled={submitting || !category}
          >
            {submitting ? 'Đang gửi…' : 'Gửi báo cáo'}
          </button>
        </div>
        </div>
      </div>
    </div>
  );
}
