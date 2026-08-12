import { type ChangeEvent, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import { disputeApi } from '../api/disputeApi';
import type {
  ClassIssueRequestedAction,
  ClassIssueType,
  DisputeResponse,
  EvidenceUploadResponse,
  ReportCategory,
} from '../types/disputeTypes';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  type BankOption,
} from '../../finance/components/BankPicker';
import './ClassIssueModal.css';

type ClassIssueModalProps = {
  open: boolean;
  classId: number;
  classTitle?: string | null;
  assignmentId?: number | null;
  classStudentId?: number | null;
  currentUserRole?: string | null;
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

const MAX_EVIDENCE_FILES = 5;
const MAX_EVIDENCE_SIZE = 10 * 1024 * 1024;
const EVIDENCE_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function categoryForIssueType(issueType: ClassIssueType): ReportCategory {
  if (issueType === 'PAYMENT_OR_REFUND') return 'FRAUD';
  if (issueType === 'INAPPROPRIATE_BEHAVIOR') return 'ABUSE';
  return 'SPAM';
}

function buildEvidenceUrls(files: EvidenceUploadResponse[]) {
  return files.map((file) => file.fileUrl).join('\n');
}

function needsRefundPayoutInfo(
  issueType: ClassIssueType,
  requestedAction: ClassIssueRequestedAction,
  currentUserRole?: string | null,
) {
  if (currentUserRole !== 'CLIENT') {
    return false;
  }
  return issueType === 'PAYMENT_OR_REFUND'
    || requestedAction === 'REFUND_REVIEW'
    || requestedAction === 'TERMINATE_CLASS';
}

export function ClassIssueModal({
  open,
  classId,
  classTitle,
  assignmentId,
  classStudentId,
  currentUserRole,
  onClose,
}: ClassIssueModalProps) {
  const isClient = currentUserRole === 'CLIENT';
  const [issueType, setIssueType] = useState<ClassIssueType>('TUTOR_ABSENT');
  const [lessonRef, setLessonRef] = useState('');
  const [occurredAt, setOccurredAt] = useState('');
  const [requestedAction, setRequestedAction] = useState<ClassIssueRequestedAction>('RESCHEDULE');
  const [description, setDescription] = useState('');
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [evidenceFiles, setEvidenceFiles] = useState<EvidenceUploadResponse[]>([]);
  const [uploadingEvidence, setUploadingEvidence] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState<DisputeResponse | null>(null);
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);
  const showRefundPayoutInfo = needsRefundPayoutInfo(issueType, requestedAction, currentUserRole);
  const availableIssueTypes = Object.entries(ISSUE_TYPE_LABELS).filter(([value]) => {
    if (isClient) return true;
    return value !== 'PAYMENT_OR_REFUND';
  }) as Array<[ClassIssueType, string]>;
  const availableRequestedActions = Object.entries(REQUESTED_ACTION_LABELS).filter(([value]) => {
    if (isClient) return true;
    return value !== 'REFUND_REVIEW';
  }) as Array<[ClassIssueRequestedAction, string]>;

  if (!open) return null;

  const resetAndClose = () => {
    if (submitting || uploadingEvidence) return;
    setIssueType('TUTOR_ABSENT');
    setLessonRef('');
    setOccurredAt('');
    setRequestedAction('RESCHEDULE');
    setDescription('');
    setSelectedBankCode('');
    setBankPickerOpen(false);
    setAccountNo('');
    setAccountHolderName('');
    setEvidenceFiles([]);
    setError('');
    setSuccess(null);
    onClose();
  };

  const handleEvidenceFilesChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.currentTarget.value = '';
    if (files.length === 0) return;

    setError('');
    if (evidenceFiles.length + files.length > MAX_EVIDENCE_FILES) {
      setError(`Mỗi báo cáo chỉ nên đính kèm tối đa ${MAX_EVIDENCE_FILES} ảnh.`);
      return;
    }

    const invalidFile = files.find((file) => !EVIDENCE_IMAGE_TYPES.has(file.type));
    if (invalidFile) {
      setError(`"${invalidFile.name}" không đúng định dạng. Vui lòng chọn ảnh JPG, PNG hoặc WEBP.`);
      return;
    }

    const oversizedFile = files.find((file) => file.size > MAX_EVIDENCE_SIZE);
    if (oversizedFile) {
      setError(`"${oversizedFile.name}" vượt quá 10MB.`);
      return;
    }

    setUploadingEvidence(true);
    try {
      const uploadedFiles = await Promise.all(files.map((file) => disputeApi.uploadEvidenceImage(file)));
      setEvidenceFiles((current) => [...current, ...uploadedFiles]);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tải ảnh bằng chứng. Vui lòng thử lại.'));
    } finally {
      setUploadingEvidence(false);
    }
  };

  const removeEvidenceFile = (fileId: number) => {
    setEvidenceFiles((current) => current.filter((file) => file.fileId !== fileId));
  };

  const handleSelectBank = (bank: BankOption) => {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
  };

  const handleSubmit = async () => {
    setError('');
    if (uploadingEvidence) {
      setError('Vui lòng chờ tải ảnh bằng chứng xong trước khi gửi báo cáo.');
      return;
    }
    if (description.trim().length < 20) {
      setError('Vui lòng mô tả sự cố tối thiểu 20 ký tự.');
      return;
    }
    if (occurredAt && new Date(occurredAt) > new Date()) {
      setError('Ngày xảy ra sự cố không được ở tương lai.');
      return;
    }
    const normalizedAccountNo = accountNo.trim().replace(/\s+/g, '');
    if (showRefundPayoutInfo) {
      if (!selectedBank) {
        setError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
        return;
      }
      if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
        setError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
        return;
      }
      if (accountHolderName.trim().length < 2) {
        setError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
        return;
      }
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
        evidenceUrls: buildEvidenceUrls(evidenceFiles) || undefined,
        assignmentId: assignmentId ?? undefined,
        classStudentId: classStudentId ?? undefined,
        refundPayoutInfo: showRefundPayoutInfo && selectedBank
          ? {
              bankName: selectedBank.name,
              accountNo: normalizedAccountNo,
              accountHolderName: accountHolderName.trim().replace(/\s+/g, ' '),
            }
          : undefined,
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
                  đã được chuyển sang trạng thái {success.escrowStatus}. Admin sẽ thấy hồ sơ trong mục
                  Tranh chấp và Báo cáo sự cố lớp.
                </p>
              ) : (
                <p>
                  Mã báo cáo #{success.reportId}. Admin sẽ thấy ticket trong mục Báo cáo sự cố lớp.
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
                  {availableIssueTypes.map(([value, label]) => (
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
                  {availableRequestedActions.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              {showRefundPayoutInfo ? (
                <div className="issue-payout">
                  <p className="issue-payout__title">Tài khoản nhận hoàn tiền</p>
                  <div className="issue-field">
                    <span>Ngân hàng</span>
                    <BankSelectField
                      id="issue-refund-bank-field"
                      selectedBank={selectedBank}
                      onOpen={() => setBankPickerOpen(true)}
                    />
                  </div>
                  <div className="issue-field-grid">
                    <label className="issue-field">
                      <span>Số tài khoản</span>
                      <input
                        value={accountNo}
                        onChange={(event) => setAccountNo(event.target.value)}
                        placeholder="Nhập số tài khoản"
                      />
                    </label>
                    <label className="issue-field">
                      <span>Tên chủ tài khoản</span>
                      <input
                        value={accountHolderName}
                        onChange={(event) => setAccountHolderName(event.target.value)}
                        placeholder="Nhập tên chủ tài khoản"
                      />
                    </label>
                  </div>
                </div>
              ) : null}

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
                <span className="issue-upload">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    multiple
                    disabled={uploadingEvidence || submitting}
                    onChange={handleEvidenceFilesChange}
                  />
                  <strong>{uploadingEvidence ? 'Đang tải ảnh...' : 'Chọn ảnh bằng chứng'}</strong>
                  <small>JPG, PNG hoặc WEBP, tối đa 10MB/ảnh.</small>
                </span>
              </label>

              {evidenceFiles.length > 0 && (
                <div className="issue-evidence-list" aria-label="Ảnh bằng chứng đã tải lên">
                  {evidenceFiles.map((file) => (
                    <FileThumbnail
                      key={file.fileId}
                      src={file.fileUrl}
                      fileName={file.fileName}
                      mimeType={file.mimeType}
                      fileSize={file.fileSize}
                      actions={
                        <button
                          className="issue-evidence-remove"
                          type="button"
                          disabled={submitting || uploadingEvidence}
                          onClick={() => removeEvidenceFile(file.fileId)}
                        >
                          Xóa
                        </button>
                      }
                    />
                  ))}
                </div>
              )}
            </>
          )}

          {error && <p className="issue-error">{error}</p>}
        </div>

        <div className="issue-modal__footer">
          <button className="btn btn-secondary" type="button" onClick={resetAndClose} disabled={submitting}>
            {success ? 'Đóng' : 'Hủy'}
          </button>
          {!success && (
            <button
              className="btn btn-primary"
              type="button"
              onClick={handleSubmit}
              disabled={submitting || uploadingEvidence}
            >
              {submitting ? 'Đang gửi...' : 'Gửi báo cáo'}
            </button>
          )}
        </div>
        <BankPickerDialog
          open={bankPickerOpen}
          selectedBankCode={selectedBankCode}
          onSelect={handleSelectBank}
          onClose={() => setBankPickerOpen(false)}
        />
      </div>
    </div>
  );
}
