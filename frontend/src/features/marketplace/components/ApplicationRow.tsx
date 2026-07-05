import { StatusBadge } from './StatusBadge';
import type { TutorApplication } from '../types/marketplaceTypes';

type Props = {
  application: TutorApplication;
  busy?: boolean;
  /** Reviewer = class owner: hiện Accept / Reject */
  showReviewActions?: boolean;
  /** Applicant = tutor: hiện Withdraw */
  showWithdraw?: boolean;
  onAccept?: (application: TutorApplication) => void;
  onReject?: (application: TutorApplication) => void;
  onWithdraw?: (application: TutorApplication) => void;
};

function formatRate(value?: number | null) {
  if (value == null) return '—';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}

function formatDate(value?: string | null) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('vi-VN');
  } catch {
    return value;
  }
}

export function ApplicationRow({
  application,
  busy,
  showReviewActions,
  showWithdraw,
  onAccept,
  onReject,
  onWithdraw,
}: Props) {
  const canReview =
    showReviewActions &&
    (application.status === 'SUBMITTED' || application.status === 'UNDER_REVIEW');
  const canWithdraw = showWithdraw && application.status === 'SUBMITTED';

  return (
    <div className="mp-app-row">
      <div className="mp-app-row__main">
        <div className="mp-app-row__name">
          {application.tutorName}
          <StatusBadge status={application.status} />
        </div>
        <div className="mp-app-row__meta">
          <span>Lớp: {application.classTitle}</span>
          {application.proposedRate != null && (
            <span>Đề xuất: {formatRate(application.proposedRate)}</span>
          )}
          <span>Nộp: {formatDate(application.appliedAt)}</span>
          {application.reviewedAt && (
            <span>Duyệt: {formatDate(application.reviewedAt)}</span>
          )}
        </div>
        {application.coverLetter && (
          <div className="mp-app-row__cover">“{application.coverLetter}”</div>
        )}
      </div>
      <div className="mp-app-row__actions">
        {canReview && (
          <>
            <button
              type="button"
              className="mp-btn mp-btn--primary"
              disabled={busy}
              onClick={() => onAccept?.(application)}
            >
              Chấp nhận
            </button>
            <button
              type="button"
              className="mp-btn mp-btn--ghost"
              disabled={busy}
              onClick={() => onReject?.(application)}
            >
              Từ chối
            </button>
          </>
        )}
        {canWithdraw && (
          <button
            type="button"
            className="mp-btn mp-btn--ghost"
            disabled={busy}
            onClick={() => onWithdraw?.(application)}
          >
            Rút đơn
          </button>
        )}
      </div>
    </div>
  );
}