import { useEffect, useState } from 'react';
import type {
  PaymentMethodInfo,
  WalletInfo,
  WithdrawalInfo,
  WithdrawalPayload,
} from '../types/financeTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';

interface Props {
  wallet: WalletInfo | null;
  paymentMethods: PaymentMethodInfo[];
  paymentMethodsLoading: boolean;
  onLoadPaymentMethods: () => Promise<void>;
  onWithdraw: (payload: WithdrawalPayload) => Promise<WithdrawalInfo>;
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

const WITHDRAWAL_PRESETS = [200000, 500000, 1000000, 2000000, 5000000];

export function WithdrawalModal({
  wallet,
  paymentMethods,
  paymentMethodsLoading,
  onLoadPaymentMethods,
  onWithdraw,
}: Props) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState('');
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [hasAutoSelectedMethod, setHasAutoSelectedMethod] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const availableBalance = wallet?.availableBalance ?? wallet?.balance ?? 0;
  const useSavedMethod = paymentMethodId !== '';
  const defaultPaymentMethod = paymentMethods.find((method) => method.isDefault) ?? paymentMethods[0];

  useEffect(() => {
    if (open) {
      setHasAutoSelectedMethod(false);
      void onLoadPaymentMethods();
    }
  }, [open, onLoadPaymentMethods]);

  useEffect(() => {
    if (!open || hasAutoSelectedMethod || paymentMethodId || !defaultPaymentMethod) {
      return;
    }
    setPaymentMethodId(String(defaultPaymentMethod.paymentMethodId));
    setHasAutoSelectedMethod(true);
  }, [defaultPaymentMethod, hasAutoSelectedMethod, open, paymentMethodId]);

  function resetForm() {
    setAmount('');
    setPaymentMethodId('');
    setHasAutoSelectedMethod(false);
    setSubmitting(false);
    setSuccessMessage(null);
    setError(null);
  }

  function handleClose() {
    if (submitting) {
      return;
    }
    setOpen(false);
    resetForm();
  }

  async function handleSubmit() {
    const parsedAmount = Number(amount);
    if (!parsedAmount || parsedAmount <= 0) {
      setError('Số tiền rút phải lớn hơn 0');
      return;
    }
    if (parsedAmount > availableBalance) {
      setError('Số dư khả dụng không đủ để rút tiền');
      return;
    }
    if (!useSavedMethod) {
      setError('Vui lòng thêm và chọn tài khoản nhận tiền trước khi rút tiền');
      return;
    }

    setSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const response = await onWithdraw({
        amount: parsedAmount,
        paymentMethodId: Number(paymentMethodId),
      });
      setSuccessMessage(
        `Đã tạo yêu cầu rút ${formatMoney(response.amount)}. Vui lòng chờ quản trị viên xử lý.`
      );
      setAmount('');
      setPaymentMethodId('');
      setHasAutoSelectedMethod(false);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Không thể tạo yêu cầu rút tiền. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button className="withdraw-btn" onClick={() => setOpen(true)}>
        Rút tiền
      </button>

      {open && (
        <div className="modal-overlay" onClick={handleClose}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal__header">
              <h2>Rút tiền</h2>
              {!submitting && (
                <button className="modal__close" onClick={handleClose}>×</button>
              )}
            </div>

            <div className="modal__body">
              <div className="withdrawal-summary">
                <span>Số dư khả dụng</span>
                <strong>{formatMoney(availableBalance)}</strong>
              </div>

              <label className="form-label" htmlFor="withdraw-amount">Số tiền rút (VND)</label>
              <input
                id="withdraw-amount"
                type="number"
                className="form-input"
                placeholder="Nhập số tiền muốn rút"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                min={1}
              />
              <div className="withdrawal-presets" aria-label="Gợi ý số tiền rút">
                {WITHDRAWAL_PRESETS.map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    className={`withdrawal-preset${Number(amount) === preset ? ' withdrawal-preset--active' : ''}`}
                    onClick={() => setAmount(String(preset))}
                  >
                    {formatMoney(preset)}
                  </button>
                ))}
              </div>

              {paymentMethods.length > 0 ? (
                <>
                  <label className="form-label" htmlFor="withdraw-method">Tài khoản nhận tiền</label>
                  <select
                    id="withdraw-method"
                    className="form-input"
                    value={paymentMethodId}
                    onChange={(event) => setPaymentMethodId(event.target.value)}
                    disabled={paymentMethodsLoading}
                  >
                    {paymentMethods.map((method) => (
                      <option key={method.paymentMethodId} value={method.paymentMethodId}>
                        {method.bankName || method.provider || 'Ngân hàng'} • {method.accountNoMasked || `****${method.lastFour || '----'}`}
                        {method.accountHolderName ? ` • ${method.accountHolderName}` : ''}
                        {method.isDefault ? ' • Mặc định' : ''}
                      </option>
                    ))}
                  </select>
                </>
              ) : (
                <div className="withdrawal-empty-method">
                  Vui lòng thêm tài khoản nhận tiền ở mục Tài khoản nhận tiền trước khi rút.
                </div>
              )}

              {successMessage && (
                <div className="withdrawal-success">
                  {successMessage}
                </div>
              )}
              {error && <p className="form-error">{error}</p>}
            </div>

            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={handleClose}>
                Đóng
              </button>
              <button
                className="btn btn--primary"
                onClick={handleSubmit}
                disabled={submitting || paymentMethods.length === 0}
              >
                {submitting ? 'Đang gửi…' : 'Tạo yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
