import type { TransactionPage } from '../types/financeTypes';
import { TransactionRow } from './TransactionRow';
import type { TransactionFilter } from '../types/financeTypes';

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
];

export function TransactionList({ page, loading, filters, onFilterChange }: Props) {
  const totalPages = page.totalPages;

  return (
    <div className="tx-list">
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
            ← Trước
          </button>
          <span className="tx-list__page-info">
            Trang {page.page + 1} / {totalPages} — {page.totalElements} giao dịch
          </span>
          <button
            className="tx-list__page-btn"
            disabled={page.page >= totalPages - 1}
            onClick={() =>
              onFilterChange({ ...filters, page: (page.page + 1) })
            }
          >
            Sau →
          </button>
        </div>
      )}
    </div>
  );
}
