import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { useExecuteRefund, useExecuteSettlement } from '../hooks/usePlatformMutations';
import { APP_ROUTES } from '../../../shared/constants/routes';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  type BankOption,
} from '../../finance/components/BankPicker';
import './PlatformEscrowPage.css';
import { AdminEscrowQueue } from '../components/AdminEscrowQueue';

const formatCurrency = (value: number | null | undefined) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
};

const normalizeDigits = (value: string) => value.replace(/[^\d]/g, '');

const readMoneyParam = (value: string | null) => {
  if (!value) return '';
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? String(Math.trunc(parsed)) : normalizeDigits(value);
};

export default function PlatformEscrowPage() {
  const [searchParams] = useSearchParams();
  const {
    status,
    errorMessage,
    executeSettlement,
    reset: resetSettlement,
  } = useExecuteSettlement();
  const {
    status: refundStatus,
    errorMessage: refundErrorMessage,
    executeRefund,
    reset: resetRefund,
  } = useExecuteRefund();

  const [escrowId, setEscrowId] = useState('');
  const [escrowAmount, setEscrowAmount] = useState('');
  const [releaseAmount, setReleaseAmount] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [reason, setReason] = useState('Tất toán escrow theo quyết định xử lý tranh chấp');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);

  useEffect(() => {
    const amount = readMoneyParam(searchParams.get('amount'));
    setEscrowId(normalizeDigits(searchParams.get('escrowId') ?? ''));
    setEscrowAmount(amount);
    setReleaseAmount(amount);
    setRefundAmount('');
    setSelectedBankCode('');
    setBankPickerOpen(false);
    setAccountNo('');
    setAccountHolderName('');
    resetSettlement();
    resetRefund();
    setFormError('');
    setSuccessMessage('');
  }, [resetRefund, resetSettlement, searchParams]);

  const isSubmitting = status === 'loading' || refundStatus === 'loading';
  const escrowAmountNumber = useMemo(() => {
    if (!escrowAmount) return 0;
    return Number(escrowAmount);
  }, [escrowAmount]);
  const releaseAmountNumber = useMemo(() => {
    if (!releaseAmount) return 0;
    return Number(releaseAmount);
  }, [releaseAmount]);
  const refundAmountNumber = useMemo(() => {
    if (!refundAmount) return 0;
    return Number(refundAmount);
  }, [refundAmount]);
  const totalSettlement = releaseAmountNumber + refundAmountNumber;
  const remainingAmount = escrowAmountNumber > 0 ? escrowAmountNumber - totalSettlement : null;

  const setSplit = (release: number, refund: number) => {
    setReleaseAmount(release > 0 ? String(Math.trunc(release)) : '');
    setRefundAmount(refund > 0 ? String(Math.trunc(refund)) : '');
  };

  const handleSelectBank = (bank: BankOption) => {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
  };

  const applyQuickAction = (mode: 'release-all' | 'refund-all' | 'half' | 'refund-30') => {
    if (escrowAmountNumber <= 0) {
      setFormError('Nhập tổng tiền escrow trước khi dùng gợi ý chia tiền.');
      return;
    }
    setFormError('');
    if (mode === 'release-all') {
      setSplit(escrowAmountNumber, 0);
      return;
    }
    if (mode === 'refund-all') {
      setSplit(0, escrowAmountNumber);
      return;
    }
    if (mode === 'half') {
      const refund = Math.trunc(escrowAmountNumber / 2);
      setSplit(escrowAmountNumber - refund, refund);
      return;
    }
    const refund = Math.trunc(escrowAmountNumber * 0.3);
    setSplit(escrowAmountNumber - refund, refund);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const parsedEscrowId = Number(escrowId);
    const parsedReleaseAmount = releaseAmount ? Number(releaseAmount) : 0;
    const parsedRefundAmount = refundAmount ? Number(refundAmount) : 0;
    const trimmedReason = reason.trim();

    if (!Number.isInteger(parsedEscrowId) || parsedEscrowId <= 0) {
      setFormError('Vui lòng nhập mã escrow hợp lệ.');
      return;
    }
    if (!Number.isFinite(parsedReleaseAmount) || !Number.isFinite(parsedRefundAmount)) {
      setFormError('Vui lòng nhập số tiền hợp lệ.');
      return;
    }
    if (parsedReleaseAmount < 0 || parsedRefundAmount < 0) {
      setFormError('Số tiền giải ngân và hoàn tiền không được âm.');
      return;
    }
    if (parsedReleaseAmount + parsedRefundAmount <= 0) {
      setFormError('Cần có số tiền giải ngân hoặc hoàn tiền.');
      return;
    }
    if (escrowAmountNumber > 0 && parsedReleaseAmount + parsedRefundAmount !== escrowAmountNumber) {
      setFormError('Tổng tiền giải ngân và hoàn tiền phải bằng tổng tiền escrow.');
      return;
    }
    if (parsedRefundAmount > 0) {
      const normalizedAccountNo = accountNo.trim().replace(/\s+/g, '');
      if (!selectedBank) {
        setFormError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
        return;
      }
      if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
        setFormError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
        return;
      }
      if (accountHolderName.trim().length < 2) {
        setFormError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
        return;
      }
    }
    if (trimmedReason.length < 10) {
      setFormError('Vui lòng nhập lý do tất toán ít nhất 10 ký tự.');
      return;
    }

    setFormError('');
    setSuccessMessage('');
    const payload = {
      escrowId: parsedEscrowId,
      releaseToBeneficiary: parsedReleaseAmount,
      refundToPayer: parsedRefundAmount,
      reason: trimmedReason,
      refundPayoutInfo: parsedRefundAmount > 0 && selectedBank
        ? {
            bankName: selectedBank.name,
            accountNo: accountNo.trim().replace(/\s+/g, ''),
            accountHolderName: accountHolderName.trim().replace(/\s+/g, ' '),
          }
        : undefined,
    };

    const result = parsedRefundAmount > 0
      ? await executeRefund(payload)
      : await executeSettlement(payload);

    if (result) {
      setSuccessMessage(typeof result === 'string'
        ? result
        : `${result.message}: hoàn ${formatCurrency(result.refundToPayer)}, giải ngân ${formatCurrency(result.releaseToBeneficiary)}.`);
    }
  };

  return (
    <AdminLayout
      title="Quản lý escrow"
      subtitle="Theo dõi toàn bộ escrow trong hệ thống và thực hiện giải ngân hoặc hoàn tiền khi cần."
    >
      <AdminEscrowQueue onSelect={(item) => {
        setEscrowId(String(item.escrowId)); setEscrowAmount(String(item.amount));
        setReleaseAmount(String(item.amount)); setRefundAmount(''); setSuccessMessage(''); setFormError('');
      }} />
      <div className="pe-layout">
        <section className="adm-card pe-card">
          <div className="pe-card__head">
            <div>
              <h2 className="pe-card__title">Thông tin tất toán</h2>
              <p className="pe-card__meta">Tổng tiền giải ngân và hoàn lại phải bằng số tiền escrow.</p>
            </div>
            <Link className="tcs-btn tcs-btn--ghost" to={APP_ROUTES.platformReports}>
              Xem tranh chấp
            </Link>
          </div>

          <form className="pe-form" onSubmit={handleSubmit}>
            <label className="pe-field">
              <span>Mã escrow</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={escrowId}
                disabled={isSubmitting}
                onChange={(event) => setEscrowId(normalizeDigits(event.target.value))}
              />
            </label>

            <label className="pe-field">
              <span>Tổng tiền escrow</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={escrowAmount}
                disabled={isSubmitting}
                onChange={(event) => setEscrowAmount(normalizeDigits(event.target.value))}
              />
            </label>

            <div className="pe-quick-actions">
              <button className="tcs-btn tcs-btn--soft tcs-btn--sm" type="button" onClick={() => applyQuickAction('release-all')}>
                Giải ngân 100%
              </button>
              <button className="tcs-btn tcs-btn--soft tcs-btn--sm" type="button" onClick={() => applyQuickAction('refund-all')}>
                Hoàn 100%
              </button>
              <button className="tcs-btn tcs-btn--soft tcs-btn--sm" type="button" onClick={() => applyQuickAction('half')}>
                Chia 50/50
              </button>
              <button className="tcs-btn tcs-btn--soft tcs-btn--sm" type="button" onClick={() => applyQuickAction('refund-30')}>
                Hoàn 30%
              </button>
            </div>

            <label className="pe-field">
              <span>Giải ngân cho bên nhận</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={releaseAmount}
                disabled={isSubmitting}
                onChange={(event) => setReleaseAmount(normalizeDigits(event.target.value))}
              />
            </label>

            <label className="pe-field">
              <span>Hoàn lại người thanh toán</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={refundAmount}
                disabled={isSubmitting}
                onChange={(event) => setRefundAmount(normalizeDigits(event.target.value))}
              />
            </label>

            {refundAmountNumber > 0 ? (
              <>
                <div className="pe-field pe-field--full">
                  <span>Ngân hàng nhận hoàn tiền</span>
                  <BankSelectField
                    id="escrow-refund-bank-field"
                    selectedBank={selectedBank}
                    onOpen={() => setBankPickerOpen(true)}
                  />
                </div>
                <label className="pe-field">
                  <span>Số tài khoản nhận hoàn tiền</span>
                  <input
                    className="adm-field"
                    inputMode="text"
                    value={accountNo}
                    disabled={isSubmitting}
                    onChange={(event) => setAccountNo(event.target.value)}
                  />
                </label>
                <label className="pe-field">
                  <span>Tên chủ tài khoản</span>
                  <input
                    className="adm-field"
                    inputMode="text"
                    value={accountHolderName}
                    disabled={isSubmitting}
                    onChange={(event) => setAccountHolderName(event.target.value)}
                  />
                </label>
              </>
            ) : null}

            <label className="pe-field pe-field--full">
              <span>Lý do tất toán</span>
              <textarea
                className="pe-textarea"
                rows={4}
                maxLength={1000}
                value={reason}
                disabled={isSubmitting}
                onChange={(event) => setReason(event.target.value)}
              />
            </label>

            {formError && <div className="adm-alert adm-alert--error pe-alert">{formError}</div>}
            {status === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error pe-alert">{errorMessage}</div>
            )}
            {refundStatus === 'error' && refundErrorMessage && (
              <div className="adm-alert adm-alert--error pe-alert">{refundErrorMessage}</div>
            )}
            {successMessage && (
              <div className="adm-alert adm-alert--success pe-alert">{successMessage}</div>
            )}

            <div className="pe-form__actions">
              <button
                className="tcs-btn tcs-btn--primary"
                type="submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Đang tất toán...' : 'Thực thi tất toán'}
              </button>
            </div>
          </form>
        </section>

        <aside className="adm-card pe-preview">
          <h2 className="pe-card__title">Dữ liệu gửi đi</h2>
          <div className="pe-preview__rows">
            <div className="pe-preview__row">
              <span>Escrow</span>
              <strong>{escrowId ? `#${escrowId}` : '—'}</strong>
            </div>
            <div className="pe-preview__row">
              <span>Tổng escrow</span>
              <strong>{formatCurrency(escrowAmountNumber || null)}</strong>
            </div>
            <div className="pe-preview__row">
              <span>Giải ngân</span>
              <strong>{formatCurrency(releaseAmountNumber || null)}</strong>
            </div>
            <div className="pe-preview__row">
              <span>Hoàn lại</span>
              <strong>{formatCurrency(refundAmountNumber || null)}</strong>
            </div>
            <div className={`pe-preview__row${remainingAmount === 0 ? ' pe-preview__row--ok' : ''}`}>
              <span>Còn lệch</span>
              <strong>{remainingAmount == null ? '—' : formatCurrency(remainingAmount)}</strong>
            </div>
          </div>
        </aside>
      </div>

      <BankPickerDialog
        open={bankPickerOpen}
        selectedBankCode={selectedBankCode}
        onSelect={handleSelectBank}
        onClose={() => setBankPickerOpen(false)}
      />
    </AdminLayout>
  );
}
