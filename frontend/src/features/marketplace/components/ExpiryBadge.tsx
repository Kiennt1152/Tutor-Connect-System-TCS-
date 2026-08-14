import { useEffect, useState } from 'react';

/**
 * Đồng hồ đếm ngược 30 ngày hiển thị lớp (từ lúc đăng). Hết hạn mà chưa ký hợp
 * đồng thì lớp tự bị xóa. Dùng chung ở màn "Yêu cầu của tôi" và "Tìm lớp" (gia sư).
 * CSS class `.mkt-expiry*` nằm trong MarketplacePage.css (đã nạp ở cả hai màn).
 */
export function ExpiryBadge({ expiresAt }: { readonly expiresAt: string }) {
  const end = new Date(expiresAt).getTime();
  const [msLeft, setMsLeft] = useState(() => end - Date.now());

  useEffect(() => {
    const tick = () => setMsLeft(end - Date.now());
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [end]);

  const pad = (n: number) => String(n).padStart(2, '0');
  let label: string;
  let tone: 'ok' | 'warn' | 'expired';
  if (msLeft <= 0) {
    label = 'Đã hết hạn';
    tone = 'expired';
  } else {
    const totalSec = Math.floor(msLeft / 1000);
    const days = Math.floor(totalSec / 86400);
    const hours = Math.floor((totalSec % 86400) / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    const secs = totalSec % 60;
    label = `Còn ${days}d ${pad(hours)}:${pad(mins)}:${pad(secs)}`;
    const daysLeft = Math.ceil(msLeft / 86400000);
    tone = daysLeft <= 7 ? 'warn' : 'ok';
  }

  return (
    <span
      className={`mkt-expiry mkt-expiry--${tone}`}
      title={`Lớp chỉ hiển thị đến ${new Date(expiresAt).toLocaleString('vi-VN')}. Hết hạn sẽ tự bị xóa nếu chưa ký hợp đồng.`}
    >
      ⏳ {label}
    </span>
  );
}
