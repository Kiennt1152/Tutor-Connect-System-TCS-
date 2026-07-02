import { useState } from 'react';
import axios from 'axios';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { useVerifications } from '../hooks/useVerifications';
import type {
  VerificationDetail,
  VerificationDocumentType,
  VerificationStatus,
  VerificationType,
} from '../types/platformTypes';
import './PlatformUsersPage.css';
import './PlatformVerificationsPage.css';

const TYPE_LABEL: Record<VerificationType, string> = {
  TUTOR_PROFILE: 'Hồ sơ gia sư',
  TUTOR_CENTER_LICENSE: 'Giấy phép trung tâm',
};

const DOC_LABEL: Record<VerificationDocumentType, string> = {
  ID_CARD: 'CMND/CCCD',
  DEGREE: 'Bằng cấp',
  CERTIFICATE: 'Chứng chỉ',
  LICENSE: 'Giấy phép',
};

const STATUS_LABEL: Record<VerificationStatus, string> = {
  DRAFT: 'Nháp',
  SUBMITTED: 'Chờ duyệt',
  UNDER_REVIEW: 'Đang xét',
  VERIFIED: 'Đã duyệt',
  REJECTED: 'Từ chối',
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatDate(value: string | null): string {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('vi-VN');
}

export default function PlatformVerificationsPage() {
  const { status, items, reload, errorMessage } = useVerifications();

  const [detail, setDetail] = useState<VerificationDetail | null>(null);
  const [openingId, setOpeningId] = useState<number | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [rejectNotes, setRejectNotes] = useState('');
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState('');

  const closeDetail = () => {
    setDetail(null);
    setRejecting(false);
    setRejectNotes('');
    setActionError('');
  };

  const handleOpen = async (verificationId: number) => {
    setOpeningId(verificationId);
    setActionError('');
    try {
      const response = await platformApi.openVerification(verificationId);
      setDetail(response.data);
      setRejecting(false);
      setRejectNotes('');
      void reload(); // trạng thái có thể chuyển SUBMITTED -> UNDER_REVIEW
    } catch (error) {
      window.alert(extractError(error, 'Không mở được yêu cầu xác minh.'));
    } finally {
      setOpeningId(null);
    }
  };

  const handleApprove = async () => {
    if (!detail) return;
    if (detail.hasUnreadableDocument) {
      const proceed = window.confirm(
        'Có tài liệu bị thiếu/không đọc được. Bạn vẫn muốn DUYỆT?',
      );
      if (!proceed) return;
    } else if (!window.confirm('Duyệt hồ sơ này?')) {
      return;
    }
    setActionBusy(true);
    setActionError('');
    try {
      await platformApi.reviewVerification(detail.verificationId, { status: 'VERIFIED' });
      closeDetail();
      void reload();
    } catch (error) {
      setActionError(extractError(error, 'Không duyệt được yêu cầu.'));
    } finally {
      setActionBusy(false);
    }
  };

  const handleReject = async () => {
    if (!detail) return;
    if (rejectNotes.trim().length < 10) {
      setActionError('Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).');
      return;
    }
    setActionBusy(true);
    setActionError('');
    try {
      await platformApi.reviewVerification(detail.verificationId, {
        status: 'REJECTED',
        adminNotes: rejectNotes.trim(),
      });
      closeDetail();
      void reload();
    } catch (error) {
      setActionError(extractError(error, 'Không từ chối được yêu cầu.'));
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <AdminLayout
      title="Duyệt hồ sơ xác minh"
      subtitle="Xem xét và phê duyệt/từ chối hồ sơ gia sư và giấy phép trung tâm."
    >
      <div className="adm-card">
        <div className="adm-toolbar">
          <span className="adm-muted">Chỉ hiển thị hồ sơ đang chờ xử lý (Chờ duyệt / Đang xét).</span>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
        </div>

        {status === 'loading' && <div className="adm-state">Đang tải danh sách…</div>}
        {status === 'error' && (
          <div className="adm-state">
            <p>{errorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
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
                  <th>Người nộp</th>
                  <th>Loại</th>
                  <th>Trạng thái</th>
                  <th>Ngày nộp</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6}>Không có hồ sơ nào đang chờ duyệt.</td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.verificationId}>
                      <td>{item.verificationId}</td>
                      <td>
                        <div className="adm-submitter">
                          <span className="adm-submitter__name">{item.submitterName ?? '—'}</span>
                          <span className="adm-submitter__email">{item.userEmail}</span>
                        </div>
                      </td>
                      <td>{TYPE_LABEL[item.verificationType]}</td>
                      <td className="adm-table__badge">
                        <span className={`adm-vbadge adm-vbadge--${item.status.toLowerCase()}`}>
                          {STATUS_LABEL[item.status]}
                        </span>
                      </td>
                      <td>{formatDate(item.submittedAt)}</td>
                      <td className="adm-table__actions">
                        <button
                          className="tcs-btn tcs-btn--primary tcs-btn--sm"
                          type="button"
                          disabled={openingId === item.verificationId}
                          onClick={() => handleOpen(item.verificationId)}
                        >
                          {openingId === item.verificationId ? 'Đang mở…' : 'Xem & duyệt'}
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

      {detail && (
        <div className="adm-modal" role="dialog" aria-modal="true">
          <div className="adm-modal__backdrop" onClick={closeDetail} />
          <div className="adm-modal__card">
            <div className="adm-modal__head">
              <div>
                <h2 className="adm-modal__title">
                  {detail.submitterName ?? detail.userEmail}
                </h2>
                <p className="adm-modal__sub">
                  {TYPE_LABEL[detail.verificationType]} ·{' '}
                  <span className={`adm-vbadge adm-vbadge--${detail.status.toLowerCase()}`}>
                    {STATUS_LABEL[detail.status]}
                  </span>
                </p>
              </div>
              <button className="adm-modal__close" type="button" onClick={closeDetail} aria-label="Đóng">
                ×
              </button>
            </div>

            <div className="adm-modal__body">
              <section className="adm-section">
                <h3 className="adm-section__title">Thông tin người nộp</h3>
                <div className="adm-kv">
                  <div className="adm-kv__row">
                    <span className="adm-kv__k">Email</span>
                    <span className="adm-kv__v">{detail.userEmail}</span>
                  </div>
                  <div className="adm-kv__row">
                    <span className="adm-kv__k">Số điện thoại</span>
                    <span className="adm-kv__v">{detail.submitterPhone ?? '—'}</span>
                  </div>
                  {Object.entries(detail.submitterDetails).map(([k, v]) => (
                    <div className="adm-kv__row" key={k}>
                      <span className="adm-kv__k">{k}</span>
                      <span className="adm-kv__v">{v}</span>
                    </div>
                  ))}
                </div>
              </section>

              <section className="adm-section">
                <h3 className="adm-section__title">Tài liệu ({detail.documents.length})</h3>
                {detail.documents.length === 0 ? (
                  <p className="adm-muted">Không có tài liệu đính kèm.</p>
                ) : (
                  <ul className="adm-docs">
                    {detail.documents.map((doc) => (
                      <li className="adm-docs__item" key={doc.documentId}>
                        <span className="adm-docs__type">{DOC_LABEL[doc.documentType]}</span>
                        {doc.available && doc.fileUrl ? (
                          <a
                            className="adm-docs__link"
                            href={doc.fileUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            {doc.fileName ?? 'Xem tài liệu'}
                          </a>
                        ) : (
                          <span className="adm-docs__broken">⚠ Thiếu / không đọc được</span>
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

              {actionError && <div className="adm-alert adm-alert--error">{actionError}</div>}

              {rejecting && (
                <section className="adm-section">
                  <h3 className="adm-section__title">Lý do từ chối</h3>
                  <textarea
                    className="adm-textarea"
                    rows={3}
                    placeholder="Nhập lý do từ chối (tối thiểu 10 ký tự)…"
                    value={rejectNotes}
                    onChange={(e) => setRejectNotes(e.target.value)}
                  />
                </section>
              )}
            </div>

            <div className="adm-modal__foot">
              {rejecting ? (
                <>
                  <button
                    className="tcs-btn tcs-btn--ghost"
                    type="button"
                    disabled={actionBusy}
                    onClick={() => {
                      setRejecting(false);
                      setActionError('');
                    }}
                  >
                    Quay lại
                  </button>
                  <button
                    className="tcs-btn tcs-btn--danger"
                    type="button"
                    disabled={actionBusy}
                    onClick={handleReject}
                  >
                    {actionBusy ? 'Đang xử lý…' : 'Xác nhận từ chối'}
                  </button>
                </>
              ) : (
                <>
                  <button
                    className="tcs-btn tcs-btn--danger"
                    type="button"
                    disabled={actionBusy}
                    onClick={() => {
                      setRejecting(true);
                      setActionError('');
                    }}
                  >
                    Từ chối
                  </button>
                  <button
                    className="tcs-btn tcs-btn--success"
                    type="button"
                    disabled={actionBusy}
                    onClick={handleApprove}
                  >
                    {actionBusy ? 'Đang xử lý…' : 'Duyệt'}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
