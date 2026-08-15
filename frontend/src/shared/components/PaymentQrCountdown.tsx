import { useEffect, useState } from 'react';
import './PaymentQrCountdown.css';

const DEFAULT_DURATION_MS = 5 * 60 * 1000;

type PaymentQrCountdownProps = {
  resetKey?: string | number | null;
  durationMs?: number;
  label?: string;
  expiredLabel?: string;
};

function formatCountdown(ms: number) {
  const safeMs = Math.max(ms, 0);
  const minutes = Math.floor(safeMs / 60000);
  const seconds = Math.floor((safeMs % 60000) / 1000);
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function PaymentQrCountdown({
  resetKey,
  durationMs = DEFAULT_DURATION_MS,
  label = 'Mã QR còn hiệu lực',
  expiredLabel = 'Mã QR đã hết thời gian hiển thị. Vui lòng tạo hoặc tải lại mã nếu chưa chuyển khoản.',
}: PaymentQrCountdownProps) {
  const [expiresAt, setExpiresAt] = useState(() => Date.now() + durationMs);
  const [remainingMs, setRemainingMs] = useState(durationMs);

  useEffect(() => {
    const nextExpiresAt = Date.now() + durationMs;
    setExpiresAt(nextExpiresAt);
    setRemainingMs(durationMs);
  }, [durationMs, resetKey]);

  useEffect(() => {
    const update = () => {
      setRemainingMs(Math.max(expiresAt - Date.now(), 0));
    };
    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [expiresAt]);

  const expired = remainingMs <= 0;

  return (
    <div className={`payment-qr-countdown${expired ? ' payment-qr-countdown--expired' : ''}`}>
      <span>{expired ? expiredLabel : label}</span>
      <strong>{formatCountdown(remainingMs)}</strong>
    </div>
  );
}
