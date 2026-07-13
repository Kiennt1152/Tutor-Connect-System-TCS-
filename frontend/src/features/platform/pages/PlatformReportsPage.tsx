import { AdminLayout } from '../components/AdminLayout';
import { useReportList } from '../hooks/useReportList';
import type { ReportStatus } from '../types/platformTypes';

function reportBadgeClass(status: ReportStatus) {
  return status === 'PENDING' ? 'tcs-badge tcs-badge--suspended' : 'tcs-badge tcs-badge--active';
}

export default function PlatformReportsPage() {
  const { status, items, errorMessage, reload } = useReportList();

  const openCount = items.filter((item) => item.status === 'PENDING').length;

  return (
    <AdminLayout
      title="Xử lý báo cáo"
      subtitle="Theo dõi báo cáo vi phạm từ người dùng trên nền tảng."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Đang mở</p>
          <p className="adm-summary-card__value">{openCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tổng báo cáo</p>
          <p className="adm-summary-card__value">{items.length}</p>
        </article>
      </div>

      <div className="adm-card">
        <div className="adm-alert adm-alert--info">
          Chức năng đánh dấu &quot;Đã xử lý&quot; sẽ được bổ sung khi API cập nhật trạng thái báo cáo
          sẵn sàng.
        </div>

        <div className="adm-toolbar">
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách báo cáo…
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
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có báo cáo nào.</td>
                  </tr>
                ) : (
                  items.map((item) => (
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
