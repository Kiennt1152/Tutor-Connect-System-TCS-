import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useGuardianApprovals, formatApprovalAction, statusLabel } from '../hooks/useGuardianApprovals';
import './DependentProfileLinkerPage.css';

function formatDate(value?: string) {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN');
}

export default function GuardianApprovalPage() {
  const { status, errorMessage, approvals, actionStatus, reload, approve, reject } =
    useGuardianApprovals('pending');

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="dpl-main">
        <div className="tcs-container">
          <header className="dpl-page-header">
            <h1>Xác nhận phụ huynh</h1>
            <p>Phê duyệt các yêu cầu thanh toán và hợp đồng từ học sinh liên kết.</p>
          </header>

          {/* Điều hướng chéo trong khu vực hồ sơ phụ huynh (thay cho ClientLayout cũ). */}
          <nav className="dpl-actions">
            <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to={APP_ROUTES.profile}>
              Về hồ sơ
            </Link>
            <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to={APP_ROUTES.profileDependents}>
              Liên kết hồ sơ
            </Link>
          </nav>

          <div className="dpl-content">
            {status === 'loading' && <div className="dpl-state">Đang tải yêu cầu…</div>}

            {status === 'error' && (
              <div className="dpl-card">
                <div className="dpl-alert dpl-alert--error">{errorMessage}</div>
                <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
                  Thử lại
                </button>
              </div>
            )}

            {status === 'success' && (
              <div className="dpl-card">
                <h2 className="dpl-section-title">Yêu cầu chờ xác nhận</h2>
                {approvals.length === 0 ? (
                  <p className="dpl-muted">Không có yêu cầu nào đang chờ xác nhận.</p>
                ) : (
                  <ul className="dpl-child-list">
                    {approvals.map((approval) => (
                      <li key={approval.approvalId} className="dpl-child-item dpl-approval-item">
                        <div>
                          <strong>{formatApprovalAction(approval)}</strong>
                          <span className="dpl-child-item__meta"> · {approval.minorName}</span>
                          <p className="dpl-muted">{approval.description}</p>
                          <p className="dpl-muted">Gửi lúc {formatDate(approval.createdAt)}</p>
                        </div>
                        <div className="dpl-approval-item__actions">
                          <button
                            className="tcs-btn tcs-btn--primary"
                            type="button"
                            disabled={actionStatus === 'loading'}
                            onClick={() => approve(approval.approvalId)}
                          >
                            Xác nhận
                          </button>
                          <button
                            className="tcs-btn tcs-btn--ghost"
                            type="button"
                            disabled={actionStatus === 'loading'}
                            onClick={() => reject(approval.approvalId)}
                          >
                            Từ chối
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

export function SubmittedApprovalsSection() {
  const { status, approvals } = useGuardianApprovals('submitted');

  if (status !== 'success' || approvals.length === 0) return null;

  return (
    <div className="dpl-card">
      <h2 className="dpl-section-title">Yêu cầu đã gửi</h2>
      <ul className="dpl-child-list">
        {approvals.slice(0, 5).map((approval) => (
          <li key={approval.approvalId} className="dpl-child-item">
            <div>
              <strong>{formatApprovalAction(approval)}</strong>
              <span className="dpl-child-item__meta"> · {statusLabel(approval.status)}</span>
            </div>
            <span className="dpl-muted">{formatDate(approval.createdAt)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
