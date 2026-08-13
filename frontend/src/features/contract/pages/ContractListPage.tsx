import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useContractList } from '../hooks/useContract';
import type { ContractApiResponse, ContractStatus, EscrowPaymentInfo } from '../types/contractTypes';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'contract-status--pending' },
  DRAFT: { label: 'Chưa ký', cls: 'contract-status--draft' },
  SIGNED: { label: 'Đã ký', cls: 'contract-status--signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'contract-status--active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'contract-status--completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'contract-status--terminated' },
};

const SOURCE_LABEL: Record<'PRIVATE' | 'CENTER', string> = {
  PRIVATE: 'Lớp riêng',
  CENTER: 'Lớp trung tâm',
};

const ESCROW_STATUS_LABEL: Record<string, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ nạp', cls: 'contract-status--pending' },
  FUNDED: { label: 'Đã nạp', cls: 'contract-status--active' },
  ON_HOLD: { label: 'Đang giữ', cls: 'contract-status--signed' },
  DISPUTED: { label: 'Tranh chấp', cls: 'contract-status--terminated' },
  RELEASED: { label: 'Đã giải ngân', cls: 'contract-status--completed' },
  REFUNDED: { label: 'Đã hoàn tiền', cls: 'contract-status--completed' },
};

const PAYMENT_STATUS_LABEL: Record<string, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ xác nhận', cls: 'contract-status--pending' },
  SUCCESS: { label: 'Thành công', cls: 'contract-status--active' },
  FAILED: { label: 'Thất bại', cls: 'contract-status--terminated' },
  CANCELLED: { label: 'Đã hủy', cls: 'contract-status--completed' },
};

const formatCurrency = (value: number | string | null | undefined) => {
  const amount = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(amount)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
};

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

const getEscrowLabel = (escrow: EscrowPaymentInfo | null | undefined) => {
  if (!escrow) return null;
  return ESCROW_STATUS_LABEL[escrow.escrowStatus] ?? {
    label: escrow.escrowStatus,
    cls: 'contract-status--draft',
  };
};

export default function ContractListPage() {
  const { contracts, loading, error, reload } = useContractList();
  const escrowContracts = useMemo(
    () => contracts.filter((contract: ContractApiResponse) => contract.escrowPayment != null),
    [contracts],
  );
  const escrowSummary = useMemo(() => {
    const pending = escrowContracts.filter((contract) => {
      const escrow = contract.escrowPayment;
      return escrow?.paymentStatus === 'PENDING' || escrow?.escrowStatus === 'PENDING';
    }).length;
    const active = escrowContracts.filter((contract) => {
      const status = contract.escrowPayment?.escrowStatus;
      return status === 'FUNDED' || status === 'ON_HOLD';
    }).length;
    const finished = escrowContracts.filter((contract) => {
      const status = contract.escrowPayment?.escrowStatus;
      return status === 'RELEASED' || status === 'REFUNDED';
    }).length;
    return { pending, active, finished };
  }, [escrowContracts]);

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

        <div className="contract-section-stack">
          <section className="contract-table-card">
            <div className="contract-table-card__head">
              <div>
                <h2>Danh sách hợp đồng</h2>
                <span>Tất cả hợp đồng bạn tham gia và trạng thái hiện tại.</span>
              </div>
              <span>{contracts.length} hợp đồng</span>
            </div>

            {contracts.length === 0 ? (
              <div className="contract-empty">
                <strong>Chưa có hợp đồng nào.</strong>
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
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {contracts.map((c) => {
                      const st = STATUS_LABEL[c.status] ?? { label: c.status, cls: 'contract-status--draft' };
                      return (
                        <tr key={c.contractId}>
                          <td className="contract-no">{c.contractNo}</td>
                          <td className="contract-table__title">{c.classTitle ?? c.clientName ?? '—'}</td>
                          <td>{SOURCE_LABEL[c.sourceType ?? 'PRIVATE'] ?? c.sourceType ?? '—'}</td>
                          <td>{formatCurrency(c.tuitionFee)}</td>
                          <td>
                            <span className={`contract-status ${st.cls}`}>{st.label}</span>
                          </td>
                          <td>{new Date(c.createdAt).toLocaleDateString('vi-VN')}</td>
                          <td>
                            <Link to={`/contract/${c.contractId}`} className="contract-action-link">
                              Chi tiết
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="contract-table-card">
            <div className="contract-table-card__head">
              <div>
                <h2>Escrows của tôi</h2>
                <span>Theo dõi các escrow gắn với hợp đồng của bạn.</span>
              </div>
              <span>{escrowContracts.length} giao dịch</span>
            </div>

            <div className="contract-escrow-summary">
              <div className="contract-escrow-summary__item">
                <span>Tổng escrow</span>
                <strong>{escrowContracts.length}</strong>
              </div>
              <div className="contract-escrow-summary__item">
                <span>Đang chờ</span>
                <strong>{escrowSummary.pending}</strong>
              </div>
              <div className="contract-escrow-summary__item">
                <span>Đang giữ</span>
                <strong>{escrowSummary.active}</strong>
              </div>
              <div className="contract-escrow-summary__item">
                <span>Đã hoàn tất</span>
                <strong>{escrowSummary.finished}</strong>
              </div>
            </div>

            {escrowContracts.length === 0 ? (
              <div className="contract-empty">
                <strong>Chưa có escrow nào.</strong>
                <p className="contract-empty-hint">
                  Khi hợp đồng chuyển sang bước thanh toán, escrow tương ứng sẽ xuất hiện ở đây.
                </p>
              </div>
            ) : (
              <div className="contract-table-wrapper">
                <table className="contract-table">
                  <thead>
                    <tr>
                      <th>Escrow</th>
                      <th>Hợp đồng</th>
                      <th>Lớp học</th>
                      <th>Số tiền</th>
                      <th>Escrow</th>
                      <th>Thanh toán</th>
                      <th>Cập nhật</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {escrowContracts.map((contract) => {
                      const escrow = contract.escrowPayment!;
                      const escrowLabel = getEscrowLabel(escrow);
                      const paymentLabel = escrow.paymentStatus
                        ? PAYMENT_STATUS_LABEL[escrow.paymentStatus] ?? {
                            label: escrow.paymentStatus,
                            cls: 'contract-status--draft',
                          }
                        : null;
                      const lastUpdate = escrow.processedAt ?? escrow.depositedAt ?? contract.updatedAt;
                      return (
                        <tr key={contract.contractId}>
                          <td>
                            <div className="contract-escrow-code">
                              <strong>#{escrow.escrowId}</strong>
                              <code>{escrow.referenceCode ?? '—'}</code>
                            </div>
                          </td>
                          <td className="contract-table__title">{contract.contractNo}</td>
                          <td>{contract.classTitle ?? '—'}</td>
                          <td>{formatCurrency(escrow.amount)}</td>
                          <td>
                            {escrowLabel ? (
                              <span className={`contract-status ${escrowLabel.cls}`}>{escrowLabel.label}</span>
                            ) : (
                              '—'
                            )}
                          </td>
                          <td>
                            {paymentLabel ? (
                              <span className={`contract-status ${paymentLabel.cls}`}>{paymentLabel.label}</span>
                            ) : (
                              '—'
                            )}
                          </td>
                          <td>{formatDateTime(lastUpdate)}</td>
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
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
