export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value);
}

/** Cắt chuỗi giờ về "HH:mm". */
export const hhmm = (time: string | null | undefined): string => (time ?? '').slice(0, 5);

/** Giờ đã lưu đúng dạng hiển thị (00:00 = nửa đêm), chỉ cần cắt về HH:mm. */
export const hhmmDisplay = (time: string | null | undefined): string => hhmm(time);

/**
 * Mốc kết thúc nửa đêm. "00:00" ở GIỜ KẾT THÚC nghĩa là 24:00 của chính ngày hôm đó, không
 * phải 0h đầu ngày — vì vậy không bao giờ so sánh chuỗi giờ kết thúc trực tiếp ("00:00" là
 * chuỗi nhỏ nhất, sẽ làm mọi phép so trùng lịch và tính giờ sai). Luôn đi qua các hàm dưới.
 * Bản backend tương ứng: com.tcs.common.util.SlotTime.
 */
export const MIDNIGHT_END = '00:00';

/** Phút trong ngày của giờ BẮT ĐẦU (0..1439). */
export const startMinutes = (time: string | null | undefined): number => {
  const [h, m] = hhmm(time).split(':').map(Number);
  return Number.isFinite(h) && Number.isFinite(m) ? h * 60 + m : Number.NaN;
};

/** Phút trong ngày của giờ KẾT THÚC (1..1440); nửa đêm = 1440. */
export const endMinutes = (time: string | null | undefined): number =>
  hhmm(time) === MIDNIGHT_END ? 24 * 60 : startMinutes(time);

/** Khung giờ hợp lệ (kết thúc thực sự sau bắt đầu, tính cả mốc nửa đêm). */
export const isValidTimeRange = (start: string | null | undefined, end: string | null | undefined) =>
  endMinutes(end) > startMinutes(start);

/** Hai khung giờ trong cùng một ngày có chồng lấn nhau không. */
export const slotOverlaps = (
  start: string,
  end: string,
  otherStart: string,
  otherEnd: string,
): boolean =>
  startMinutes(start) < endMinutes(otherEnd) && startMinutes(otherStart) < endMinutes(end);

/** Đổi Date sang "yyyy-MM-dd" theo giờ máy (không lệch múi giờ như toISOString). */
export function toIsoDate(d: Date): string {
  /** Thêm số 0 đằng trước cho đủ 2 chữ số. */
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
