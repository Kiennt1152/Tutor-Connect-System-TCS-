import { useState } from 'react';
import axios from 'axios';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { useReviewVerification } from '../hooks/usePlatformMutations';
import { useVerificationList } from '../hooks/useVerificationList';
import type {
  VerificationDetailApiResponse,
  VerificationDocumentType,
  VerificationRequestItem,
  VerificationStatus,
} from '../types/platformTypes';
import './PlatformVerificationsPage.css';

const DOC_LABEL: Record<VerificationDocumentType, string> = {
  ID_CARD: 'CMND/CCCD',
  DEGREE: 'Bằng cấp',
  CERTIFICATE: 'Chứng chỉ',
  LICENSE: 'Giấy phép',
};

function verificationBadgeClass(status: VerificationStatus) {
  if (status === 'VERIFIED') return 'tcs-badge tcs-badge--active';
  if (status === 'REJECTED') return 'tcs-badge tcs-badge--banned';
  if (status === 'SUBMITTED' || status === 'UNDER_REVIEW') return 'tcs-badge tcs-badge--suspended';
  return 'tcs-badge tcs-badge--role';
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export default function PlatformVerificationsPage() {
  const { status, items, errorMessage, reload } = useVerificationList();
  const { status: mutationStatus, errorMessage: mutationError, review, reset } =
    useReviewVerification();

  const [selected, setSelected] = useState<VerificationRequestItem | null>(null);
  const [detail, setDetail] = useState<VerificationDetailApiResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [rejecting, setRejecting] = useState(false);
  const [rejectNotes, setRejectNotes] = useState('');
  const [formError, setFormError] = useState('');

  const pendingCount = items.filter((item) => item.canReview).length;

  const closeModal = () => {
    setSelected(null);
    setDetail(null);
    setDetailError('');
    setRejecting(false);
    setRejectNotes('');
    setFormError('');
    reset();
  };

  const openDetail = async (item: VerificationRequestItem) => {
    setSelected(item);
    setDetail(null);
    setDetailError('');
    setRejecting(false);
    setRejectNotes('');
    setFormError('');
    reset();
    setDetailLoading(true);
    try {
      // BR-01: mở hồ sơ SUBMITTED sẽ tự động chuyển sang UNDER_REVIEW ở phía server.
      const response = await platformApi.getVerificationDetail(item.id);
      setDetail(response.data);
    } catch (error) {
      setDetailError(extractError(error, 'Không tải được chi tiết hồ sơ.'));
    } finally {
      setDetailLoading(false);
      // Làm mới danh sách để phản ánh trạng thái mới (UNDER_REVIEW).
      reload();
    }
  };

  const handleApprove = async () => {
    if (!selected) return;
    setFormError('');
    const ok = await review(selected.id, 'VERIFIED');
    if (ok) {
      closeModal();
    }
    // AF-02: dù thành công hay thất bại (đã bị người khác xử lý) đều làm mới danh sách.
    reload();
  };

  const handleReject = async () => {
    if (!selected) return;
    // BR-03 / AF-01: lý do từ chối bắt buộc, tối thiểu 10 ký tự.
    if (rejectNotes.trim().length < 10) {
      setFormError('Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).');
      return;
    }
    const ok = await review(selected.id, 'REJECTED', rejectNotes.trim());
    if (ok) {
      closeModal();
    }
    reload();
  };

  const displayStatus = detail?.status ?? selected?.status ?? 'SUBMITTED';

  return (
    <AdminLayout
      title="Duyệt xác minh"
      subtitle="Xử lý hồ sơ xác minh gia sư và trung tâm gia sư."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Chờ xử lý</p>
          <p className="adm-summary-card__value">{pendingCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tổng yêu cầu</p>
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
            Đang tải danh sách xác minh…
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
                  <th>Email</th>
                  <th>Loại</th>
                  <th>Trạng thái</th>
                  <th>Gửi lúc</th>
                  <th>Duyệt lúc</th>
                  <th>Ghi chú</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có yêu cầu xác minh nào.</td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>{item.userEmail}</td>
                      <td>{item.typeLabel}</td>
                      <td className="adm-table__badge">
                        <span className={verificationBadgeClass(item.status)}>
                          {item.statusLabel}
                        </span>
                      </td>
                      <td>{item.submittedAt}</td>
                      <td>{item.reviewedAt}</td>
                      <td className="adm-table__notes">{item.adminNotes}</td>
                      <td className="adm-table__actions">
                        <button
                          className="tcs-btn tcs-btn--market tcs-btn--sm"
                          type="button"
                          onClick={() => openDetail(item)}
                        >
                          {item.canReview ? 'Xem & duyệt' : 'Xem chi tiết'}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selected && (
        <div className="pv-modal" role="dialog" aria-modal="true">
          <div className="pv-modal__backdrop" onClick={closeModal} />
          <div className="pv-modal__card">
            <div className="pv-modal__head">
              <div>
                <h2 className="pv-modal__title">
                  {detail?.submitterName ?? selected.userEmail}
                </h2>
                <p className="pv-modal__sub">
                  {selected.typeLabel} ·{' '}
                  <span className={verificationBadgeClass(displayStatus)}>
                    {displayStatus === 'SUBMITTED'
                      ? 'Chờ duyệt'
                      : displayStatus === 'UNDER_REVIEW'
                        ? 'Đang xem xét'
                        : displayStatus === 'VERIFIED'
                          ? 'Đã duyệt'
                          : displayStatus === 'REJECTED'
                            ? 'Từ chối'
                            : displayStatus}
                  </span>
                </p>
              </div>
              <button
                className="pv-modal__close"
                type="button"
                onClick={closeModal}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>

            <div className="pv-modal__body">
              {detailLoading && <div className="adm-state">Đang tải chi tiết…</div>}
              {detailError && <div className="adm-alert adm-alert--error">{detailError}</div>}

              {detail && (
                <>
                  <section className="pv-section">
                    <h3 className="pv-section__title">Thông tin người nộp</h3>
                    <div className="pv-kv">
                      <div className="pv-kv__row">
                        <span className="pv-kv__k">Email</span>
                        <span className="pv-kv__v">{detail.userEmail}</span>
                      </div>
                      <div className="pv-kv__row">
                        <span className="pv-kv__k">Số điện thoại</span>
                        <span className="pv-kv__v">{detail.submitterPhone ?? '—'}</span>
                      </div>
                      {Object.entries(detail.submitterDetails).map(([k, v]) => (
                        <div className="pv-kv__row" key={k}>
                          <span className="pv-kv__k">{k}</span>
                          <span className="pv-kv__v">{v}</span>
                        </div>
                      ))}
                    </div>
                  </section>

                  <section className="pv-section">
                    <h3 className="pv-section__title">Tài liệu ({detail.documents.length})</h3>
                    {detail.documents.length === 0 ? (
                      <p className="adm-muted">Không có tài liệu đính kèm.</p>
                    ) : (
                      <ul className="pv-docs">
                        {detail.documents.map((doc) => (
                          <li className="pv-docs__item" key={doc.documentId}>
                            <span className="pv-docs__type">{DOC_LABEL[doc.documentType]}</span>
                            {doc.available && doc.fileUrl ? (
                              <a
                                className="pv-docs__link"
                                href={doc.fileUrl}
                                target="_blank"
                                rel="noreferrer"
                              >
                                {doc.fileName ?? 'Xem tài liệu'}
                              </a>
                            ) : (
                              <span className="pv-docs__broken">⚠ Thiếu / không đọc được</span>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                    {detail.hasUnreadableDocument && (
                      <div className="adm-alert adm-alert--warning">
                        Có tài liệu bị thiếu hoặc không đọc được — cân nhắc kỹ trước khi Duyệt.
                      </div>
                    )}
                  </section>
                </>
              )}

              {formError && <div className="adm-alert adm-alert--error">{formError}</div>}
              {mutationStatus === 'error' && mutationError && (
                <div className="adm-alert adm-alert--error">{mutationError}</div>
              )}

              {rejecting && (
                <section className="pv-section">
                  <h3 className="pv-section__title">Lý do từ chối</h3>
                  <textarea
                    className="pv-textarea"
                    rows={3}
                    placeholder="Nhập lý do từ chối (tối thiểu 10 ký tự)…"
                    value={rejectNotes}
                    onChange={(e) => setRejectNotes(e.target.value)}
                  />
                </section>
              )}
            </div>

            {selected.canReview && (
              <div className="pv-modal__foot">
                {rejecting ? (
                  <>
                    <button
                      className="tcs-btn tcs-btn--ghost"
                      type="button"
                      disabled={mutationStatus === 'loading'}
                      onClick={() => {
                        setRejecting(false);
                        setFormError('');
                      }}
                    >
                      Quay lại
                    </button>
                    <button
                      className="tcs-btn tcs-btn--danger"
                      type="button"
                      disabled={mutationStatus === 'loading'}
                      onClick={handleReject}
                    >
                      {mutationStatus === 'loading' ? 'Đang xử lý…' : 'Xác nhận từ chối'}
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      className="tcs-btn tcs-btn--danger"
                      type="button"
                      disabled={mutationStatus === 'loading' || detailLoading}
                      onClick={() => {
                        setRejecting(true);
                        setFormError('');
                      }}
                    >
                      Từ chối
                    </button>
                    <button
                      className="tcs-btn tcs-btn--success"
                      type="button"
                      disabled={mutationStatus === 'loading' || detailLoading}
                      onClick={handleApprove}
                    >
                      {mutationStatus === 'loading' ? 'Đang xử lý…' : 'Duyệt'}
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
