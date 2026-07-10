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

export function WithdrawalModal({
  wallet,
  paymentMethods,
  paymentMethodsLoading,
  onLoadPaymentMethods,
  onWithdraw,
}: Props) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState('');
  const [bankName, setBankName] = useState('');
  const [accountNo, setAccountNo] = useState('');
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const availableBalance = wallet?.availableBalance ?? wallet?.balance ?? 0;
  const useSavedMethod = paymentMethodId !== '';

  useEffect(() => {
    if (open) {
      void onLoadPaymentMethods();
    }
  }, [open, onLoadPaymentMethods]);

  function resetForm() {
    setAmount('');
    setBankName('');
    setAccountNo('');
    setPaymentMethodId('');
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
    if (!useSavedMethod && !bankName.trim()) {
      setError('Vui lòng nhập tên ngân hàng nhận tiền');
      return;
    }
    if (!useSavedMethod && !accountNo.trim()) {
      setError('Vui lòng nhập số tài khoản nhận tiền');
      return;
    }

    setSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const response = await onWithdraw({
        amount: parsedAmount,
        paymentMethodId: useSavedMethod ? Number(paymentMethodId) : undefined,
        bankName: useSavedMethod ? undefined : bankName.trim(),
        accountNo: useSavedMethod ? undefined : accountNo.trim(),
      });
      setSuccessMessage(
        `Đã tạo yêu cầu rút ${formatMoney(response.amount)}. Vui lòng chờ quản trị viên xử lý.`
      );
      setAmount('');
      setBankName('');
      setAccountNo('');
      setPaymentMethodId('');
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

              {paymentMethods.length > 0 && (
                <>
                  <label className="form-label" htmlFor="withdraw-method">Tài khoản nhận tiền</label>
                  <select
                    id="withdraw-method"
                    className="form-input"
                    value={paymentMethodId}
                    onChange={(event) => setPaymentMethodId(event.target.value)}
                    disabled={paymentMethodsLoading}
                  >
                    <option value="">Nhập tài khoản mới</option>
                    {paymentMethods.map((method) => (
                      <option key={method.paymentMethodId} value={method.paymentMethodId}>
                        {method.provider || 'Ngân hàng'} • {method.lastFour || 'Không rõ'}
                      </option>
                    ))}
                  </select>
                </>
              )}

              {!useSavedMethod && (
                <>
                  <label className="form-label" htmlFor="withdraw-bank">Ngân hàng</label>
                  <input
                    id="withdraw-bank"
                    type="text"
                    className="form-input"
                    placeholder="VD: TPBank"
                    value={bankName}
                    onChange={(event) => setBankName(event.target.value)}
                  />

                  <label className="form-label" htmlFor="withdraw-account">Số tài khoản</label>
                  <input
                    id="withdraw-account"
                    type="text"
                    className="form-input"
                    placeholder="Nhập số tài khoản nhận tiền"
                    value={accountNo}
                    onChange={(event) => setAccountNo(event.target.value)}
                  />
                </>
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
              <button className="btn btn--primary" onClick={handleSubmit} disabled={submitting}>
                {submitting ? 'Đang gửi…' : 'Tạo yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
