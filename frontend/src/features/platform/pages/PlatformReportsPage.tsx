import { useState } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { useDisputeReviewList } from '../hooks/useDisputeReviewList';
import { useReportList } from '../hooks/useReportList';
import type {
  AdminDisputeReviewApiResponse,
  DisputeReviewItem,
  DisputeStatus,
  EscrowStatus,
  ReportStatus,
} from '../types/platformTypes';
import './PlatformReportsPage.css';

function reportBadgeClass(status: ReportStatus) {
  return status === 'PENDING' ? 'tcs-badge tcs-badge--suspended' : 'tcs-badge tcs-badge--active';
}

function disputeBadgeClass(status: DisputeStatus) {
  if (status === 'RESOLVED') return 'tcs-badge tcs-badge--active';
  if (status === 'UNDER_INVESTIGATION') return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--suspended';
}

function escrowBadgeClass(status: EscrowStatus | null) {
  if (status === 'RELEASED' || status === 'REFUNDED') return 'tcs-badge tcs-badge--active';
  if (status === 'DISPUTED' || status === 'ON_HOLD') return 'tcs-badge tcs-badge--suspended';
  if (!status) return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--role';
}

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

const formatCurrency = (value: number | null | undefined) => {
  if (typeof value !== 'number') return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
};

function InfoRow({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div className="pd-info-row">
      <span className="pd-info-row__label">{label}</span>
      <span className="pd-info-row__value">{value ?? '—'}</span>
    </div>
  );
}

function DisputeDetail({
  detail,
  status,
  errorMessage,
}: {
  detail: AdminDisputeReviewApiResponse | null;
  status: 'loading' | 'success' | 'error';
  errorMessage: string | null;
}) {
  if (!detail) {
    return (
      <div className="pd-detail pd-detail--empty">
        <p>Chọn một tranh chấp để xem chi tiết.</p>
      </div>
    );
  }

  if (status === 'loading') {
    return (
      <div className="pd-detail">
        <div className="adm-state">Đang tải chi tiết tranh chấp…</div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="pd-detail">
        <div className="adm-alert adm-alert--error">
          {errorMessage ?? 'Không tải được chi tiết tranh chấp.'}
        </div>
      </div>
    );
  }

  return (
    <div className="pd-detail">
      <div className="pd-detail__head">
        <div>
          <p className="pd-detail__eyebrow">Tranh chấp #{detail.disputeId}</p>
          <h2 className="pd-detail__title">{detail.tutoringClass?.title ?? 'Không xác định lớp'}</h2>
        </div>
        <span className={disputeBadgeClass(detail.disputeStatus)}>{detail.disputeStatus}</span>
      </div>

      <section className="pd-section">
        <h3 className="pd-section__title">Báo cáo</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã báo cáo" value={detail.reportId ? `#${detail.reportId}` : '—'} />
          <InfoRow label="Người báo cáo" value={detail.reporterEmail ?? detail.reporterId} />
          <InfoRow label="Loại đối tượng" value={detail.targetType} />
          <InfoRow label="ID đối tượng" value={detail.targetId ? `#${detail.targetId}` : '—'} />
          <InfoRow label="Danh mục" value={detail.category} />
          <InfoRow label="Tạo lúc" value={formatDateTime(detail.reportCreatedAt)} />
        </div>
        <p className="pd-description">{detail.description ?? '—'}</p>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Bằng chứng</h3>
        {detail.evidenceUrlList.length === 0 ? (
          <p className="adm-muted">Không có bằng chứng đính kèm.</p>
        ) : (
          <div className="pd-evidence-list">
            {detail.evidenceUrlList.map((url) => (
              <a key={url} className="pd-evidence-link" href={url} target="_blank" rel="noreferrer">
                {url}
              </a>
            ))}
          </div>
        )}
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Escrow</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã escrow" value={detail.escrow?.escrowId ? `#${detail.escrow.escrowId}` : '—'} />
          <InfoRow label="Trạng thái" value={detail.escrow?.status ?? '—'} />
          <InfoRow label="Số tiền" value={formatCurrency(detail.escrow?.amount)} />
          <InfoRow label="Mã tham chiếu" value={detail.escrow?.paymentReferenceCode} />
          <InfoRow label="Người thanh toán" value={detail.escrow?.payerEmail ?? detail.escrow?.payerUserId} />
          <InfoRow label="Khóa lúc" value={formatDateTime(detail.escrow?.depositedAt)} />
        </div>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Lớp học</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã lớp" value={detail.tutoringClass?.classId ? `#${detail.tutoringClass.classId}` : '—'} />
          <InfoRow label="Trạng thái lớp" value={detail.tutoringClass?.status} />
          <InfoRow label="Chủ lớp" value={detail.tutoringClass?.creatorEmail ?? detail.tutoringClass?.creatorUserId} />
          <InfoRow label="Gia sư" value={detail.tutoringClass?.tutorName ?? detail.tutoringClass?.tutorEmail} />
          <InfoRow label="Assignment" value={detail.tutoringClass?.assignmentId ? `#${detail.tutoringClass.assignmentId}` : '—'} />
          <InfoRow label="Học viên" value={detail.tutoringClass?.studentName} />
        </div>
      </section>

      {detail.terminationRequest && (
        <section className="pd-section">
          <h3 className="pd-section__title">Yêu cầu chấm dứt sớm</h3>
          <div className="pd-info-grid">
            <InfoRow label="Mã yêu cầu" value={`#${detail.terminationRequest.terminationId}`} />
            <InfoRow label="Trạng thái" value={detail.terminationRequest.status} />
            <InfoRow label="Người yêu cầu" value={detail.terminationRequest.requestedByEmail ?? detail.terminationRequest.requestedByUserId} />
            <InfoRow label="Ngày hiệu lực" value={formatDate(detail.terminationRequest.effectiveDate)} />
          </div>
          <p className="pd-description">{detail.terminationRequest.reason ?? '—'}</p>
        </section>
      )}
    </div>
  );
}

export default function PlatformReportsPage() {
  const reports = useReportList();
  const [disputeStatusFilter, setDisputeStatusFilter] = useState<DisputeStatus | undefined>();
  const disputes = useDisputeReviewList(disputeStatusFilter);

  const openReportCount = reports.items.filter((item) => item.status === 'PENDING').length;
  const openDisputeCount = disputes.items.filter((item) => item.status !== 'RESOLVED').length;
  const reviewingCount = disputes.items.filter((item) => item.status === 'UNDER_INVESTIGATION').length;

  const selectDispute = (item: DisputeReviewItem) => {
    disputes.selectDispute(item);
  };

  return (
    <AdminLayout
      title="Báo cáo & tranh chấp"
      subtitle="Theo dõi báo cáo vi phạm, tranh chấp lớp học và bằng chứng liên quan."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Tranh chấp cần xử lý</p>
          <p className="adm-summary-card__value">{openDisputeCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Đang xem xét</p>
          <p className="adm-summary-card__value">{reviewingCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Báo cáo đang mở</p>
          <p className="adm-summary-card__value">{openReportCount}</p>
        </article>
      </div>

      <section className="pd-console" aria-label="Danh sách tranh chấp">
        <div className="adm-card pd-console__list">
          <div className="pd-card-head">
            <div>
              <h2 className="pd-card-head__title">Tranh chấp</h2>
              <p className="pd-card-head__meta">{disputes.items.length} hồ sơ</p>
            </div>
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={disputes.reload}>
              Làm mới
            </button>
          </div>

          <div className="adm-toolbar">
            <select
              className="adm-field"
              value={disputeStatusFilter ?? ''}
              onChange={(event) =>
                setDisputeStatusFilter((event.target.value as DisputeStatus) || undefined)
              }
            >
              <option value="">Tất cả trạng thái</option>
              <option value="OPEN">Mới mở</option>
              <option value="UNDER_INVESTIGATION">Đang xem xét</option>
              <option value="WAITING">Chờ bổ sung</option>
              <option value="RESOLVED">Đã xử lý</option>
            </select>
          </div>

          {disputes.status === 'loading' && <div className="adm-state">Đang tải danh sách tranh chấp…</div>}
          {disputes.status === 'error' && (
            <div className="adm-state">
              <p>{disputes.errorMessage ?? 'Không tải được dữ liệu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={disputes.reload}>
                Thử lại
              </button>
            </div>
          )}

          {disputes.status === 'success' && (
            <div className="pd-dispute-list">
              {disputes.items.length === 0 ? (
                <div className="adm-state">Chưa có tranh chấp nào.</div>
              ) : (
                disputes.items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`pd-dispute-item${
                      disputes.selected?.disputeId === item.raw.disputeId ? ' pd-dispute-item--active' : ''
                    }`}
                    onClick={() => selectDispute(item)}
                  >
                    <span className="pd-dispute-item__top">
                      <span className="pd-dispute-item__id">#{item.id}</span>
                      <span className={disputeBadgeClass(item.status)}>{item.statusLabel}</span>
                    </span>
                    <span className="pd-dispute-item__title">{item.classTitle}</span>
                    <span className="pd-dispute-item__desc">{item.description}</span>
                    <span className="pd-dispute-item__meta">
                      {item.amount} · {item.evidenceCount} bằng chứng · {item.createdAt}
                      <span className={escrowBadgeClass(item.escrowStatus)}>{item.escrowStatusLabel}</span>
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <DisputeDetail
          detail={disputes.selected}
          status={disputes.selectedStatus}
          errorMessage={disputes.detailErrorMessage}
        />
      </section>

      <div className="adm-card pd-report-card">
        <div className="pd-card-head">
          <div>
            <h2 className="pd-card-head__title">Báo cáo thường</h2>
            <p className="pd-card-head__meta">{reports.items.length} báo cáo</p>
          </div>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reports.reload}>
            Làm mới
          </button>
        </div>

        {reports.status === 'loading' && (
          <div className="adm-state adm-state--loading">Đang tải danh sách báo cáo…</div>
        )}

        {reports.status === 'error' && (
          <div className="adm-state">
            <p>{reports.errorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={reports.reload}>
              Thử lại
            </button>
          </div>
        )}

        {reports.status === 'success' && (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Người báo cáo</th>
                  <th>Đối tượng</th>
                  <th>ID đối tượng</th>
                  <th>Danh mục</th>
                  <th>Mô tả</th>
                  <th>Trạng thái</th>
                  <th>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {reports.items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có báo cáo nào.</td>
                  </tr>
                ) : (
                  reports.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>#{item.reporterId}</td>
                      <td>{item.targetTypeLabel}</td>
                      <td>{item.targetId}</td>
                      <td>{item.categoryLabel}</td>
                      <td className="adm-table__notes">{item.description}</td>
                      <td className="adm-table__badge">
                        <span className={reportBadgeClass(item.status)}>{item.statusLabel}</span>
                      </td>
                      <td>{item.createdAt}</td>
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
