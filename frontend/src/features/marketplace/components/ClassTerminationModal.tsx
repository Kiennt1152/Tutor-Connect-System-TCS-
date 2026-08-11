import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { marketplaceApi } from '../api/marketplaceApi';
import type { ClassTerminationResponse } from '../types/marketplaceTypes';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  type BankOption,
} from '../../finance/components/BankPicker';
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
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<ClassTerminationResponse | null>(null);
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting) return;
    setReason('');
    setEffectiveDate('');
    setSelectedBankCode('');
    setBankPickerOpen(false);
    setAccountNo('');
    setAccountHolderName('');
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleSelectBank = (bank: BankOption) => {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
  };

  const handleSubmit = async () => {
    setError('');
    if (reason.trim().length < 10) {
      setError('Vui lòng nhập lý do tối thiểu 10 ký tự.');
      return;
    }
    const normalizedAccountNo = accountNo.trim().replace(/\s+/g, '');
    if (!selectedBank) {
      setError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
      return;
    }
    if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
      setError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
      return;
    }
    if (accountHolderName.trim().length < 2) {
      setError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await marketplaceApi.requestClassTermination(classId, {
        assignmentId: assignmentId ?? undefined,
        classStudentId: classStudentId ?? undefined,
        reason: reason.trim(),
        effectiveDate: effectiveDate || undefined,
        bankName: selectedBank.name,
        accountNo: normalizedAccountNo,
        accountHolderName: accountHolderName.trim().replace(/\s+/g, ' '),
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

              <div className="termination-field">
                <span>Ngân hàng nhận hoàn tiền</span>
                <BankSelectField
                  id="termination-bank-field"
                  selectedBank={selectedBank}
                  onOpen={() => setBankPickerOpen(true)}
                />
              </div>

              <label className="termination-field">
                <span>Số tài khoản nhận hoàn tiền</span>
                <input
                  type="text"
                  inputMode="text"
                  value={accountNo}
                  onChange={(event) => setAccountNo(event.target.value)}
                  placeholder="Nhập số tài khoản"
                />
              </label>

              <label className="termination-field">
                <span>Tên chủ tài khoản</span>
                <input
                  type="text"
                  inputMode="text"
                  value={accountHolderName}
                  onChange={(event) => setAccountHolderName(event.target.value)}
                  placeholder="Nhập tên chủ tài khoản"
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

        <BankPickerDialog
          open={bankPickerOpen}
          selectedBankCode={selectedBankCode}
          onSelect={handleSelectBank}
          onClose={() => setBankPickerOpen(false)}
        />
      </div>
    </div>
  );
}
