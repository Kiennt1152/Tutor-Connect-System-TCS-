import type { Transaction } from '../types/financeTypes';

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDate(dateStr: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(dateStr));
}

export function statusLabel(type: Transaction['type']): string {
  const map: Record<Transaction['type'], string> = {
    DEPOSIT: 'Nạp tiền',
    WITHDRAWAL: 'Rút tiền',
    REFUND: 'Hoàn tiền',
    ESCROW_DEPOSIT: 'Đặt cọc ký quỹ',
    ESCROW_RELEASE: 'Giải ngân ký quỹ',
    PLATFORM_FEE: 'Phí nền tảng',
  };
  return map[type] ?? type;
}

export function walletStatusLabel(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: 'Hoạt động',
    SUSPENDED: 'Bị khóa',
    CLOSED: 'Đã đóng',
  };
  return map[status] ?? status;
}
