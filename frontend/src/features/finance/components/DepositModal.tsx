import { useState } from 'react';
import type { DepositPayload } from '../types/financeTypes';

interface Props {
  onDeposit: (payload: DepositPayload) => Promise<boolean>;
}

export function DepositModal({ onDeposit }: Props) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const presets = [100000, 200000, 500000, 1000000];

  async function handleSubmit() {
    const parsed = Number(amount);
    if (!parsed || parsed <= 0) {
      setError('Số tiền phải lớn hơn 0');
      return;
    }
    setSubmitting(true);
    setError(null);
    const ok = await onDeposit({ amount: parsed, description: description || undefined });
    setSubmitting(false);
    if (ok) {
      setSuccess(true);
      setAmount('');
      setDescription('');
      setTimeout(() => {
        setSuccess(false);
        setOpen(false);
      }, 1500);
    } else {
      setError('Nạp tiền thất bại. Vui lòng thử lại.');
    }
  }

  return (
    <>
      <button className="deposit-btn" onClick={() => setOpen(true)}>
        Nạp tiền
      </button>

      {open && (
        <div className="modal-overlay" onClick={() => !submitting && setOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal__header">
              <h2>Nạp tiền vào ví</h2>
              {!submitting && (
                <button className="modal__close" onClick={() => setOpen(false)}>×</button>
              )}
            </div>

            {success ? (
              <div className="modal__success">
                <p>Nạp tiền thành công!</p>
              </div>
            ) : (
              <>
                <div className="modal__body">
                  <label className="form-label">Chọn nhanh</label>
                  <div className="deposit-presets">
                    {presets.map((v) => (
                      <button
                        key={v}
                        className={`deposit-preset ${amount === String(v) ? 'deposit-preset--active' : ''}`}
                        onClick={() => setAmount(String(v))}
                      >
                        {v >= 1000000
                          ? `${(v / 1000000).toFixed(0)}M`
                          : `${(v / 1000).toFixed(0)}K`}
                      </button>
                    ))}
                  </div>

                  <label className="form-label" htmlFor="deposit-amount">Số tiền (VND)</label>
                  <input
                    id="deposit-amount"
                    type="number"
                    className="form-input"
                    placeholder="Nhập số tiền"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    min={1}
                  />

                  <label className="form-label" htmlFor="deposit-desc">Ghi chú (tùy chọn)</label>
                  <input
                    id="deposit-desc"
                    type="text"
                    className="form-input"
                    placeholder="VD: Nạp tiền tháng 7"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />

                  {error && <p className="form-error">{error}</p>}
                </div>

                <div className="modal__footer">
                  <button className="btn btn--secondary" onClick={() => setOpen(false)}>
                    Hủy
                  </button>
                  <button className="btn btn--primary" onClick={handleSubmit} disabled={submitting}>
                    {submitting ? 'Đang xử lý…' : 'Xác nhận nạp'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
