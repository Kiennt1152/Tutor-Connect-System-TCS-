import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { Pagination } from '../../../shared/components';
import { platformApi } from '../api/platformApi';
import type { AdminEscrowApiResponse, EscrowStatus } from '../types/platformTypes';

export function AdminEscrowQueue() {
  const [items, setItems] = useState<AdminEscrowApiResponse[]>([]);
  const [status, setStatus] = useState<'' | EscrowStatus>('');
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const load = useCallback(async () => {
    try {
      const filters: Record<string, string> = { page: '0', size: '100' };
      if (status) filters.status = status;
      if (keyword.trim()) filters.reference = keyword.trim();
      setItems((await platformApi.getEscrows(filters)).data.content);
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không tải được escrow.'));
    }
  }, [keyword, status]);

  useEffect(() => {
    const id = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(id);
  }, [load]);

  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, status, items.length]);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (validCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, items.length);
  const paginatedItems = items.slice(startIndex, endIndex);

  return (
    <section className="adm-card" style={{ marginBottom: 16 }}>
      <div className="adm-toolbar">
        <input
          className="adm-field"
          placeholder="Mã tham chiếu"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <select
          className="adm-field"
          value={status}
          onChange={(e) => setStatus(e.target.value as '' | EscrowStatus)}
        >
          <option value="">Tất cả trạng thái</option>
          {['PENDING', 'FUNDED', 'ON_HOLD', 'DISPUTED', 'RELEASED', 'REFUNDED'].map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void load()}>
          Làm mới
        </button>
      </div>

      {error && <div className="adm-alert adm-alert--error">{error}</div>}

      <div className="adm-table-wrap">
        <table className="adm-table">
          <thead>
            <tr>
              <th>Escrow</th>
              <th>Tham chiếu</th>
              <th>Người trả</th>
              <th>Người nhận</th>
              <th>Số tiền</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && (
              <tr>
                <td colSpan={6}>Không có escrow phù hợp.</td>
              </tr>
            )}
            {paginatedItems.map((item) => (
              <tr key={item.escrowId}>
                <td>#{item.escrowId}</td>
                <td>{item.referenceCode ?? '—'}</td>
                <td>{item.payerEmail}</td>
                <td>{item.beneficiaryEmail ?? '—'}</td>
                <td>{item.amount.toLocaleString('vi-VN')} VND</td>
                <td>
                  <span className="tcs-badge tcs-badge--role">{item.status}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {items.length > 0 && (
        <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
          <select
            className="adm-field adm-field--fixed"
            style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
            value={pageSize}
            onChange={(e) => {
              setPageSize(Number(e.target.value));
              setCurrentPage(1);
            }}
          >
            <option value={10}>10 / trang</option>
            <option value={20}>20 / trang</option>
            <option value={50}>50 / trang</option>
          </select>
          <Pagination
            current={validCurrentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        </div>
      )}
    </section>
  );
}
