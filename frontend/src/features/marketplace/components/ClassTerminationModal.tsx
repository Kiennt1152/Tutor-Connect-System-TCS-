import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { marketplaceApi } from '../api/marketplaceApi';
import type { ClassTerminationResponse } from '../types/marketplaceTypes';
import './ClassTerminationModal.css';

type ClassTerminationModalProps = {
  open: boolean;
  classId: number;
  assignmentId?: number | null;
  classStudentId?: number | null;
  classTitle?: string | null;
  onClose: () => void;
};

function todayInputValue() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function ClassTerminationModal({
  open,
  classId,
  assignmentId,
  classStudentId,
  classTitle,
  onClose,
}: ClassTerminationModalProps) {
  const [reason, setReason] = useState('');
  const [effectiveDate, setEffectiveDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<ClassTerminationResponse | null>(null);

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting) return;
    setReason('');
    setEffectiveDate('');
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleSubmit = async () => {
    setError('');
    if (reason.trim().length < 10) {
      setError('Vui lòng nhập lý do tối thiểu 10 ký tự.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await marketplaceApi.requestClassTermination(classId, {
        assignmentId: assignmentId ?? undefined,
        classStudentId: classStudentId ?? undefined,
        reason: reason.trim(),
        effectiveDate: effectiveDate || undefined,
      });
      setSuccess(result);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi yêu cầu chấm dứt lớp.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="termination-modal-overlay" role="presentation" onClick={resetAndClose}>
      <div
        className="termination-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="termination-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="termination-modal__header">
          <div>
            <p className="termination-modal__eyebrow">Yêu cầu chấm dứt sớm</p>
            <h2 id="termination-modal-title">Dừng lớp trước thời hạn</h2>
            <p className="termination-modal__subtitle">
              {classTitle?.trim() || `Lớp #${classId}`}
            </p>
          </div>
          <button
            className="termination-modal__close"
            type="button"
            onClick={resetAndClose}
            aria-label="Đóng"
          >
            ×
          </button>
        </div>

        <div className="termination-modal__body">
          {success ? (
            <div className="termination-success">
              <p className="termination-success__title">Đã gửi yêu cầu</p>
              <p>
                Mã yêu cầu #{success.terminationId}. Trạng thái hiện tại: {success.status}.
                Lớp sẽ được admin xem xét trước khi xử lý tiếp.
              </p>
            </div>
          ) : (
            <>
              <label className="termination-field">
                <span>Lý do chấm dứt</span>
                <textarea
                  rows={5}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="Mô tả lý do cần dừng lớp sớm, tình trạng hiện tại và mong muốn xử lý..."
                />
              </label>

              <label className="termination-field">
                <span>Ngày hiệu lực mong muốn</span>
                <input
                  type="date"
                  min={todayInputValue()}
                  value={effectiveDate}
                  onChange={(event) => setEffectiveDate(event.target.value)}
                />
              </label>
            </>
          )}

          {error && <p className="termination-error">{error}</p>}
        </div>

        <div className="termination-modal__footer">
          <button
            className="termination-btn termination-btn--secondary"
            type="button"
            onClick={resetAndClose}
            disabled={submitting}
          >
            {success ? 'Đóng' : 'Hủy'}
          </button>
          {!success && (
            <button
              className="termination-btn termination-btn--primary"
              type="button"
              onClick={handleSubmit}
              disabled={submitting}
            >
              {submitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
