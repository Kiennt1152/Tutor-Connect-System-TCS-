import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { disputeApi } from '../api/disputeApi';
import type { DisputeResponse, ReportCategory } from '../types/disputeTypes';
import './ClassIssueModal.css';

type ClassIssueModalProps = {
  open: boolean;
  classId: number;
  classTitle?: string | null;
  onClose: () => void;
};

const CATEGORY_LABELS: Record<ReportCategory, string> = {
  FRAUD: 'Gian lận / sai lệch thông tin',
  ABUSE: 'Hành vi không phù hợp',
  SPAM: 'Nội dung rác hoặc vấn đề khác',
};

export function ClassIssueModal({ open, classId, classTitle, onClose }: ClassIssueModalProps) {
  const [category, setCategory] = useState<ReportCategory>('ABUSE');
  const [description, setDescription] = useState('');
  const [evidenceUrls, setEvidenceUrls] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<DisputeResponse | null>(null);

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting) return;
    setCategory('ABUSE');
    setDescription('');
    setEvidenceUrls('');
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleSubmit = async () => {
    setError('');
    if (description.trim().length < 10) {
      setError('Vui lòng mô tả vấn đề tối thiểu 10 ký tự.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await disputeApi.createClassIssue({
        classId,
        category,
        description: description.trim(),
        evidenceUrls: evidenceUrls.trim() || undefined,
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
            <h2 id="issue-modal-title">Tạo tranh chấp</h2>
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
              <p>
                Mã tranh chấp #{success.disputeId}, mã báo cáo #{success.reportId}. Escrow liên quan
                đã được chuyển sang trạng thái {success.escrowStatus}.
              </p>
            </div>
          ) : (
            <>
              <label className="issue-field">
                <span>Loại vấn đề</span>
                <select
                  value={category}
                  onChange={(event) => setCategory(event.target.value as ReportCategory)}
                >
                  {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
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
                  placeholder="Mô tả sự cố, thời điểm xảy ra và mong muốn xử lý..."
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
