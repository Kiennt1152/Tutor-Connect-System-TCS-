import { useEffect, useState } from 'react';
import type {
  PaymentMethodInfo,
  WalletInfo,
  WithdrawalInfo,
  WithdrawalPayload,
} from '../types/financeTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import {
  BANK_OPTIONS,
  type BankOption,
  BankPickerDialog,
  BankSelectField,
} from './BankPicker';

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
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const availableBalance = wallet?.availableBalance ?? wallet?.balance ?? 0;
  const useSavedMethod = paymentMethodId !== '';
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);

  useEffect(() => {
    if (open) {
      void onLoadPaymentMethods();
    }
  }, [open, onLoadPaymentMethods]);

  function resetForm() {
    setAmount('');
    setSelectedBankCode('');
    setBankPickerOpen(false);
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
    if (!useSavedMethod && !selectedBank) {
      setError('Vui lòng chọn ngân hàng nhận tiền');
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
        bankName: useSavedMethod ? undefined : selectedBank?.name,
        accountNo: useSavedMethod ? undefined : accountNo.trim(),
      });
      setSuccessMessage(
        `Đã tạo yêu cầu rút ${formatMoney(response.amount)}. Vui lòng chờ quản trị viên xử lý.`
      );
      setAmount('');
      setSelectedBankCode('');
      setBankPickerOpen(false);
      setAccountNo('');
      setPaymentMethodId('');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Không thể tạo yêu cầu rút tiền. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  }

  function handleSelectBank(bank: BankOption) {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
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
                  <label className="form-label" htmlFor="withdraw-bank-field">Ngân hàng</label>
                  <BankSelectField
                    id="withdraw-bank-field"
                    selectedBank={selectedBank}
                    onOpen={() => setBankPickerOpen(true)}
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

                  <BankPickerDialog
                    open={bankPickerOpen}
                    selectedBankCode={selectedBankCode}
                    onSelect={handleSelectBank}
                    onClose={() => setBankPickerOpen(false)}
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
