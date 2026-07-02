import { AdminLayout } from '../components/AdminLayout';
import { useReviewVerification } from '../hooks/usePlatformMutations';
import { useVerificationList } from '../hooks/useVerificationList';
import type { VerificationStatus } from '../types/platformTypes';

function verificationBadgeClass(status: VerificationStatus) {
  if (status === 'VERIFIED') return 'tcs-badge tcs-badge--active';
  if (status === 'REJECTED') return 'tcs-badge tcs-badge--banned';
  if (status === 'SUBMITTED' || status === 'UNDER_REVIEW') return 'tcs-badge tcs-badge--suspended';
  return 'tcs-badge tcs-badge--role';
}

export default function PlatformVerificationsPage() {
  const { status, items, errorMessage, reload } = useVerificationList();
  const { status: mutationStatus, errorMessage: mutationError, review, reset } =
    useReviewVerification();

  const pendingCount = items.filter((item) => item.canReview).length;

  const handleReview = async (id: string, decision: 'VERIFIED' | 'REJECTED') => {
    const label = decision === 'VERIFIED' ? 'duyệt' : 'từ chối';
    if (!window.confirm(`Xác nhận ${label} hồ sơ xác minh #${id}?`)) return;
    const notes = window.prompt('Ghi chú admin (tuỳ chọn):') ?? undefined;
    reset();
    const ok = await review(id, decision, notes);
    if (ok) reload();
  };

  return (
    <AdminLayout
      title="Duyệt xác minh"
      subtitle="Xử lý hồ sơ xác minh gia sư và trung tâm gia sư."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Chờ xử lý</p>
          <p className="adm-summary-card__value">{pendingCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tổng yêu cầu</p>
          <p className="adm-summary-card__value">{items.length}</p>
        </article>
      </div>

      <div className="adm-card">
        {mutationStatus === 'error' && mutationError && (
          <div className="adm-alert adm-alert--error">{mutationError}</div>
        )}

        <div className="adm-toolbar">
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách xác minh…
          </div>
        )}

        {status === 'error' && (
          <div className="adm-state">
            <p>{errorMessage ?? 'Không tải được dữ liệu.'}</p>
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
                  <th>Email</th>
                  <th>Loại</th>
                  <th>Trạng thái</th>
                  <th>Gửi lúc</th>
                  <th>Duyệt lúc</th>
                  <th>Ghi chú</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có yêu cầu xác minh nào.</td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>{item.userEmail}</td>
                      <td>{item.typeLabel}</td>
                      <td className="adm-table__badge">
                        <span className={verificationBadgeClass(item.status)}>
                          {item.statusLabel}
                        </span>
                      </td>
                      <td>{item.submittedAt}</td>
                      <td>{item.reviewedAt}</td>
                      <td className="adm-table__notes">{item.adminNotes}</td>
                      <td className="adm-table__actions">
                        {item.canReview ? (
                          <div className="adm-row-actions">
                            <button
                              className="tcs-btn tcs-btn--success tcs-btn--sm"
                              type="button"
                              disabled={mutationStatus === 'loading'}
                              onClick={() => handleReview(item.id, 'VERIFIED')}
                            >
                              Duyệt
                            </button>
                            <button
                              className="tcs-btn tcs-btn--danger tcs-btn--sm"
                              type="button"
                              disabled={mutationStatus === 'loading'}
                              onClick={() => handleReview(item.id, 'REJECTED')}
                            >
                              Từ chối
                            </button>
                          </div>
                        ) : (
                          <span className="adm-muted">—</span>
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
