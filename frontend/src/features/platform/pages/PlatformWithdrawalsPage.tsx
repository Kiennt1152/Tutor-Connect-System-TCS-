import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter } from '../components/AdminTimeFilter';
import { Pagination } from '../../../shared/components';
import { useWithdrawalDecision } from '../hooks/usePlatformMutations';
import { useWithdrawalList } from '../hooks/useWithdrawalList';
import type { AdminWithdrawalItem, WithdrawalRequestStatus } from '../types/platformTypes';
import './PlatformWithdrawalsPage.css';

const AUTO_REFRESH_INTERVAL_MS = 5000;

function VisibilityIcon({ hidden }: { hidden: boolean }) {
  if (hidden) {
    return (
      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
        <path
          d="M2.5 12c1.8-4.3 5.5-7 9.5-7s7.7 2.7 9.5 7c-1.8 4.3-5.5 7-9.5 7s-7.7-2.7-9.5-7Zm9.5 4c2.2 0 4-1.8 4-4s-1.8-4-4-4-4 1.8-4 4 1.8 4 4 4Zm0-2.5A1.5 1.5 0 1 1 12 10a1.5 1.5 0 0 1 0 3Z"
          fill="currentColor"
        />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
      <path
        d="M12 5c4 0 7.7 2.7 9.5 7-1.8 4.3-5.5 7-9.5 7S4.3 16.3 2.5 12C4.3 7.7 8 5 12 5Zm0 2c-3 0-5.9 1.9-7.4 5 1.5 3.1 4.4 5 7.4 5s5.9-1.9 7.4-5c-1.5-3.1-4.4-5-7.4-5Zm0 2.5A2.5 2.5 0 1 1 12 14a2.5 2.5 0 0 1 0-5Z"
        fill="currentColor"
      />
    </svg>
  );
}

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
  const [searchParams] = useSearchParams();
  const targetId = searchParams.get('id');

  const { status, data, filters, setFilters, reload, errorMessage: listErrorMessage } = useWithdrawalList({
    page: 0,
    size: 10,
  });
  const {
    status: decisionStatus,
    errorMessage: decisionErrorMessage,
    approveWithdrawal,
    rejectWithdrawal,
    markTransferFailed,
    reset: resetDecision,
  } = useWithdrawalDecision();
  const [decisionDialog, setDecisionDialog] = useState<{
    type: 'reject' | 'transferFailed';
    withdrawalId: string;
    title: string;
  } | null>(null);
  const [decisionReason, setDecisionReason] = useState('');
  const [visibleAccountIds, setVisibleAccountIds] = useState<Record<string, boolean>>({});
  const [lastAutoRefreshAt, setLastAutoRefreshAt] = useState<Date | null>(null);
  const autoRefreshInFlightRef = useRef(false);

  const pagePendingCount = useMemo(
    () => data?.items.filter((item) => item.status === 'PENDING').length ?? 0,
    [data],
  );
  const pageTotalAmount = useMemo(
    () => data?.items.reduce((sum, item) => sum + item.rawAmount, 0) ?? 0,
    [data],
  );
  const lastAutoRefreshLabel = useMemo(() => {
    if (!lastAutoRefreshAt) return null;

    return lastAutoRefreshAt.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }, [lastAutoRefreshAt]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      if (document.visibilityState !== 'visible' || autoRefreshInFlightRef.current) return;

      autoRefreshInFlightRef.current = true;
      void reload({ silent: true })
        .then(() => setLastAutoRefreshAt(new Date()))
        .finally(() => {
          autoRefreshInFlightRef.current = false;
        });
    }, AUTO_REFRESH_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [reload]);

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  };

  const handleApprove = async (withdrawalId: string) => {
    const ok = await approveWithdrawal(withdrawalId);
    if (ok) reload();
  };

  const openDecisionDialog = (
    type: 'reject' | 'transferFailed',
    withdrawalId: string,
    title: string,
  ) => {
    resetDecision();
    setDecisionReason('');
    setDecisionDialog({ type, withdrawalId, title });
  };

  const closeDecisionDialog = () => {
    setDecisionDialog(null);
    setDecisionReason('');
    resetDecision();
  };

  const submitDecision = async () => {
    // Reject and transfer-failed both return locked money to the requester, but with different audit meaning.
    if (!decisionDialog) return;
    const reason = decisionReason.trim();
    const payload = reason ? { reason } : {};
    const ok = decisionDialog.type === 'reject'
      ? await rejectWithdrawal(decisionDialog.withdrawalId, payload)
      : await markTransferFailed(decisionDialog.withdrawalId, payload);
    if (ok) {
      closeDecisionDialog();
      reload();
    }
  };

  const toggleAccountVisibility = (withdrawalId: string) => {
    // Bank account numbers stay masked by default and are revealed per row only when admin needs to transfer.
    setVisibleAccountIds((current) => ({
      ...current,
      [withdrawalId]: !current[withdrawalId],
    }));
  };

  const isTargetRow = (item: AdminWithdrawalItem) => {
    if (!targetId) return false;

    return String(item.id) === String(targetId)
      || String(item.displayId) === String(targetId)
      || (item.raw.withdrawalId != null && String(item.raw.withdrawalId) === String(targetId))
      || (item.raw.refundId != null && String(item.raw.refundId) === String(targetId));
  };

  return (
    <AdminLayout
      title="Yêu cầu chuyển tiền"
      subtitle="Theo dõi yêu cầu rút tiền và hoàn tiền cần chuyển khoản ra ngoài."
    >
      {/* This page intentionally merges withdrawals and refund transfers so admin has one transfer queue. */}
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

      <AdminTimeFilter showGranularity={false} />

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
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => reload()}>
            Làm mới
          </button>
          <span className="pw-auto-refresh" aria-live="polite">
            Tự cập nhật mỗi 5 giây
            {lastAutoRefreshLabel ? ` · lần cuối ${lastAutoRefreshLabel}` : ''}
          </span>
        </div>

        {status === 'loading' && <div className="adm-state">Đang tải yêu cầu rút tiền...</div>}
        {status === 'error' && (
          <div className="adm-state">
            <p>{listErrorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={() => reload()}>
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
                    <th>Loại</th>
                    <th>Người yêu cầu</th>
                    <th>Số tiền</th>
                    <th>Tài khoản nhận</th>
                    <th>Mã giao dịch</th>
                    <th>Trạng thái</th>
                    <th>Thời gian</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.length === 0 ? (
                    <tr>
                      <td colSpan={9}>Chưa có yêu cầu chuyển tiền nào.</td>
                    </tr>
                  ) : (
                    data.items.map((item) => (
                      <tr key={item.id} style={{ backgroundColor: isTargetRow(item) ? '#fef3c7' : undefined }}>
                        <td className="pw-table__id">#{item.displayId}</td>
                        <td>
                          <span className={`pw-type-pill pw-type-pill--${item.requestType.toLowerCase()}`}>
                            {item.requestTypeLabel}
                          </span>
                        </td>
                        <td>
                          <div className="pw-user-cell">
                            <strong title={item.requester}>{item.requester}</strong>
                            <span title={item.requestType === 'REFUND' ? 'Người nhận hoàn tiền' : `Ví #${item.walletId}`}>
                              {item.requestType === 'REFUND' ? 'Người nhận hoàn tiền' : `Ví #${item.walletId}`}
                            </span>
                          </div>
                        </td>
                        <td className="pw-table__amount">{item.amount}</td>
                        <td>
                          <div className="pw-bank-cell">
                            <strong title={item.bankName}>{item.bankName}</strong>
                            <div className="pw-bank-row">
                              <span className="pw-bank-row__account" title={visibleAccountIds[item.id] && item.raw.accountNo
                                ? item.raw.accountNo
                                : item.accountNoMasked}>
                                {visibleAccountIds[item.id] && item.raw.accountNo
                                  ? item.raw.accountNo
                                  : item.accountNoMasked}
                              </span>
                              <button
                                className="pw-account-toggle"
                                type="button"
                                onClick={() => toggleAccountVisibility(item.id)}
                                aria-label={
                                  visibleAccountIds[item.id]
                                    ? 'Ẩn số tài khoản nhận'
                                    : 'Hiện số tài khoản nhận'
                                }
                                title={
                                  visibleAccountIds[item.id]
                                    ? 'Ẩn số tài khoản nhận'
                                    : 'Hiện số tài khoản nhận'
                                }
                              >
                                <VisibilityIcon hidden={!!visibleAccountIds[item.id]} />
                              </button>
                            </div>
                            <span className="pw-bank-cell__holder" title={item.accountHolderName || '—'}>
                              {item.accountHolderName || '—'}
                            </span>
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
                        <td>
                          <div className="pw-actions">
                            {item.canApprove && (
                              <button
                                className="tcs-btn tcs-btn--primary pw-action-btn"
                                type="button"
                                disabled={decisionStatus === 'loading'}
                                onClick={() => handleApprove(String(item.raw.withdrawalId))}
                              >
                                Duyệt
                              </button>
                            )}
                            {item.canReject && (
                              <button
                                className="tcs-btn tcs-btn--ghost pw-action-btn"
                                type="button"
                                disabled={decisionStatus === 'loading'}
                                onClick={() => openDecisionDialog('reject', String(item.raw.withdrawalId), 'Từ chối yêu cầu rút tiền')}
                              >
                                Từ chối
                              </button>
                            )}
                            {item.canMarkTransferFailed && (
                              <button
                                className="tcs-btn tcs-btn--ghost pw-action-btn pw-action-btn--danger"
                                type="button"
                                disabled={decisionStatus === 'loading'}
                                onClick={() =>
                                  openDecisionDialog('transferFailed', String(item.raw.withdrawalId), 'Báo lỗi chuyển khoản')
                                }
                              >
                                Báo lỗi chuyển
                              </button>
                            )}
                            {!item.canApprove && !item.canReject && !item.canMarkTransferFailed && (
                              <span className="pw-actions__empty">—</span>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
              <select
                className="adm-field adm-field--fixed"
                style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
                value={filters.size}
                onChange={(e) =>
                  setFilters((current) => ({
                    ...current,
                    size: Number(e.target.value),
                    page: 0,
                  }))
                }
              >
                <option value={10}>10 / trang</option>
                <option value={20}>20 / trang</option>
                <option value={50}>50 / trang</option>
              </select>
              <Pagination
                current={data.page + 1}
                totalPages={Math.max(data.totalPages, 1)}
                onPageChange={(p) => setFilters((current) => ({ ...current, page: p - 1 }))}
              />
            </div>
          </>
        )}
      </div>

      {decisionDialog && (
        <div className="pw-dialog-backdrop" role="presentation" onMouseDown={closeDecisionDialog}>
          <section
            className="pw-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="pw-decision-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <header className="pw-dialog__header">
              <h3 id="pw-decision-title">{decisionDialog.title}</h3>
              <button className="pw-dialog__close" type="button" onClick={closeDecisionDialog} aria-label="Đóng">
                ×
              </button>
            </header>
            <label className="pw-dialog__field">
              <span>Lý do xử lý</span>
              <textarea
                value={decisionReason}
                onChange={(event) => setDecisionReason(event.target.value)}
                placeholder="Nhập lý do để lưu vào lịch sử giao dịch"
                rows={4}
              />
            </label>
            {decisionErrorMessage && <p className="pw-dialog__error">{decisionErrorMessage}</p>}
            <footer className="pw-dialog__actions">
              <button className="tcs-btn tcs-btn--ghost" type="button" onClick={closeDecisionDialog}>
                Hủy
              </button>
              <button
                className="tcs-btn tcs-btn--primary"
                type="button"
                disabled={decisionStatus === 'loading'}
                onClick={submitDecision}
              >
                {decisionStatus === 'loading' ? 'Đang xử lý...' : 'Xác nhận'}
              </button>
            </footer>
          </section>
        </div>
      )}
    </AdminLayout>
  );
}
