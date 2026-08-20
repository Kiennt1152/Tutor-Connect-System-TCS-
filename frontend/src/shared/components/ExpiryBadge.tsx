import { useEffect, useState } from 'react';
import './ExpiryBadge.css';

type ExpiryBadgeProps = {
  /** Mốc hết hạn (chuỗi ISO từ backend). */
  readonly expiresAt: string;
  /** Chú thích khi rê chuột. Mặc định là nội dung của tin đăng lớp ở marketplace. */
  readonly title?: string;
  /** Nhãn hiển thị khi đã quá hạn. */
  readonly expiredLabel?: string;
};

/**
 * Đồng hồ đếm ngược tới mốc hết hạn, cập nhật mỗi giây và đổi màu khi còn ≤ 7 ngày.
 * Dùng chung cho tin đăng lớp ở marketplace (30 ngày hiển thị) và lớp của trung tâm
 * (30 ngày mở ghi danh) để hai nơi không lệch cách tính.
 */
export function ExpiryBadge({ expiresAt, title, expiredLabel = 'Đã hết hạn' }: ExpiryBadgeProps) {
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
    label = expiredLabel;
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
      className={`tcs-expiry tcs-expiry--${tone}`}
      title={
        title ??
        `Lớp chỉ hiển thị đến ${new Date(expiresAt).toLocaleString('vi-VN')}. Hết hạn sẽ tự bị xóa nếu chưa ký hợp đồng.`
      }
    >
      ⏳ {label}
    </span>
  );
}
