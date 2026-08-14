import { useContractList } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { normalizeRole } from '../../../shared/auth/rbac';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'contract-status--pending' },
  DRAFT: { label: 'Chưa ký', cls: 'contract-status--draft' },
  SIGNED: { label: 'Đã ký', cls: 'contract-status--signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'contract-status--active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'contract-status--completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'contract-status--terminated' },
};

export default function ContractListPage() {
  const { contracts, loading, error, reload } = useContractList();
  const { user } = useAuth();
  const viewerRole = normalizeRole(user?.role);

  if (loading) {
    return (
      <div className="tcs-page">
        <HomeNavbar />
        <div className="contract-page">
          <div className="contract-loading">Đang tải hợp đồng...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="tcs-page">
        <HomeNavbar />
        <div className="contract-page">
          <div className="contract-error">{error}</div>
          <button className="btn btn-primary" onClick={reload}>Thử lại</button>
        </div>
      </div>
    );
  }

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="contract-page">
        <div className="contract-header">
          <h1>Hợp đồng của tôi</h1>
          <button className="btn btn-outline" onClick={reload}>
            Làm mới
          </button>
        </div>

        {contracts.length === 0 ? (
          <div className="contract-empty">
            <p>Chưa có hợp đồng nào.</p>
            <p className="contract-empty-hint">
              Hợp đồng sẽ được tạo khi bạn nhận lớp gia sư hoặc ghi danh vào lớp trung tâm.
            </p>
          </div>
        ) : (
          <div className="contract-table-wrapper">
            <table className="contract-table">
              <thead>
                <tr>
                  <th>Số HĐ</th>
                  <th>Lớp học</th>
                  <th>Người ký với</th>
                  <th>Phí</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {contracts.map((c) => {
                  const st = STATUS_LABEL[c.status] ?? { label: c.status, cls: '' };
                  // Chỉ hiện BÊN KIA mà mình ký cùng — ẩn chính người đang xem theo vai trò.
                  const signers = [
                    viewerRole !== 'CLIENT' ? c.clientName : null,
                    viewerRole !== 'TUTOR' ? c.tutorName : null,
                    viewerRole !== 'TUTOR_CENTER' ? c.centerName : null,
                  ]
                    .filter((n): n is string => Boolean(n))
                    .join(' · ');
                  return (
                    <tr key={c.contractId}>
                      <td className="contract-no">{c.contractNo}</td>
                      <td>{c.classTitle ?? '—'}</td>
                      <td>{signers || '—'}</td>
                      <td>
                        {c.tuitionFee != null && c.tuitionFee !== ''
                          ? `${new Intl.NumberFormat('vi-VN').format(Number(c.tuitionFee))} đ`
                          : '—'}
                      </td>
                      <td>
                        <span className={`contract-status ${st.cls}`}>{st.label}</span>
                      </td>
                      <td>{new Date(c.createdAt).toLocaleDateString('vi-VN')}</td>
                      <td>
                        <a href={`/contract/${c.contractId}`} className="btn-link">
                          Chi tiết
                        </a>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
