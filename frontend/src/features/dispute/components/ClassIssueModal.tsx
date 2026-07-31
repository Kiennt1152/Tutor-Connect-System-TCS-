import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { disputeApi } from '../api/disputeApi';
import type {
  ClassIssueRequestedAction,
  ClassIssueType,
  DisputeResponse,
  ReportCategory,
} from '../types/disputeTypes';
import './ClassIssueModal.css';

type ClassIssueModalProps = {
  open: boolean;
  classId: number;
  classTitle?: string | null;
  assignmentId?: number | null;
  classStudentId?: number | null;
  onClose: () => void;
};

const ISSUE_TYPE_LABELS: Record<ClassIssueType, string> = {
  TUTOR_ABSENT: 'Gia sư vắng mặt',
  CLIENT_ABSENT: 'Học viên/phụ huynh vắng mặt',
  TECHNICAL_ISSUE: 'Sự cố kỹ thuật',
  INAPPROPRIATE_BEHAVIOR: 'Hành vi không phù hợp',
  SCHEDULE_CONFLICT: 'Xung đột lịch học',
  QUALITY_ISSUE: 'Chất lượng buổi học',
  PAYMENT_OR_REFUND: 'Thanh toán/hoàn tiền',
  OTHER: 'Khác',
};

const REQUESTED_ACTION_LABELS: Record<ClassIssueRequestedAction, string> = {
  CONTINUE_CLASS: 'Tiếp tục lớp',
  RESCHEDULE: 'Dời lịch/bù buổi',
  REPLACE_TUTOR: 'Đổi gia sư',
  REFUND_REVIEW: 'Xem xét hoàn tiền',
  ESCALATE_DISPUTE: 'Chuyển thành tranh chấp',
  TERMINATE_CLASS: 'Đề nghị chấm dứt lớp',
  OTHER: 'Khác',
};

function categoryForIssueType(issueType: ClassIssueType): ReportCategory {
  if (issueType === 'PAYMENT_OR_REFUND') return 'FRAUD';
  if (issueType === 'INAPPROPRIATE_BEHAVIOR') return 'ABUSE';
  return 'SPAM';
}

export function ClassIssueModal({
  open,
  classId,
  classTitle,
  assignmentId,
  classStudentId,
  onClose,
}: ClassIssueModalProps) {
  const [issueType, setIssueType] = useState<ClassIssueType>('TUTOR_ABSENT');
  const [lessonRef, setLessonRef] = useState('');
  const [occurredAt, setOccurredAt] = useState('');
  const [requestedAction, setRequestedAction] = useState<ClassIssueRequestedAction>('RESCHEDULE');
  const [description, setDescription] = useState('');
  const [evidenceUrls, setEvidenceUrls] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<DisputeResponse | null>(null);

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting) return;
    setIssueType('TUTOR_ABSENT');
    setLessonRef('');
    setOccurredAt('');
    setRequestedAction('RESCHEDULE');
    setDescription('');
    setEvidenceUrls('');
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleSubmit = async () => {
    setError('');
    if (description.trim().length < 20) {
      setError('Vui lòng mô tả sự cố tối thiểu 20 ký tự.');
      return;
    }
    if (occurredAt && new Date(occurredAt) > new Date()) {
      setError('Ngày xảy ra sự cố không được ở tương lai.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await disputeApi.createClassIssue({
        classId,
        issueType,
        category: categoryForIssueType(issueType),
        lessonRef: lessonRef.trim() || undefined,
        occurredAt: occurredAt || undefined,
        requestedAction,
        description: description.trim(),
        evidenceUrls: evidenceUrls.trim() || undefined,
        assignmentId: assignmentId ?? undefined,
        classStudentId: classStudentId ?? undefined,
      });
      setSuccess(result);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi báo cáo sự cố.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="issue-modal-overlay" role="presentation" onClick={resetAndClose}>
      <div
        className="issue-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="issue-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="issue-modal__header">
          <div>
            <p className="issue-modal__eyebrow">Báo cáo sự cố lớp học</p>
            <h2 id="issue-modal-title">Gửi báo cáo sự cố</h2>
            <p className="issue-modal__subtitle">
              {classTitle?.trim() || `Lớp #${classId}`}
            </p>
          </div>
          <button className="issue-modal__close" type="button" onClick={resetAndClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <div className="issue-modal__body">
          {success ? (
            <div className="issue-success">
              <p className="issue-success__title">Đã gửi báo cáo</p>
              {success.escalatedToDispute && success.disputeId ? (
                <p>
                  Mã báo cáo #{success.reportId}, mã tranh chấp #{success.disputeId}. Escrow liên quan
                  đã được chuyển sang trạng thái {success.escrowStatus}.
                </p>
              ) : (
                <p>
                  Mã báo cáo #{success.reportId}. Admin đã nhận ticket và sẽ xử lý theo luồng báo cáo sự cố.
                </p>
              )}
            </div>
          ) : (
            <>
              <label className="issue-field">
                <span>Loại sự cố</span>
                <select
                  value={issueType}
                  onChange={(event) => setIssueType(event.target.value as ClassIssueType)}
                >
                  {Object.entries(ISSUE_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              <div className="issue-field-grid">
                <label className="issue-field">
                  <span>Buổi/ngày liên quan</span>
                  <input
                    value={lessonRef}
                    onChange={(event) => setLessonRef(event.target.value)}
                    placeholder="Ví dụ: Buổi 3, tối thứ Hai"
                  />
                </label>

                <label className="issue-field">
                  <span>Ngày xảy ra</span>
                  <input
                    type="date"
                    value={occurredAt}
                    onChange={(event) => setOccurredAt(event.target.value)}
                  />
                </label>
              </div>

              <label className="issue-field">
                <span>Hướng xử lý mong muốn</span>
                <select
                  value={requestedAction}
                  onChange={(event) => setRequestedAction(event.target.value as ClassIssueRequestedAction)}
                >
                  {Object.entries(REQUESTED_ACTION_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="issue-field">
                <span>Mô tả chi tiết</span>
                <textarea
                  rows={5}
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  placeholder="Mô tả diễn biến, ai liên quan, ảnh hưởng tới buổi học và mong muốn xử lý..."
                />
              </label>

              <label className="issue-field">
                <span>Bằng chứng</span>
                <textarea
                  rows={3}
                  value={evidenceUrls}
                  onChange={(event) => setEvidenceUrls(event.target.value)}
                  placeholder="Dán link ảnh, tài liệu hoặc video. Có thể nhập nhiều link, mỗi link một dòng."
                />
              </label>
            </>
          )}

          {error && <p className="issue-error">{error}</p>}
        </div>

        <div className="issue-modal__footer">
          <button className="btn btn-secondary" type="button" onClick={resetAndClose} disabled={submitting}>
            {success ? 'Đóng' : 'Hủy'}
          </button>
          {!success && (
            <button className="btn btn-primary" type="button" onClick={handleSubmit} disabled={submitting}>
              {submitting ? 'Đang gửi...' : 'Gửi báo cáo'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
