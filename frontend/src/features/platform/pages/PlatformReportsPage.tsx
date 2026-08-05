import { AdminLayout } from '../components/AdminLayout';
import { useReportList } from '../hooks/useReportList';
import type { ReportStatus } from '../types/platformTypes';
import { useState } from 'react';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';

function reportBadgeClass(status: ReportStatus) {
  return status === 'PENDING' ? 'tcs-badge tcs-badge--suspended' : 'tcs-badge tcs-badge--active';
}

export default function PlatformReportsPage() {
  const { status, items, errorMessage, reload } = useReportList();
  const [resolvingId, setResolvingId] = useState<number | null>(null);

  const handleResolve = async (id: number) => {
    try {
      setResolvingId(id);
      await platformApi.resolveReport(id, { status: 'RESOLVED' });
      reload();
    } catch (err) {
      alert(getApiErrorMessage(err));
    } finally {
      setResolvingId(null);
    }
  };

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
                  <th>Hành động</th>
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
                      <td className="adm-table__actions">
                        {item.status === 'PENDING' && (
                          <button
                            className="tcs-btn tcs-btn--sm tcs-btn--primary"
                            type="button"
                            onClick={() => handleResolve(Number(item.id))}
                            disabled={resolvingId === Number(item.id)}
                          >
                            {resolvingId === Number(item.id) ? 'Đang xử lý...' : 'Đã xử lý'}
                          </button>
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
