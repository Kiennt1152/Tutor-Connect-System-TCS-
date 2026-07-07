import { useCallback, useEffect, useState } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import type { AdminWithdrawal, WithdrawalStatus } from '../../finance/types/financeTypes';

type LoadStatus = 'loading' | 'success' | 'error';

const vnd = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value) + ' ₫';

const STATUS_LABEL: Record<WithdrawalStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  COMPLETED: 'Hoàn tất',
};

function badgeClass(status: WithdrawalStatus) {
  if (status === 'PENDING') return 'tcs-badge tcs-badge--suspended';
  if (status === 'REJECTED') return 'tcs-badge tcs-badge--suspended';
  return 'tcs-badge tcs-badge--active';
}

export default function PlatformWithdrawalsPage() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [items, setItems] = useState<AdminWithdrawal[]>([]);
  const [filter, setFilter] = useState<WithdrawalStatus | 'ALL'>('PENDING');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setStatus('loading');
    try {
      const res = await platformApi.getWithdrawals(filter === 'ALL' ? undefined : filter);
      setItems(res.data);
      setStatus('success');
    } catch {
      setStatus('error');
    }
  }, [filter]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function approve(item: AdminWithdrawal) {
    setBusyId(item.withdrawalId);
    setError('');
    try {
      await platformApi.reviewWithdrawal(item.withdrawalId, { approve: true });
      await reload();
    } catch {
      setError('Không duyệt được yêu cầu.');
    } finally {
      setBusyId(null);
    }
  }

  async function reject(item: AdminWithdrawal) {
    const reason = window.prompt('Lý do từ chối yêu cầu rút này?');
    if (reason === null) return;
    if (!reason.trim()) {
      setError('Phải nhập lý do từ chối.');
      return;
    }
    setBusyId(item.withdrawalId);
    setError('');
    try {
      await platformApi.reviewWithdrawal(item.withdrawalId, { approve: false, reason: reason.trim() });
      await reload();
    } catch {
      setError('Không từ chối được yêu cầu.');
    } finally {
      setBusyId(null);
    }
  }

  const pendingCount = items.filter((i) => i.status === 'PENDING').length;

  return (
    <AdminLayout
      title="Duyệt yêu cầu rút tiền"
      subtitle="Xét duyệt yêu cầu rút tiền của khách hàng (Client) để chống spam và rửa tiền."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Chờ duyệt</p>
          <p className="adm-summary-card__value">
            {filter === 'PENDING' ? items.length : pendingCount}
          </p>
        </article>
      </div>

      <div className="adm-card">
        <div className="adm-toolbar">
          <div className="adm-tabs">
            <button
              className={`tcs-btn ${filter === 'PENDING' ? 'tcs-btn--market' : 'tcs-btn--ghost'}`}
              type="button"
              onClick={() => setFilter('PENDING')}
            >
              Chờ duyệt
            </button>
            <button
              className={`tcs-btn ${filter === 'ALL' ? 'tcs-btn--market' : 'tcs-btn--ghost'}`}
              type="button"
              onClick={() => setFilter('ALL')}
            >
              Tất cả
            </button>
          </div>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {error ? <div className="adm-alert adm-alert--info">{error}</div> : null}

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách…
          </div>
        )}

        {status === 'error' && (
          <div className="adm-state">
            <p>Không tải được dữ liệu.</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={reload}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Người dùng</th>
                  <th>Tài khoản nhận</th>
                  <th>Số tiền</th>
                  <th>Trạng thái</th>
                  <th>Thời gian</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={7}>Không có yêu cầu nào.</td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.withdrawalId}>
                      <td>#{item.withdrawalId}</td>
                      <td>{item.userEmail}</td>
                      <td>
                        {item.bankName} · {item.accountNo}
                        <br />
                        <span className="adm-table__notes">{item.accountName}</span>
                        {item.status === 'REJECTED' && item.failureReason ? (
                          <div className="adm-table__notes">Lý do: {item.failureReason}</div>
                        ) : null}
                      </td>
                      <td>{vnd(item.amount)}</td>
                      <td className="adm-table__badge">
                        <span className={badgeClass(item.status)}>{STATUS_LABEL[item.status]}</span>
                      </td>
                      <td>{new Date(item.requestedAt).toLocaleString('vi-VN')}</td>
                      <td>
                        {item.status === 'PENDING' ? (
                          <div className="adm-actions">
                            <button
                              className="tcs-btn tcs-btn--market"
                              type="button"
                              disabled={busyId === item.withdrawalId}
                              onClick={() => approve(item)}
                            >
                              Duyệt
                            </button>
                            <button
                              className="tcs-btn tcs-btn--ghost"
                              type="button"
                              disabled={busyId === item.withdrawalId}
                              onClick={() => reject(item)}
                            >
                              Từ chối
                            </button>
                          </div>
                        ) : (
                          <span className="adm-table__notes">—</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
