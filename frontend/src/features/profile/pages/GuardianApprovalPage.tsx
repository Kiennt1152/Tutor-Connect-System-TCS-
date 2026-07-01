import { ClientLayout } from '../components/ClientLayout';
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
    <ClientLayout
      title="Xác nhận phụ huynh"
      subtitle="Phê duyệt các yêu cầu thanh toán và hợp đồng từ học sinh liên kết."
    >
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
    </ClientLayout>
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
