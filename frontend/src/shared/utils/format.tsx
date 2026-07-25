export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value);
}

/** '18:00:00' → '18:00'. Chịu được null vì nhiều chỗ dùng giờ của buổi cũ (có thể trống). */
export const hhmm = (time: string | null | undefined): string => (time ?? '').slice(0, 5);

/** 'YYYY-MM-DD' theo giờ máy — KHÔNG dùng toISOString vì nó quy về UTC, lệch ngày. */
export function toIsoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
