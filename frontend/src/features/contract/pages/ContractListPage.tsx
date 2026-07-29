import { useContractList } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Đang chờ', cls: 'status-pending' },
  DRAFT: { label: 'Chưa ký', cls: 'status-draft' },
  SIGNED: { label: 'Đã ký', cls: 'status-signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'status-active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'status-completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'status-terminated' },
};

export default function ContractListPage() {
  const { contracts, loading, error, reload } = useContractList();

  if (loading) {
    return (
      <div className="contract-page">
        <div className="contract-loading">Đang tải hợp đồng...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="contract-page">
        <div className="contract-error">{error}</div>
        <button className="btn btn-primary" onClick={reload}>Thử lại</button>
      </div>
    );
  }

  return (
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
                <th>Loại</th>
                <th>Phí</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {contracts.map((c) => {
                const st = STATUS_LABEL[c.status] ?? { label: c.status, cls: '' };
                return (
                  <tr key={c.contractId}>
                    <td className="contract-no">{c.contractNo}</td>
                    <td>{c.clientName ?? '—'}</td>
                    <td>{c.sourceType}</td>
                    <td>—</td>
                    <td>
                      <span className={`status-badge ${st.cls}`}>{st.label}</span>
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

      <style>{`
        .contract-page {
          max-width: 960px;
          margin: 0 auto;
          padding: 24px 16px;
          font-family: 'Segoe UI', Arial, sans-serif;
        }
        .contract-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 24px;
        }
        .contract-header h1 { margin: 0; font-size: 24px; color: #1a1a2e; }
        .contract-loading, .contract-error {
          text-align: center;
          padding: 48px;
          color: #64748b;
        }
        .contract-error { color: #dc2626; }
        .contract-empty {
          text-align: center;
          padding: 48px 24px;
          background: #f8fafc;
          border-radius: 12px;
          color: #64748b;
        }
        .contract-empty-hint { font-size: 14px; margin-top: 8px; color: #94a3b8; }
        .contract-table-wrapper { overflow-x: auto; }
        .contract-table {
          width: 100%;
          border-collapse: collapse;
          background: #fff;
          border-radius: 12px;
          overflow: hidden;
          box-shadow: 0 1px 4px rgba(0,0,0,0.08);
        }
        .contract-table th {
          background: #f1f5f9;
          padding: 12px 16px;
          text-align: left;
          font-size: 13px;
          font-weight: 600;
          color: #475569;
          border-bottom: 1px solid #e2e8f0;
        }
        .contract-table td {
          padding: 12px 16px;
          font-size: 14px;
          color: #1e293b;
          border-bottom: 1px solid #f1f5f9;
        }
        .contract-table tr:last-child td { border-bottom: none; }
        .contract-table tr:hover td { background: #f8fafc; }
        .contract-no { font-weight: 600; color: #2563eb; font-family: monospace; }
        .status-badge {
          display: inline-block;
          padding: 3px 10px;
          border-radius: 20px;
          font-size: 12px;
          font-weight: 600;
        }
        .status-draft { background: #fef3c7; color: #92400e; }
        .status-signed { background: #d1fae5; color: #065f46; }
        .status-active { background: #dbeafe; color: #1e40af; }
        .status-completed { background: #f1f5f9; color: #475569; }
        .status-terminated { background: #fee2e2; color: #991b1b; }
        .btn-link {
          color: #2563eb;
          text-decoration: none;
          font-weight: 600;
          font-size: 14px;
        }
        .btn-link:hover { text-decoration: underline; }
        .btn { padding: 8px 16px; border-radius: 8px; font-size: 14px; cursor: pointer; border: none; }
        .btn-primary { background: #2563eb; color: #fff; }
        .btn-outline { background: #fff; border: 1px solid #e2e8f0; color: #475569; }
      `}</style>
    </div>
  );
}
