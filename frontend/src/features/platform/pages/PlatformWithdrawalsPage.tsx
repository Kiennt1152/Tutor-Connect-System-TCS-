import { useMemo } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { useWithdrawalList } from '../hooks/useWithdrawalList';
import type { WithdrawalRequestStatus } from '../types/platformTypes';
import './PlatformWithdrawalsPage.css';

function statusBadgeClass(status: WithdrawalRequestStatus) {
  if (status === 'COMPLETED') return 'tcs-badge tcs-badge--active';
  if (status === 'REJECTED') return 'tcs-badge tcs-badge--banned';
  if (status === 'APPROVED') return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--suspended';
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

export default function PlatformWithdrawalsPage() {
  const { status, data, filters, setFilters, reload, errorMessage: listErrorMessage } = useWithdrawalList({
    page: 0,
    size: 10,
  });

  const pagePendingCount = useMemo(
    () => data?.items.filter((item) => item.status === 'PENDING').length ?? 0,
    [data],
  );
  const pageTotalAmount = useMemo(
    () => data?.items.reduce((sum, item) => sum + item.rawAmount, 0) ?? 0,
    [data],
  );

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  };

  return (
    <AdminLayout
      title="Yêu cầu rút tiền"
      subtitle="Theo dõi yêu cầu rút tiền và trạng thái đối soát tự động từ SePay."
    >
      {data && (
        <section className="pw-summary" aria-label="Tổng quan yêu cầu rút tiền">
          <article className="pw-summary-card pw-summary-card--warn">
            <span className="pw-summary-card__label">Đang chờ trên trang</span>
            <strong className="pw-summary-card__value">{pagePendingCount}</strong>
          </article>
          <article className="pw-summary-card">
            <span className="pw-summary-card__label">Tổng yêu cầu</span>
            <strong className="pw-summary-card__value">{data.totalElements}</strong>
          </article>
          <article className="pw-summary-card">
            <span className="pw-summary-card__label">Tổng tiền trên trang</span>
            <strong className="pw-summary-card__value">{formatCurrency(pageTotalAmount)}</strong>
          </article>
        </section>
      )}

      <div className="adm-card pw-card">
        <div className="adm-toolbar pw-toolbar">
          <select
            className="adm-field"
            value={filters.status ?? ''}
            onChange={(event) =>
              applyFilter({
                status: (event.target.value as WithdrawalRequestStatus) || undefined,
              })
            }
          >
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING">Chờ xử lý</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="COMPLETED">Thành công</option>
            <option value="REJECTED">Từ chối</option>
          </select>
          <select
            className="adm-field"
            value={filters.size}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                page: 0,
                size: Number(event.target.value),
              }))
            }
          >
            <option value={10}>10 dòng/trang</option>
            <option value={20}>20 dòng/trang</option>
            <option value={50}>50 dòng/trang</option>
          </select>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {status === 'loading' && <div className="adm-state">Đang tải yêu cầu rút tiền...</div>}
        {status === 'error' && (
          <div className="adm-state">
            <p>{listErrorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && data && (
          <>
            <div className="adm-table-wrap">
              <table className="adm-table pw-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Người yêu cầu</th>
                    <th>Số tiền</th>
                    <th>Tài khoản nhận</th>
                    <th>Mã giao dịch</th>
                    <th>Trạng thái</th>
                    <th>Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.length === 0 ? (
                    <tr>
                      <td colSpan={7}>Chưa có yêu cầu rút tiền nào.</td>
                    </tr>
                  ) : (
                    data.items.map((item) => (
                      <tr key={item.id}>
                        <td className="pw-table__id">#{item.id}</td>
                        <td>
                          <div className="pw-user-cell">
                            <strong>{item.requester}</strong>
                            <span>Ví #{item.walletId}</span>
                          </div>
                        </td>
                        <td className="pw-table__amount">{item.amount}</td>
                        <td>
                          <div className="pw-bank-cell">
                            <strong>{item.bankName}</strong>
                            <span>{item.accountNoMasked}</span>
                          </div>
                        </td>
                        <td>
                          <div className="pw-ref-cell">
                            <code>{item.referenceCode}</code>
                            <span>{item.transactionStatusLabel}</span>
                          </div>
                        </td>
                        <td className="adm-table__badge">
                          <span className={statusBadgeClass(item.status)}>{item.statusLabel}</span>
                        </td>
                        <td>
                          <div className="pw-time-cell">
                            <span>{item.requestedAt}</span>
                            <small>{item.processedAt !== '—' ? `Xử lý: ${item.processedAt}` : 'Chưa xử lý'}</small>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="adm-pagination">
              <span>
                Trang {data.page + 1}/{Math.max(data.totalPages, 1)} · {data.totalElements} yêu cầu
              </span>
              <div className="adm-pagination__actions">
                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  disabled={data.page <= 0}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}
                >
                  Trước
                </button>
                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  disabled={data.page + 1 >= data.totalPages}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
                >
                  Sau
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  );
}
