import { useState, useEffect, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { AdminDisputeReviewApiResponse, DisputeResolutionAction } from '../types/platformTypes';
import './SettleDisputeModal.css';

export interface SettleDisputeModalProps {
  isOpen: boolean;
  dispute: AdminDisputeReviewApiResponse | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function SettleDisputeModal({
  isOpen,
  dispute,
  onClose,
  onSuccess,
}: SettleDisputeModalProps) {
  const [refundAmount, setRefundAmount] = useState<string>('0');
  const [releaseAmount, setReleaseAmount] = useState<string>('0');
  const [adminNotes, setAdminNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const escrowAmount = typeof dispute?.escrow?.amount === 'number'
    ? Math.trunc(dispute.escrow.amount)
    : 0;

  useEffect(() => {
    if (isOpen && dispute) {
      const suggestion = dispute.settlementSuggestion;
      if (
        suggestion &&
        typeof suggestion.refundAmount === 'number' &&
        typeof suggestion.releaseAmount === 'number' &&
        Math.trunc(suggestion.refundAmount) + Math.trunc(suggestion.releaseAmount) === escrowAmount
      ) {
        setRefundAmount(String(Math.trunc(suggestion.refundAmount)));
        setReleaseAmount(String(Math.trunc(suggestion.releaseAmount)));
      } else {
        setRefundAmount('0');
        setReleaseAmount(String(escrowAmount));
      }
      setAdminNotes(dispute.resolution || '');
      setError(null);
    }
  }, [isOpen, dispute, escrowAmount]);

  if (!isOpen || !dispute) return null;

  const numRefund = Number(refundAmount) || 0;
  const numRelease = Number(releaseAmount) || 0;
  const totalAllocated = numRefund + numRelease;
  const remaining = escrowAmount - totalAllocated;

  // Backend strictly enforces releaseAmount + refundAmount === escrowAmount
  const isValidAllocation = numRefund >= 0 && numRelease >= 0 && totalAllocated === escrowAmount;
  const isValidNotes = adminNotes.trim().length >= 20;

  const handleSetFullRefund = () => {
    setRefundAmount(String(escrowAmount));
    setReleaseAmount('0');
  };

  const handleSetFullRelease = () => {
    setRefundAmount('0');
    setReleaseAmount(String(escrowAmount));
  };

  const handleFillRemainingToRefund = () => {
    if (remaining > 0) {
      setRefundAmount(String(numRefund + remaining));
    }
  };

  const handleFillRemainingToRelease = () => {
    if (remaining > 0) {
      setReleaseAmount(String(numRelease + remaining));
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (numRefund < 0 || numRelease < 0) {
      setError('Số tiền hoàn và giải ngân không được âm.');
      return;
    }
    if (totalAllocated !== escrowAmount) {
      setError(`Tổng tiền hoàn và giải ngân (${totalAllocated.toLocaleString('vi-VN')} ₫) phải bằng chính xác số tiền Escrow (${escrowAmount.toLocaleString('vi-VN')} ₫).`);
      return;
    }
    if (adminNotes.trim().length < 20) {
      setError('Lý do giải quyết tranh chấp phải có ít nhất 20 ký tự.');
      return;
    }

    setSubmitting(true);
    setError(null);

    let action: DisputeResolutionAction = 'TERMINATE_CLASS';
    if (numRefund > 0 && numRelease === 0) {
      action = 'APPROVE_FULL_REFUND';
    } else if (numRefund > 0 && numRelease > 0) {
      action = 'APPROVE_PARTIAL_REFUND';
    } else if (numRefund === 0 && numRelease > 0) {
      action = 'TERMINATE_CLASS';
    } else {
      action = 'CLOSE_MUTUAL_AGREEMENT';
    }

    try {
      await platformApi.resolveDispute(String(dispute.disputeId), {
        action,
        status: 'RESOLVED',
        resolution: adminNotes.trim(),
        refundToPayer: numRefund,
        releaseToBeneficiary: numRelease,
      });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(getApiErrorMessage(err, 'Không thể giải quyết tranh chấp.'));
    } finally {
      setSubmitting(false);
    }
  };

  return createPortal(
    <div className="adm-settle-modal-overlay" role="dialog" aria-modal="true">
      <div className="adm-settle-modal">
        <div className="adm-settle-modal__header">
          <h2>Giải quyết tranh chấp #{dispute.disputeId}</h2>
          <button type="button" className="adm-settle-modal__close" onClick={onClose} aria-label="Đóng">×</button>
        </div>

        <div className="adm-settle-modal__escrow-banner">
          <span>Tổng số tiền Escrow cần phân bổ:</span>
          <strong>{escrowAmount.toLocaleString('vi-VN')} ₫</strong>
        </div>

        <div className="adm-settle-quick-actions">
          <button type="button" className="adm-settle-quick-btn" onClick={handleSetFullRefund}>
            Hoàn 100% cho học viên
          </button>
          <button type="button" className="adm-settle-quick-btn" onClick={handleSetFullRelease}>
            Giải ngân 100% cho gia sư
          </button>
          {remaining > 0 && (
            <>
              <button type="button" className="adm-settle-quick-btn" onClick={handleFillRemainingToRefund}>
                + Gán phần dư ({remaining.toLocaleString('vi-VN')} ₫) cho học viên
              </button>
              <button type="button" className="adm-settle-quick-btn" onClick={handleFillRemainingToRelease}>
                + Gán phần dư ({remaining.toLocaleString('vi-VN')} ₫) cho gia sư
              </button>
            </>
          )}
        </div>

        <form className="adm-settle-form" onSubmit={handleSubmit}>
          {error && <div className="adm-settle-error">{error}</div>}

          <div className="adm-settle-grid">
            <div className="form-group">
              <label>Tiền hoàn cho học viên/phụ huynh (₫)</label>
              <input
                type="number"
                min={0}
                max={escrowAmount}
                value={refundAmount}
                onChange={(e) => setRefundAmount(e.target.value)}
                placeholder="Nhập số tiền hoàn"
                required
              />
            </div>
            <div className="form-group">
              <label>Tiền giải ngân cho gia sư (₫)</label>
              <input
                type="number"
                min={0}
                max={escrowAmount}
                value={releaseAmount}
                onChange={(e) => setReleaseAmount(e.target.value)}
                placeholder="Nhập số tiền giải ngân"
                required
              />
            </div>
          </div>

          <div className="adm-settle-summary">
            <div className="adm-settle-summary__row">
              <span>Hoàn cho học viên:</span>
              <span>{numRefund.toLocaleString('vi-VN')} ₫</span>
            </div>
            <div className="adm-settle-summary__row">
              <span>Giải ngân cho gia sư:</span>
              <span>{numRelease.toLocaleString('vi-VN')} ₫</span>
            </div>
            <div className="adm-settle-summary__row adm-settle-summary__row--total">
              <span>Tổng phân bổ:</span>
              <span style={{ color: totalAllocated === escrowAmount ? '#059669' : '#dc2626' }}>
                {totalAllocated.toLocaleString('vi-VN')} / {escrowAmount.toLocaleString('vi-VN')} ₫
              </span>
            </div>
          </div>

          {totalAllocated === escrowAmount ? (
            <div className="adm-settle-success-bar">
              <span>✓</span>
              <span>Tổng phân bổ khớp chính xác với 100% Escrow ({escrowAmount.toLocaleString('vi-VN')} ₫).</span>
            </div>
          ) : totalAllocated < escrowAmount ? (
            <div className="adm-settle-error">
              <span>⚠️</span>
              <span>Chưa phân bổ hết Escrow (còn thiếu <strong>{remaining.toLocaleString('vi-VN')} ₫</strong>). Backend yêu cầu tổng giải ngân + hoàn tiền phải bằng 100% Escrow.</span>
            </div>
          ) : (
            <div className="adm-settle-error">
              <span>⚠️</span>
              <span>Tổng phân bổ vượt quá Escrow <strong>{(totalAllocated - escrowAmount).toLocaleString('vi-VN')} ₫</strong>. Vui lòng điều chỉnh lại.</span>
            </div>
          )}

          <div className="form-group">
            <label>Ghi chú quyết định & Căn cứ xử lý (tối thiểu 20 ký tự)</label>
            <textarea
              rows={4}
              value={adminNotes}
              onChange={(e) => setAdminNotes(e.target.value)}
              placeholder="Nhập lý do chi tiết về quyết định phân chia số tiền tranh chấp..."
              required
            />
            <small style={{ color: adminNotes.trim().length >= 20 ? '#059669' : '#dc2626', fontSize: '0.75rem' }}>
              {adminNotes.trim().length} / 20 ký tự tối thiểu
            </small>
          </div>

          <div className="adm-settle-actions">
            <button type="button" className="btn-cancel" onClick={onClose} disabled={submitting}>Hủy</button>
            <button
              type="submit"
              className="btn-submit"
              disabled={submitting || !isValidAllocation || !isValidNotes}
              title={!isValidAllocation ? 'Tổng số tiền phân bổ phải bằng đúng 100% số tiền Escrow' : !isValidNotes ? 'Ghi chú phải có ít nhất 20 ký tự' : undefined}
            >
              {submitting ? 'Đang xử lý...' : 'Xác nhận giải quyết'}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
