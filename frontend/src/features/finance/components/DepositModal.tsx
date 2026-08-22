import { useEffect, useState } from 'react';
import type {
  DepositPayload,
  TopupSessionInfo,
  TopupStatusInfo,
} from '../types/financeTypes';
import { useAutoPolling } from '../../../shared/hooks/useAutoPolling';

interface Props {
  onCreateTopup: (payload: DepositPayload) => Promise<TopupSessionInfo>;
  onCheckTopupStatus: (reference: string) => Promise<TopupStatusInfo>;
}

type TopupFlowStatus = 'form' | 'pending' | 'success' | 'expired' | 'failed';

const PRESETS = [100000, 200000, 500000, 1000000];
const QR_COUNTDOWN_MS = 5 * 60 * 1000;

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

function formatCountdown(ms: number) {
  const safeMs = Math.max(ms, 0);
  const minutes = Math.floor(safeMs / 60000);
  const seconds = Math.floor((safeMs % 60000) / 1000);
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function DepositModal({
  onCreateTopup,
  onCheckTopupStatus,
}: Props) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState('');
  const [amountError, setAmountError] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [session, setSession] = useState<TopupSessionInfo | null>(null);
  const [flowStatus, setFlowStatus] = useState<TopupFlowStatus>('form');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [remainingMs, setRemainingMs] = useState(0);
  const [qrExpiresAtMs, setQrExpiresAtMs] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [checking, setChecking] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !session || flowStatus !== 'pending') {
      return;
    }

    const updateRemainingTime = () => {
      const nextRemaining = Math.max(qrExpiresAtMs - Date.now(), 0);
      setRemainingMs(nextRemaining);
      if (nextRemaining <= 0) {
        setFlowStatus('expired');
        setStatusMessage('Mã QR đã hết hạn. Tạo mã mới để tiếp tục nạp tiền.');
      }
    };

    updateRemainingTime();
    const timer = window.setInterval(updateRemainingTime, 1000);
    return () => window.clearInterval(timer);
  }, [open, session, flowStatus, qrExpiresAtMs]);

  useAutoPolling(
    async () => {
      if (!open || !session || flowStatus !== 'pending') {
        return;
      }
      try {
        // Poll every few seconds so a successful bank transfer updates the modal without manual refresh.
        const data = await onCheckTopupStatus(session.reference);
        applyTopupStatus(data, false);
      } catch {
        // Auto polling im lặng, tránh làm người dùng bị nhiễu.
      }
    },
    open && !!session && flowStatus === 'pending',
    5000,
  );

  function resetFlow() {
    setAmount('');
    setAmountError(null);
    setDescription('');
    setSession(null);
    setFlowStatus('form');
    setStatusMessage(null);
    setRemainingMs(0);
    setQrExpiresAtMs(0);
    setCopied(false);
    setError(null);
  }

  function handleClose() {
    if (submitting) {
      return;
    }
    setOpen(false);
    resetFlow();
  }

  function applyTopupStatus(data: TopupStatusInfo, manualCheck: boolean) {
    // Manual checks and auto polling both flow through this status mapper.
    const normalized = data.status.toUpperCase();

    if (normalized === 'SUCCESS') {
      setFlowStatus('success');
      setStatusMessage(data.message || 'Nạp tiền thành công. Số dư ví đã được cập nhật.');
      setError(null);
      return;
    }

    if (normalized === 'EXPIRED') {
      setFlowStatus('expired');
      setStatusMessage(data.message || 'Mã QR đã hết hạn. Tạo mã mới để tiếp tục nạp tiền.');
      return;
    }

    if (normalized === 'FAILED' || normalized === 'CANCELLED') {
      setFlowStatus('failed');
      setStatusMessage(data.message || 'Giao dịch chưa hoàn tất. Vui lòng tạo mã mới.');
      return;
    }

    if (manualCheck) {
      setStatusMessage(data.message || 'Chưa nhận được giao dịch. Hệ thống sẽ tiếp tục kiểm tra.');
    }
  }

  async function createSession(parsedAmount: number) {
    // One QR session maps to one pending backend transaction and one transfer reference.
    setSubmitting(true);
    setError(null);
    setStatusMessage(null);
    setCopied(false);

    try {
      const created = await onCreateTopup({
        amount: parsedAmount,
        description: description.trim() || undefined,
      });
      setSession(created);
      setFlowStatus('pending');
      setQrExpiresAtMs(Date.now() + QR_COUNTDOWN_MS);
      setRemainingMs(QR_COUNTDOWN_MS);
      setStatusMessage('Quét mã QR hoặc chuyển khoản đúng nội dung để hệ thống tự xác nhận.');
    } catch {
      setSession(null);
      setFlowStatus('form');
      setError('Không thể tạo mã QR. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  }

  function handleAmountChange(value: string) {
    setAmount(value);
    setError(null);

    if (!value.trim()) {
      setAmountError(null);
      return;
    }

    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      setAmountError('Số tiền phải lớn hơn 0');
      return;
    }

    setAmountError(null);
  }

  async function handleSubmit() {
    const parsed = Number(amount);
    if (!parsed || parsed <= 0) {
      setAmountError('Số tiền phải lớn hơn 0');
      return;
    }
    setAmountError(null);
    await createSession(parsed);
  }

  async function handleCheckStatus(manualCheck = true) {
    if (!session || checking) {
      return;
    }
    setChecking(true);
    if (manualCheck) {
      setError(null);
    }

    try {
      const data = await onCheckTopupStatus(session.reference);
      applyTopupStatus(data, manualCheck);
    } catch {
      if (manualCheck) {
        setError('Không thể kiểm tra giao dịch lúc này. Vui lòng thử lại.');
      }
    } finally {
      setChecking(false);
    }
  }

  async function handleRefreshQr() {
    const nextAmount = session?.amount || Number(amount);
    if (!nextAmount || nextAmount <= 0) {
      setFlowStatus('form');
      setSession(null);
      return;
    }
    setAmount(String(nextAmount));
    await createSession(nextAmount);
  }

  async function handleCopyTransferContent() {
    if (!session || !navigator.clipboard) {
      return;
    }
    await navigator.clipboard.writeText(session.transferContent);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  }

  return (
    <>
      <button className="deposit-btn" onClick={() => setOpen(true)}>
        Nạp tiền
      </button>

      {open && (
        <div className="modal-overlay" onClick={handleClose}>
          <div className="modal modal--topup" onClick={(e) => e.stopPropagation()}>
            <div className="modal__header">
              <h2>Nạp tiền vào ví</h2>
              {!submitting && <button className="modal__close" onClick={handleClose}>×</button>}
            </div>

            {!session ? (
              <>
                <div className="modal__body">
                  <label className="form-label">Chọn nhanh</label>
                  <div className="deposit-presets">
                    {PRESETS.map((v) => (
                      <button
                        key={v}
                        type="button"
                        className={`deposit-preset ${amount === String(v) ? 'deposit-preset--active' : ''}`}
                        onClick={() => handleAmountChange(String(v))}
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
                    onChange={(e) => handleAmountChange(e.target.value)}
                    min={1}
                  />
                  {amountError && <p className="form-error">{amountError}</p>}

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
                  <button className="btn btn--secondary" onClick={handleClose}>
                    Hủy
                  </button>
                  <button className="btn btn--primary" onClick={handleSubmit} disabled={submitting}>
                    {submitting ? 'Đang tạo QR…' : 'Tạo mã QR'}
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className="modal__body">
                  {statusMessage && (
                    <div className={`topup-session__status topup-session__status--${flowStatus}`}>
                      <span>{statusMessage}</span>
                      {flowStatus === 'pending' && (
                        <strong>{formatCountdown(remainingMs)}</strong>
                      )}
                    </div>
                  )}

                  <div className="topup-session">
                    <div className="topup-session__qr">
                      <img src={session.qrUrl} alt={`QR nạp tiền ${session.reference}`} />
                      {flowStatus !== 'pending' && (
                        <div className="topup-session__qr-overlay">
                          {flowStatus === 'success' ? 'Đã thanh toán' : 'Cần tạo mã mới'}
                        </div>
                      )}
                    </div>

                    <div className="topup-session__details">
                      <div className="topup-session__row">
                        <span>Ngân hàng</span>
                        <strong>{session.bankName}</strong>
                      </div>
                      <div className="topup-session__row">
                        <span>Số tài khoản</span>
                        <strong>{session.accountNumber}</strong>
                      </div>
                      <div className="topup-session__row">
                        <span>Tên tài khoản</span>
                        <strong>{session.accountName}</strong>
                      </div>
                      <div className="topup-session__row">
                        <span>Số tiền</span>
                        <strong>{formatMoney(session.amount)}</strong>
                      </div>
                      <div className="topup-session__row topup-session__row--column">
                        <span>Nội dung chuyển khoản</span>
                        <div className="topup-session__code-line">
                          <code>{session.transferContent}</code>
                          <button
                            type="button"
                            className="topup-session__copy"
                            onClick={handleCopyTransferContent}
                          >
                            {copied ? 'Đã sao chép' : 'Sao chép'}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  {error && <p className="form-error">{error}</p>}
                </div>

                <div className="modal__footer modal__footer--wrap">
                  <button className="btn btn--secondary" onClick={handleClose}>
                    Đóng
                  </button>

                  {flowStatus === 'pending' && (
                    <>
                      <button
                        className="btn btn--secondary"
                        onClick={() => handleCheckStatus(true)}
                        disabled={checking}
                      >
                        {checking ? 'Đang kiểm tra…' : 'Kiểm tra'}
                      </button>
                    </>
                  )}

                  {(flowStatus === 'expired' || flowStatus === 'failed') && (
                    <button
                      className="btn btn--primary"
                      onClick={handleRefreshQr}
                      disabled={submitting}
                    >
                      {submitting ? 'Đang tạo QR…' : 'Tạo mã mới'}
                    </button>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
