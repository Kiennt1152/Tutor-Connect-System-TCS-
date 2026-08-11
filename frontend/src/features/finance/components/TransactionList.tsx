import type { TransactionPage } from '../types/financeTypes';
import { TransactionRow } from './TransactionRow';
import type { TransactionFilter } from '../types/financeTypes';
import { formatCurrency } from '../mappers/financeMapper';

interface Props {
  page: TransactionPage;
  loading?: boolean;
  filters: TransactionFilter;
  onFilterChange: (filters: TransactionFilter) => void;
}

const TX_TYPES = [
  { value: '', label: 'Tất cả' },
  { value: 'DEPOSIT', label: 'Nạp tiền' },
  { value: 'WITHDRAWAL', label: 'Rút tiền' },
  { value: 'REFUND', label: 'Hoàn tiền' },
  { value: 'ESCROW_DEPOSIT', label: 'Đặt cọc Escrow' },
  { value: 'ESCROW_RELEASE', label: 'Giải ngân Escrow' },
  { value: 'PLATFORM_FEE', label: 'Phí nền tảng' },
];

export function TransactionList({ page, loading, filters, onFilterChange }: Props) {
  const totalPages = page.totalPages;
  const pageSize = filters.size ?? 10;
  const firstItem = page.totalElements === 0 ? 0 : page.page * pageSize + 1;
  const lastItem = Math.min((page.page + 1) * pageSize, page.totalElements);
  const pageCredit = page.transactions
    .filter((tx) =>
      tx.type === 'DEPOSIT'
      || tx.type === 'REFUND'
      || tx.type === 'ESCROW_RELEASE'
      || tx.type === 'PLATFORM_FEE')
    .reduce((sum, tx) => sum + tx.amount, 0);
  const pageDebit = page.transactions
    .filter((tx) => tx.type === 'WITHDRAWAL' || tx.type === 'ESCROW_DEPOSIT')
    .reduce((sum, tx) => sum + tx.amount, 0);

  return (
    <div className="tx-list">
      <div className="tx-list__summary">
        <div className="tx-list__summary-item">
          <span>Tổng giao dịch</span>
          <strong>{page.totalElements}</strong>
        </div>
        <div className="tx-list__summary-item tx-list__summary-item--credit">
          <span>Tiền vào trang này</span>
          <strong>{formatCurrency(pageCredit)}</strong>
        </div>
        <div className="tx-list__summary-item tx-list__summary-item--debit">
          <span>Tiền ra trang này</span>
          <strong>{formatCurrency(pageDebit)}</strong>
        </div>
        <label className="tx-list__page-size">
          <span>Số dòng</span>
          <select
            value={pageSize}
            onChange={(event) =>
              onFilterChange({ ...filters, page: 0, size: Number(event.target.value) })
            }
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>
        </label>
      </div>

      {/* Filter bar */}
      <div className="tx-list__filters">
        <select
          className="tx-list__filter-select"
          value={filters.type ?? ''}
          onChange={(e) =>
            onFilterChange({ ...filters, page: 0, type: e.target.value || undefined })
          }
        >
          {TX_TYPES.map((t) => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>
        <input
          type="date"
          className="tx-list__filter-date"
          value={filters.from ?? ''}
          onChange={(e) =>
            onFilterChange({ ...filters, page: 0, from: e.target.value || undefined })
          }
          placeholder="Từ ngày"
        />
        <span className="tx-list__filter-sep">—</span>
        <input
          type="date"
          className="tx-list__filter-date"
          value={filters.to ?? ''}
          onChange={(e) =>
            onFilterChange({ ...filters, page: 0, to: e.target.value || undefined })
          }
          placeholder="Đến ngày"
        />
      </div>

      {/* Table */}
      <div className="tx-list__table-wrap">
        {loading ? (
          <div className="tx-list__loading">
            <div className="spinner" />
          </div>
        ) : page.transactions.length === 0 ? (
          <div className="tx-list__empty">
            <p>Chưa có giao dịch nào.</p>
          </div>
        ) : (
          <table className="tx-list__table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Loại</th>
                <th>Mô tả</th>
                <th>Số tiền</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {page.transactions.map((tx) => (
                <TransactionRow key={tx.transactionId} transaction={tx} />
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="tx-list__pagination">
          <button
            className="tx-list__page-btn"
            disabled={page.page === 0}
            onClick={() =>
              onFilterChange({ ...filters, page: (page.page - 1) })
            }
          >
            Trước
          </button>
          <span className="tx-list__page-info">
            {firstItem}-{lastItem} / {page.totalElements} giao dịch · Trang {page.page + 1}/{totalPages}
          </span>
          <button
            className="tx-list__page-btn"
            disabled={page.page >= totalPages - 1}
            onClick={() =>
              onFilterChange({ ...filters, page: (page.page + 1) })
            }
          >
            Sau
          </button>
        </div>
      )}
    </div>
  );
}
