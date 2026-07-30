import { useState } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { useReviewModeration } from '../hooks/useReviewModeration';
import type { AdminReviewApiResponse, ReviewModerationStatus } from '../types/platformTypes';
import './PlatformReviewsPage.css';

const STATUS_LABEL: Record<ReviewModerationStatus, string> = {
  VISIBLE: 'Đang hiển thị',
  HIDDEN: 'Đã ẩn',
  MODERATED: 'Vi phạm',
};

function statusBadgeClass(status: ReviewModerationStatus) {
  if (status === 'VISIBLE') return 'tcs-badge tcs-badge--active';
  return 'tcs-badge tcs-badge--suspended';
}

function formatDate(iso: string) {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('vi-VN');
}

export default function PlatformReviewsPage() {
  const { status, items, errorMessage, reload, moderate, remove } = useReviewModeration();
  const [busyId, setBusyId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const visibleCount = items.filter((r) => r.status === 'VISIBLE').length;
  const flaggedCount = items.filter((r) => r.status !== 'VISIBLE').length;

  function runAction(reviewId: number, fn: () => Promise<unknown>) {
    setBusyId(reviewId);
    setActionError(null);
    fn()
      .catch(() => setActionError('Thao tác không thành công. Vui lòng thử lại.'))
      .finally(() => setBusyId(null));
  }

  function handleModerate(review: AdminReviewApiResponse, next: ReviewModerationStatus) {
    runAction(review.reviewId, () => moderate(review.reviewId, next));
  }

  function handleDelete(review: AdminReviewApiResponse) {
    const ok = window.confirm(
      `Xóa vĩnh viễn đánh giá của ${review.reviewerName ?? 'khách hàng'}? Hành động này không thể hoàn tác.`,
    );
    if (!ok) return;
    runAction(review.reviewId, () => remove(review.reviewId));
  }

  return (
    <AdminLayout
      title="Kiểm duyệt đánh giá"
      subtitle="Xem, ẩn/hiện, đánh dấu vi phạm hoặc xóa đánh giá của khách hàng."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Đang hiển thị</p>
          <p className="adm-summary-card__value">{visibleCount}</p>
        </article>
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Đã ẩn / vi phạm</p>
          <p className="adm-summary-card__value">{flaggedCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tổng đánh giá</p>
          <p className="adm-summary-card__value">{items.length}</p>
        </article>
      </div>

      <div className="adm-card">
        <div className="adm-toolbar">
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {actionError && <div className="adm-alert adm-alert--info">{actionError}</div>}

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách đánh giá…
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
            <table className="adm-table adm-table--reviews">
              <thead>
                <tr>
                  <th>Khách hàng</th>
                  <th>Gia sư</th>
                  <th>Lớp</th>
                  <th>Điểm</th>
                  <th>Nội dung</th>
                  <th>Trạng thái</th>
                  <th>Thời gian</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có đánh giá nào.</td>
                  </tr>
                ) : (
                  items.map((r) => (
                    <tr key={r.reviewId}>
                      <td>
                        <div className="adm-review__name">{r.reviewerName ?? '—'}</div>
                        <div className="adm-table__sub adm-review__email">{r.reviewerEmail}</div>
                        {r.anonymous ? (
                          <span className="tcs-badge tcs-badge--suspended adm-review__anon">
                            Ẩn danh: “{r.publicDisplayName}”
                          </span>
                        ) : null}
                      </td>
                      <td className="adm-review__tutor">{r.tutorName ?? '—'}</td>
                      <td>
                        {r.classTitle ?? '—'}
                        {r.subjectName ? (
                          <div className="adm-table__sub">{r.subjectName}</div>
                        ) : null}
                      </td>
                      <td>{r.rating != null ? `${r.rating.toFixed(1)}/5` : '—'}</td>
                      <td className="adm-table__notes">
                        {r.comment ? `“${r.comment}”` : <span className="adm-table__sub">—</span>}
                        {r.tutorReply ? (
                          <div className="adm-table__sub">↩ GS: {r.tutorReply}</div>
                        ) : null}
                      </td>
                      <td className="adm-table__badge">
                        <span className={statusBadgeClass(r.status)}>{STATUS_LABEL[r.status]}</span>
                      </td>
                      <td>{formatDate(r.createdAt)}</td>
                      <td>
                        <div className="adm-row-actions">
                          {r.status === 'VISIBLE' ? (
                            <button
                              type="button"
                              className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                              disabled={busyId === r.reviewId}
                              onClick={() => handleModerate(r, 'HIDDEN')}
                            >
                              Ẩn
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                              disabled={busyId === r.reviewId}
                              onClick={() => handleModerate(r, 'VISIBLE')}
                            >
                              Hiện lại
                            </button>
                          )}
                          {r.status !== 'MODERATED' ? (
                            <button
                              type="button"
                              className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                              disabled={busyId === r.reviewId}
                              onClick={() => handleModerate(r, 'MODERATED')}
                            >
                              Đánh dấu vi phạm
                            </button>
                          ) : null}
                          <button
                            type="button"
                            className="tcs-btn tcs-btn--danger tcs-btn--sm"
                            disabled={busyId === r.reviewId}
                            onClick={() => handleDelete(r)}
                          >
                            Xóa
                          </button>
                        </div>
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
