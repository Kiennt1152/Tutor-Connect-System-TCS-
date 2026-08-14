import { useState, useEffect, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { RefundRequestItem } from '../types/platformTypes';
import './RefundDecisionModal.css';

export interface RefundDecisionModalProps {
  isOpen: boolean;
  refund: RefundRequestItem | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function RefundDecisionModal({
  isOpen,
  refund,
  onClose,
  onSuccess,
}: RefundDecisionModalProps) {
  const [activeTab, setActiveTab] = useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [approvedAmount, setApprovedAmount] = useState<string>('0');
  const [adminNotes, setAdminNotes] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const requestedAmount = typeof refund?.raw?.amount === 'number'
    ? Math.trunc(refund.raw.amount)
    : 0;

  useEffect(() => {
    if (isOpen && refund) {
      setActiveTab('APPROVE');
      setApprovedAmount(String(requestedAmount));
      setAdminNotes('');
      setRejectionReason('');
      setError(null);
    }
  }, [isOpen, refund, requestedAmount]);

  if (!isOpen || !refund) return null;

  const numApproved = Number(approvedAmount) || 0;
  const isApproveValid = numApproved > 0 && numApproved <= requestedAmount && (adminNotes.trim().length === 0 || adminNotes.trim().length >= 10);
  const isRejectValid = rejectionReason.trim().length >= 20;

  const handleSubmitApprove = async (e: FormEvent) => {
    e.preventDefault();
    if (numApproved <= 0) {
      setError('Số tiền duyệt hoàn phải lớn hơn 0.');
      return;
    }
    if (numApproved > requestedAmount) {
      setError(`Số tiền duyệt hoàn (${numApproved.toLocaleString('vi-VN')} ₫) không được vượt quá số tiền yêu cầu (${requestedAmount.toLocaleString('vi-VN')} ₫).`);
      return;
    }
    if (adminNotes.trim().length > 0 && adminNotes.trim().length < 10) {
      setError('Ghi chú xử lý phải có ít nhất 10 ký tự nếu nhập.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await platformApi.approveRefundRequest(refund.id, {
        approvedAmount: numApproved,
        reason: adminNotes.trim() || undefined,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(getApiErrorMessage(err, 'Không thể duyệt yêu cầu hoàn tiền.'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitReject = async (e: FormEvent) => {
    e.preventDefault();
    if (rejectionReason.trim().length < 20) {
      setError('Lý do từ chối yêu cầu hoàn tiền phải có ít nhất 20 ký tự.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await platformApi.rejectRefundRequest(refund.id, {
        reason: rejectionReason.trim(),
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(getApiErrorMessage(err, 'Không thể từ chối yêu cầu hoàn tiền.'));
    } finally {
      setSubmitting(false);
    }
  };

  return createPortal(
    <div className="adm-refund-modal-overlay" role="dialog" aria-modal="true">
      <div className="adm-refund-modal">
        <div className="adm-refund-modal__header">
          <h2>Xử lý yêu cầu hoàn tiền #{refund.id}</h2>
          <button type="button" className="adm-refund-modal__close" onClick={onClose} aria-label="Đóng">×</button>
        </div>

        <div className="adm-refund-modal__tabs">
          <button
            type="button"
            className={`adm-refund-tab ${activeTab === 'APPROVE' ? 'adm-refund-tab--active' : ''}`}
            onClick={() => { setActiveTab('APPROVE'); setError(null); }}
          >
            ✓ Duyệt hoàn tiền
          </button>
          <button
            type="button"
            className={`adm-refund-tab adm-refund-tab--danger ${activeTab === 'REJECT' ? 'adm-refund-tab--active' : ''}`}
            onClick={() => { setActiveTab('REJECT'); setError(null); }}
          >
            ✕ Từ chối hoàn tiền
          </button>
        </div>

        <div className={`adm-refund-modal__banner ${activeTab === 'REJECT' ? 'adm-refund-modal__banner--reject' : ''}`}>
          <span>Số tiền yêu cầu hoàn:</span>
          <strong>{requestedAmount.toLocaleString('vi-VN')} ₫</strong>
        </div>

        {activeTab === 'APPROVE' ? (
          <form className="adm-refund-form" onSubmit={handleSubmitApprove}>
            {error && <div className="adm-refund-error">{error}</div>}

            <div className="form-group">
              <label>Số tiền duyệt hoàn (VND)</label>
              <input
                type="number"
                min={1}
                max={requestedAmount}
                value={approvedAmount}
                onChange={(e) => setApprovedAmount(e.target.value)}
                placeholder="Nhập số tiền duyệt hoàn"
                required
              />
              <small style={{ color: '#64748b', fontSize: '0.75rem' }}>
                Tối đa: {requestedAmount.toLocaleString('vi-VN')} ₫
              </small>
            </div>

            <div className="form-group">
              <label>Ghi chú quyết định (tùy chọn, tối thiểu 10 ký tự nếu nhập)</label>
              <textarea
                rows={3}
                value={adminNotes}
                onChange={(e) => setAdminNotes(e.target.value)}
                placeholder="Ghi chú thêm về quyết định duyệt hoàn tiền..."
              />
              {adminNotes.trim().length > 0 && (
                <small style={{ color: adminNotes.trim().length >= 10 ? '#059669' : '#dc2626', fontSize: '0.75rem' }}>
                  {adminNotes.trim().length} / 10 ký tự tối thiểu
                </small>
              )}
            </div>

            <div className="adm-refund-actions">
              <button type="button" className="btn-cancel" onClick={onClose} disabled={submitting}>Hủy</button>
              <button
                type="submit"
                className="btn-submit-approve"
                disabled={submitting || !isApproveValid}
              >
                {submitting ? 'Đang xử lý...' : 'Xác nhận duyệt hoàn'}
              </button>
            </div>
          </form>
        ) : (
          <form className="adm-refund-form" onSubmit={handleSubmitReject}>
            {error && <div className="adm-refund-error">{error}</div>}

            <div className="form-group">
              <label>Lý do từ chối yêu cầu hoàn tiền (bắt buộc, tối thiểu 20 ký tự)</label>
              <textarea
                rows={4}
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Nhập lý do chi tiết từ chối yêu cầu hoàn tiền..."
                required
              />
              <small style={{ color: rejectionReason.trim().length >= 20 ? '#059669' : '#dc2626', fontSize: '0.75rem' }}>
                {rejectionReason.trim().length} / 20 ký tự tối thiểu
              </small>
            </div>

            <div className="adm-refund-actions">
              <button type="button" className="btn-cancel" onClick={onClose} disabled={submitting}>Hủy</button>
              <button
                type="submit"
                className="btn-submit-reject"
                disabled={submitting || !isRejectValid}
              >
                {submitting ? 'Đang xử lý...' : 'Xác nhận từ chối'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>,
    document.body
  );
}
