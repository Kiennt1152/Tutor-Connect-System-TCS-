import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { useExecuteSettlement } from '../hooks/usePlatformMutations';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './PlatformEscrowPage.css';

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

  const [escrowId, setEscrowId] = useState('');
  const [releaseAmount, setReleaseAmount] = useState('');
  const [reason, setReason] = useState('Giải ngân escrow theo quyết định vận hành');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    setEscrowId(normalizeDigits(searchParams.get('escrowId') ?? ''));
    setReleaseAmount(readMoneyParam(searchParams.get('amount')));
    resetSettlement();
    setFormError('');
    setSuccessMessage('');
  }, [resetSettlement, searchParams]);

  const releaseAmountNumber = useMemo(() => {
    if (!releaseAmount) return 0;
    return Number(releaseAmount);
  }, [releaseAmount]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const parsedEscrowId = Number(escrowId);
    const parsedAmount = Number(releaseAmount);
    const trimmedReason = reason.trim();

    if (!Number.isInteger(parsedEscrowId) || parsedEscrowId <= 0) {
      setFormError('Vui lòng nhập mã escrow hợp lệ.');
      return;
    }
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setFormError('Vui lòng nhập số tiền giải ngân lớn hơn 0.');
      return;
    }
    if (trimmedReason.length < 10) {
      setFormError('Vui lòng nhập lý do giải ngân ít nhất 10 ký tự.');
      return;
    }

    setFormError('');
    setSuccessMessage('');
    const result = await executeSettlement({
      escrowId: parsedEscrowId,
      releaseToBeneficiary: parsedAmount,
      refundToPayer: 0,
      reason: trimmedReason,
    });

    if (result) {
      setSuccessMessage(result);
    }
  };

  return (
    <AdminLayout
      title="Giải ngân escrow"
      subtitle="Thực hiện release escrow cho lớp hoặc ghi danh đã đủ điều kiện tất toán."
    >
      <div className="pe-layout">
        <section className="adm-card pe-card">
          <div className="pe-card__head">
            <div>
              <h2 className="pe-card__title">Thông tin giải ngân</h2>
              <p className="pe-card__meta">Số tiền giải ngân sẽ được chuyển tới ví người thụ hưởng của escrow.</p>
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
                disabled={status === 'loading'}
                onChange={(event) => setEscrowId(normalizeDigits(event.target.value))}
              />
            </label>

            <label className="pe-field">
              <span>Số tiền giải ngân</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={releaseAmount}
                disabled={status === 'loading'}
                onChange={(event) => setReleaseAmount(normalizeDigits(event.target.value))}
              />
            </label>

            <label className="pe-field pe-field--full">
              <span>Lý do giải ngân</span>
              <textarea
                className="pe-textarea"
                rows={4}
                maxLength={1000}
                value={reason}
                disabled={status === 'loading'}
                onChange={(event) => setReason(event.target.value)}
              />
            </label>

            {formError && <div className="adm-alert adm-alert--error pe-alert">{formError}</div>}
            {status === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error pe-alert">{errorMessage}</div>
            )}
            {successMessage && (
              <div className="adm-alert adm-alert--success pe-alert">{successMessage}</div>
            )}

            <div className="pe-form__actions">
              <button
                className="tcs-btn tcs-btn--primary"
                type="submit"
                disabled={status === 'loading'}
              >
                {status === 'loading' ? 'Đang giải ngân...' : 'Giải ngân escrow'}
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
              <span>Giải ngân</span>
              <strong>{formatCurrency(releaseAmountNumber || null)}</strong>
            </div>
            <div className="pe-preview__row">
              <span>Hoàn lại</span>
              <strong>{formatCurrency(0)}</strong>
            </div>
          </div>
        </aside>
      </div>
    </AdminLayout>
  );
}
