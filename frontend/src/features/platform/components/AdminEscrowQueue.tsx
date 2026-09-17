import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { Pagination } from '../../../shared/components';
import { platformApi } from '../api/platformApi';
import { ESCROW_STATUS_LABELS } from '../mappers/platformMapper';
import type { AdminEscrowApiResponse, EscrowStatus } from '../types/platformTypes';

const ESCROW_STATUS_OPTIONS: { value: EscrowStatus; label: string }[] = [
  { value: 'PENDING', label: 'Chờ nạp tiền' },
  { value: 'FUNDED', label: 'Đã khóa ký quỹ' },
  { value: 'ON_HOLD', label: 'Tạm giữ' },
  { value: 'DISPUTED', label: 'Đang tranh chấp' },
  { value: 'RELEASED', label: 'Đã giải ngân' },
  { value: 'REFUNDED', label: 'Đã hoàn tiền' },
];

function escrowBadgeClass(status: EscrowStatus) {
  switch (status) {
    case 'RELEASED':
      return 'tcs-badge tcs-badge--active';
    case 'REFUNDED':
      return 'tcs-badge tcs-badge--completed';
    case 'DISPUTED':
    case 'ON_HOLD':
      return 'tcs-badge tcs-badge--suspended';
    case 'FUNDED':
      return 'tcs-badge tcs-badge--role';
    default:
      return 'tcs-badge';
  }
}

export function AdminEscrowQueue() {
  const [items, setItems] = useState<AdminEscrowApiResponse[]>([]);
  const [status, setStatus] = useState<'' | EscrowStatus>('');
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedEscrow, setSelectedEscrow] = useState<AdminEscrowApiResponse | null>(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const filters: Record<string, string> = { page: '0', size: '100' };
      if (status) filters.status = status;
      if (keyword.trim()) filters.reference = keyword.trim();
      const res = await platformApi.getEscrows(filters);
      setItems(res.data?.content || []);
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không tải được danh sách giao dịch escrow.'));
    } finally {
      setLoading(false);
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

  // Quick KPI summaries
  const totalAmount = items.reduce((sum, item) => sum + (item.amount || 0), 0);
  const fundedCount = items.filter((i) => i.status === 'FUNDED' || i.status === 'ON_HOLD').length;
  const releasedCount = items.filter((i) => i.status === 'RELEASED').length;
  const refundedCount = items.filter((i) => i.status === 'REFUNDED').length;

  return (
    <div>
      {/* KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '12px',
          marginBottom: '16px',
        }}
      >
        <div className="adm-card" style={{ padding: '14px 16px' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Tổng số khoản ký quỹ</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '4px' }}>
            {items.length} <span style={{ fontSize: '0.85rem', fontWeight: 400, color: '#64748b' }}>({totalAmount.toLocaleString('vi-VN')} ₫)</span>
          </div>
        </div>
        <div className="adm-card" style={{ padding: '14px 16px', borderLeft: '4px solid #2563eb' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Đang giữ / Đã khóa tiền</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#2563eb', marginTop: '4px' }}>
            {fundedCount} khoản
          </div>
        </div>
        <div className="adm-card" style={{ padding: '14px 16px', borderLeft: '4px solid #16a34a' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Đã giải ngân cho gia sư</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#16a34a', marginTop: '4px' }}>
            {releasedCount} khoản
          </div>
        </div>
        <div className="adm-card" style={{ padding: '14px 16px', borderLeft: '4px solid #dc2626' }}>
          <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Đã hoàn tiền khách hàng</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#dc2626', marginTop: '4px' }}>
            {refundedCount} khoản
          </div>
        </div>
      </div>

      <section className="adm-card" style={{ marginBottom: 16 }}>
        <div className="adm-toolbar" style={{ flexWrap: 'wrap', gap: '8px' }}>
          <input
            className="adm-field"
            style={{ minWidth: '240px' }}
            placeholder="Tìm theo mã tham chiếu..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <select
            className="adm-field"
            value={status}
            onChange={(e) => setStatus(e.target.value as '' | EscrowStatus)}
          >
            <option value="">Tất cả trạng thái ký quỹ</option>
            {ESCROW_STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <button
            className="tcs-btn tcs-btn--ghost"
            type="button"
            onClick={() => void load()}
            disabled={loading}
          >
            {loading ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>

        {error && <div className="adm-alert adm-alert--error">{error}</div>}

        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Mã ký quỹ</th>
                <th>Mã tham chiếu</th>
                <th>Người thanh toán</th>
                <th>Người thụ hưởng</th>
                <th style={{ textAlign: 'right' }}>Số tiền (VNĐ)</th>
                <th>Trạng thái ký quỹ</th>
                <th style={{ textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Đang tải danh sách ký quỹ...
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Không có giao dịch ký quỹ nào phù hợp.
                  </td>
                </tr>
              ) : (
                paginatedItems.map((item) => (
                  <tr key={item.escrowId}>
                    <td><strong>#{item.escrowId}</strong></td>
                    <td><code style={{ fontSize: '0.8rem', background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }}>{item.referenceCode ?? '—'}</code></td>
                    <td>{item.payerEmail}</td>
                    <td>{item.beneficiaryEmail ?? '—'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                      {item.amount.toLocaleString('vi-VN')} ₫
                    </td>
                    <td>
                      <span className={escrowBadgeClass(item.status)}>
                        {ESCROW_STATUS_LABELS[item.status] ?? item.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="tcs-btn tcs-btn--secondary"
                        style={{ height: '30px', padding: '0 10px', fontSize: '12px' }}
                        onClick={() => setSelectedEscrow(item)}
                      >
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                ))
              )}
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

      {/* Escrow Detail Modal */}
      {selectedEscrow && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            backdropFilter: 'blur(2px)',
            zIndex: 999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
          }}
          onClick={() => setSelectedEscrow(null)}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '12px',
              maxWidth: '600px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)',
              display: 'flex',
              flexDirection: 'column',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '1.25rem 1.5rem',
                borderBottom: '1px solid #e2e8f0',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span style={{ fontSize: '1.2rem', fontWeight: 700 }}>Khoản ký quỹ #{selectedEscrow.escrowId}</span>
                <span className={escrowBadgeClass(selectedEscrow.status)}>
                  {ESCROW_STATUS_LABELS[selectedEscrow.status] ?? selectedEscrow.status}
                </span>
              </div>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}
                onClick={() => setSelectedEscrow(null)}
              >
                &times;
              </button>
            </div>

            <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Số tiền ký quỹ</span>
                  <div style={{ fontSize: '1.2rem', fontWeight: 700, color: '#16a34a' }}>
                    {selectedEscrow.amount.toLocaleString('vi-VN')} ₫
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Mã tham chiếu</span>
                  <div style={{ fontSize: '0.95rem', fontWeight: 600 }}>{selectedEscrow.referenceCode || '—'}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Người thanh toán (Payer)</span>
                  <div style={{ fontSize: '0.95rem', fontWeight: 500 }}>{selectedEscrow.payerEmail}</div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b' }}>User ID: #{selectedEscrow.payerUserId}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Người thụ hưởng (Beneficiary)</span>
                  <div style={{ fontSize: '0.95rem', fontWeight: 500 }}>{selectedEscrow.beneficiaryEmail || 'Chưa xác định'}</div>
                  {selectedEscrow.beneficiaryUserId && (
                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>User ID: #{selectedEscrow.beneficiaryUserId}</div>
                  )}
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Mã thanh toán gốc</span>
                  <div style={{ fontSize: '0.95rem' }}>#{selectedEscrow.paymentId}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Mã phân công (Assignment)</span>
                  <div style={{ fontSize: '0.95rem' }}>{selectedEscrow.assignmentId ? `#${selectedEscrow.assignmentId}` : '—'}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Thời điểm nạp tiền</span>
                  <div style={{ fontSize: '0.85rem' }}>{selectedEscrow.depositedAt || '—'}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Thời điểm giải ngân</span>
                  <div style={{ fontSize: '0.85rem' }}>{selectedEscrow.releasedAt || '—'}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Ngày khởi tạo</span>
                  <div style={{ fontSize: '0.85rem' }}>{selectedEscrow.createdAt || '—'}</div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>Cập nhật lần cuối</span>
                  <div style={{ fontSize: '0.85rem' }}>{selectedEscrow.updatedAt || '—'}</div>
                </div>
              </div>
            </div>

            <div
              style={{
                padding: '1rem 1.5rem',
                borderTop: '1px solid #e2e8f0',
                display: 'flex',
                justifyContent: 'flex-end',
              }}
            >
              <button
                className="tcs-btn tcs-btn--secondary"
                onClick={() => setSelectedEscrow(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
