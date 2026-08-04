export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value);
}

export const hhmm = (time: string | null | undefined): string => (time ?? '').slice(0, 5);

export const hhmmDisplay = (time: string | null | undefined): string => {
  const t = hhmm(time);
  return t === '23:59' ? '00:00' : t;
};

export function toIsoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
