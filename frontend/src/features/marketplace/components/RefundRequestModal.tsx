import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { financeApi } from '../../finance/api/financeApi';
import type { RefundRequestInfo } from '../../finance/types/financeTypes';
import './ClassTerminationModal.css';

type RefundRequestModalProps = {
  open: boolean;
  classTitle?: string | null;
  escrowId?: number | null;
  assignmentId?: number | null;
  classStudentId?: number | null;
  amountHint?: number | null;
  onClose: () => void;
};

function normalizeDigits(value: string) {
  return value.replace(/[^\d]/g, '');
}

function formatCurrency(value: number | null | undefined) {
  if (typeof value !== 'number') return '—';
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

export function RefundRequestModal({
  open,
  classTitle,
  escrowId,
  assignmentId,
  classStudentId,
  amountHint,
  onClose,
}: RefundRequestModalProps) {
  const [amount, setAmount] = useState(amountHint ? String(Math.trunc(amountHint)) : '');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<RefundRequestInfo | null>(null);

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting) return;
    setAmount(amountHint ? String(Math.trunc(amountHint)) : '');
    setReason('');
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleSubmit = async () => {
    setError('');
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError('Số tiền hoàn phải lớn hơn 0.');
      return;
    }
    if (reason.trim().length < 10) {
      setError('Vui lòng nhập lý do tối thiểu 10 ký tự.');
      return;
    }
    if (!escrowId && !assignmentId && !classStudentId) {
      setError('Không tìm thấy escrow liên quan tới lớp này.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await financeApi.createRefundRequest({
        escrowId: escrowId ?? undefined,
        assignmentId: assignmentId ?? undefined,
        classStudentId: classStudentId ?? undefined,
        amount: parsedAmount,
        reason: reason.trim(),
      });
      setSuccess(result);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi yêu cầu hoàn tiền.'));
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
        aria-labelledby="refund-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="termination-modal__header">
          <div>
            <p className="termination-modal__eyebrow">Yêu cầu hoàn tiền</p>
            <h2 id="refund-modal-title">Gửi yêu cầu hoàn tiền escrow</h2>
            <p className="termination-modal__subtitle">{classTitle?.trim() || 'Lớp liên quan'}</p>
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
                Mã yêu cầu #{success.refundId}. Trạng thái hiện tại: {success.status}.
                Escrow liên quan đã được tạm giữ để admin xử lý.
              </p>
            </div>
          ) : (
            <>
              <label className="termination-field">
                <span>Số tiền muốn hoàn</span>
                <input
                  type="text"
                  inputMode="numeric"
                  value={amount}
                  onChange={(event) => setAmount(normalizeDigits(event.target.value))}
                  placeholder="Nhập số tiền cần hoàn"
                />
              </label>
              {amountHint ? (
                <p className="termination-modal__subtitle">
                  Học phí tham chiếu: {formatCurrency(amountHint)}. Hệ thống sẽ kiểm tra theo escrow thực tế.
                </p>
              ) : null}

              <label className="termination-field">
                <span>Lý do hoàn tiền</span>
                <textarea
                  rows={5}
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="Mô tả lý do cần hoàn tiền, các buổi liên quan và bằng chứng đã có..."
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
