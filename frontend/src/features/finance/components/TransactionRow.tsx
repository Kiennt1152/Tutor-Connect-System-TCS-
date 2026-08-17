import type { Transaction } from '../types/financeTypes';
import { formatCurrency, formatDate, statusLabel } from '../mappers/financeMapper';

interface Props {
  transaction: Transaction;
}

export function TransactionRow({ transaction }: Props) {
  const isCredit =
    transaction.type === 'DEPOSIT' ||
    transaction.type === 'REFUND' ||
    transaction.type === 'ESCROW_RELEASE';
  const typeTone = isCredit ? 'credit' : 'debit';
  const typeInitial = {
    DEPOSIT: 'N',
    WITHDRAWAL: 'R',
    REFUND: 'H',
    ESCROW_DEPOSIT: 'E',
    ESCROW_RELEASE: 'G',
    PLATFORM_FEE: 'P',
  }[transaction.type] ?? 'T';

  const statusClass = {
    SUCCESS: 'tx-row__status--success',
    PENDING: 'tx-row__status--pending',
    FAILED: 'tx-row__status--failed',
    CANCELLED: 'tx-row__status--cancelled',
  }[transaction.status] ?? '';

  return (
    <tr className="tx-row">
      <td className="tx-row__time">{formatDate(transaction.createdAt)}</td>
      <td className="tx-row__type">
        <span className={`tx-badge tx-badge--${typeTone}`}>
          <span className="tx-badge__mark">{typeInitial}</span>
          {statusLabel(transaction.type)}
        </span>
      </td>
      <td className="tx-row__desc">
        {transaction.description ?? '—'}
        {transaction.referenceCode && (
          <span className="tx-row__ref">#{transaction.referenceCode}</span>
        )}
        {transaction.type === 'ESCROW_RELEASE' && (
          <span className="tx-row__note">Số tiền đã trừ phí nền tảng.</span>
        )}
      </td>
      <td className={`tx-row__amount tx-row__amount--${isCredit ? 'credit' : 'debit'}`}>
        {isCredit ? '+' : '−'}{formatCurrency(transaction.amount)}
      </td>
      <td className="tx-row__status">
        <span className={`tx-row__status-badge ${statusClass}`}>
          {transaction.status === 'SUCCESS' ? 'Thành công' :
           transaction.status === 'PENDING' ? 'Đang xử lý' :
           transaction.status === 'FAILED' ? 'Thất bại' :
           transaction.status === 'CANCELLED' ? 'Đã hủy' : transaction.status}
        </span>
      </td>
    </tr>
  );
}
