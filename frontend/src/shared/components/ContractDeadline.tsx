import { useEffect, useState } from 'react';
import './ContractDeadline.css';

type ContractDeadlineProps = {
  /** Mốc hết hạn hợp đồng (chuỗi ISO từ backend: `matchDeadlineAt`). */
  readonly deadline: string;
  /** Chữ đứng trước đồng hồ. */
  readonly prefix?: string;
  /** Nhãn khi đã quá hạn. */
  readonly expiredLabel?: string;
  /** Chú thích khi rê chuột; mặc định mô tả luật 48 giờ. */
  readonly title?: string;
};

/**
 * Đồng hồ đếm ngược 48 giờ ký hợp đồng + chuyển tiền ký quỹ sau khi client chọn gia sư.
 * Hết giờ mà chưa ký xong và chưa có tiền vào hệ thống thì backend tự hủy hợp đồng, mở lại lớp
 * cho các gia sư đã ứng tuyển từ trước — nên đồng hồ này đổi sang trạng thái "hết hạn" tại chỗ.
 *
 * Khác {@link ExpiryBadge} (hạn hiển thị tin 30 ngày): ở đây đếm theo giờ và cảnh báo sớm hơn.
 */
export function ContractDeadline({
  deadline,
  prefix = 'Còn',
  expiredLabel = 'Hợp đồng đã hết hạn 48 giờ',
  title,
}: ContractDeadlineProps) {
  const end = new Date(deadline).getTime();
  const [msLeft, setMsLeft] = useState(() => end - Date.now());

  useEffect(() => {
    /** Cập nhật số mili-giây còn lại tới hạn. */
    const tick = () => setMsLeft(end - Date.now());
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [end]);

  /** Thêm số 0 đằng trước cho đủ 2 chữ số. */
  const pad = (n: number) => String(n).padStart(2, '0');
  let label: string;
  let tone: 'ok' | 'warn' | 'urgent' | 'expired';

  if (!Number.isFinite(end)) {
    return null;
  }
  if (msLeft <= 0) {
    label = expiredLabel;
    tone = 'expired';
  } else {
    const totalSec = Math.floor(msLeft / 1000);
    const hours = Math.floor(totalSec / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    const secs = totalSec % 60;
    label = `${prefix} ${pad(hours)}:${pad(mins)}:${pad(secs)}`;
    // Dưới 6 giờ là gấp, dưới 24 giờ là cảnh báo.
    tone = hours < 6 ? 'urgent' : hours < 24 ? 'warn' : 'ok';
  }

  return (
    <span
      className={`tcs-deadline tcs-deadline--${tone}`}
      title={
        title ??
        `Hợp đồng phải được hai bên ký và thanh toán ký quỹ trước ${new Date(deadline).toLocaleString('vi-VN')}.` +
          ' Quá hạn, lớp sẽ tự mở lại cho các gia sư đã ứng tuyển trước đó.'
      }
    >
      <span aria-hidden="true">⏳</span>
      {label}
    </span>
  );
}
