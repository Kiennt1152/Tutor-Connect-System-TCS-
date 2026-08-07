import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useContractList } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'contract-status--pending' },
  DRAFT: { label: 'Chưa ký', cls: 'contract-status--draft' },
  SIGNED: { label: 'Đã ký', cls: 'contract-status--signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'contract-status--active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'contract-status--completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'contract-status--terminated' },
};

const formatCurrency = (value: number | string | null) => {
  if (value == null) return '—';
  return `${Number(value).toLocaleString('vi-VN')} đ`;
};

const formatDate = (value: string) => new Date(value).toLocaleDateString('vi-VN');

export default function ContractListPage() {
  const { contracts, loading, error, reload } = useContractList();

  if (loading) {
    return (
      <div className="contract-shell">
        <HomeNavbar />
        <main className="contract-page tcs-container">
          <div className="contract-state">Đang tải hợp đồng...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="contract-shell">
        <HomeNavbar />
        <main className="contract-page tcs-container">
          <div className="contract-state contract-state--error">{error}</div>
          <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
            Thử lại
          </button>
        </main>
      </div>
    );
  }

  return (
    <div className="contract-shell">
      <HomeNavbar />
      <main className="contract-page tcs-container">
        <section className="contract-page-head">
          <div>
            <p className="contract-eyebrow">Quản lý hợp đồng</p>
            <h1>Hợp đồng của tôi</h1>
            <p>Theo dõi trạng thái ký, hiệu lực và xử lý phát sinh của từng lớp học.</p>
          </div>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </section>

        {contracts.length === 0 ? (
          <section className="contract-empty">
            <strong>Chưa có hợp đồng nào.</strong>
            <span>Hợp đồng sẽ xuất hiện sau khi bạn nhận lớp hoặc ghi danh lớp trung tâm.</span>
          </section>
        ) : (
          <section className="contract-table-card" aria-label="Danh sách hợp đồng">
            <div className="contract-table-card__head">
              <h2>Danh sách hợp đồng</h2>
              <span>{contracts.length} hợp đồng</span>
            </div>
            <div className="contract-table-wrapper">
              <table className="contract-table">
                <thead>
                  <tr>
                    <th>Số hợp đồng</th>
                    <th>Lớp học</th>
                    <th>Loại</th>
                    <th>Học phí</th>
                    <th>Trạng thái</th>
                    <th>Ngày tạo</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {contracts.map((contract) => {
                    const status = STATUS_LABEL[contract.status] ?? {
                      label: contract.status,
                      cls: '',
                    };
                    return (
                      <tr key={contract.contractId}>
                        <td className="contract-no">{contract.contractNo}</td>
                        <td className="contract-table__title">{contract.classTitle ?? '—'}</td>
                        <td>{contract.classType ?? '—'}</td>
                        <td>{formatCurrency(contract.tuitionFee)}</td>
                        <td>
                          <span className={`contract-status ${status.cls}`}>{status.label}</span>
                        </td>
                        <td>{formatDate(contract.createdAt)}</td>
                        <td>
                          <Link to={`/contract/${contract.contractId}`} className="contract-action-link">
                            Chi tiết
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
