import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { CccdSection } from '../../profile/components/CccdSection';
import { SiteFooter } from '../../home/components/SiteFooter';
import { profileApi } from '../../profile/api/profileApi';
import { useVerification } from '../hooks/useVerification';
import { verificationApi } from '../api/verificationApi';
import { mapDocumentType, mapVerificationStatus } from '../mappers/verificationMapper';
import {
  getSlotsForRole,
  getVerificationTypeForRole,
} from '../types/verificationTypes';
import type {
  DocumentSlotConfig,
  DocumentUpload,
  Verification,
  VerificationDocumentType,
} from '../types/verificationTypes';
import './VerificationPage.css';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

const ACCEPTED_EXTS = '.pdf,.jpg,.jpeg,.png,.webp';

interface UploadedFile {
  fileId: number;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  mimeType: string;
  slotKey: DocumentSlotConfig['key'];
  documentType: VerificationDocumentType;
}

type SubmissionStep = 'upload' | 'done';

interface UploadViewProps {
  readonly slots: readonly DocumentSlotConfig[];
  readonly uploadedFiles: UploadedFile[];
  readonly uploadingSlot: DocumentSlotConfig['key'] | null;
  readonly uploadError: string | null;
  readonly dragSlot: DocumentSlotConfig['key'] | null;
  readonly isSubmitting: boolean;
  readonly submitError: string | null;
  readonly onFileUpload: (slot: DocumentSlotConfig, file: File) => void;
  readonly onRemoveFile: (
    slot: DocumentSlotConfig['key'],
    fileId: number,
  ) => void;
  readonly onSubmit: () => void;
  readonly onDragEnter: (slot: DocumentSlotConfig['key']) => void;
  readonly onDragLeave: () => void;
}

interface SlotFieldProps {
  readonly slot: DocumentSlotConfig;
  readonly uploadedFiles: UploadedFile[];
  readonly isUploading: boolean;
  readonly isDragActive: boolean;
  readonly onFileUpload: UploadViewProps['onFileUpload'];
  readonly onRemoveFile: UploadViewProps['onRemoveFile'];
  readonly onDragEnter: UploadViewProps['onDragEnter'];
  readonly onDragLeave: UploadViewProps['onDragLeave'];
}

interface DoneViewProps {
  readonly latest: Verification | null | undefined;
  readonly hasActiveRequest: boolean;
  readonly onStartNew: () => void;
}

interface HistoryProps {
  readonly verifications: Verification[];
  readonly status: LoadStatus;
  readonly onCancel: (verificationId: number) => void;
}

interface StatusPalette {
  readonly bg: string;
  readonly color: string;
}

const STATUS_PALETTE: Record<string, StatusPalette> = {
  success: { bg: '#e8f5e9', color: '#2e7d32' },
  warn: { bg: '#fff8e1', color: '#ef6c00' },
  info: { bg: '#e3f2fd', color: '#1565c0' },
  danger: { bg: '#ffebee', color: '#c62828' },
};

const DEFAULT_PALETTE: StatusPalette = { bg: '#e3f2fd', color: '#1565c0' };

function getPalette(variant: string): StatusPalette {
  return STATUS_PALETTE[variant] ?? DEFAULT_PALETTE;
}

export default function VerificationPage() {
  const { user } = useAuth();
  // Thông báo khi bị điều hướng tới đây (VD: bấm ứng tuyển nhưng chưa xác minh).
  const location = useLocation();
  const redirectNotice =
    typeof (location.state as { notice?: string } | null)?.notice === 'string'
      ? (location.state as { notice: string }).notice
      : null;
  const userId = user?.userId ?? 0;
  const role = user?.role ?? 'CLIENT';
  const isCenter = role === 'TUTOR_CENTER';
  const slots = useMemo(() => getSlotsForRole(role), [role]);
  const verificationType = getVerificationTypeForRole(role);
  const {
    status,
    verifications,
    error,
    isSubmitting,
    reload,
    submitVerification,
    cancelVerification,
  } = useVerification(userId);
  const [step, setStep] = useState<SubmissionStep>('upload');
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [uploadingSlot, setUploadingSlot] =
    useState<DocumentSlotConfig['key'] | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [dragSlot, setDragSlot] = useState<DocumentSlotConfig['key'] | null>(
    null,
  );
  // Bắt buộc tự xác nhận thông tin CCCD quét được là đúng trước khi gửi xác minh.
  const [cccdConfirmed, setCccdConfirmed] = useState(false);

  const latest = verifications[0] ?? null;

  const hasActiveRequest = useMemo(
    () =>
      verifications.some(
        (v) =>
          v.status === 'SUBMITTED' ||
          v.status === 'UNDER_REVIEW' ||
          v.status === 'VERIFIED',
      ),
    [verifications],
  );

  // Đồng bộ step với trạng thái thực của backend khi load lần đầu:
  // - Đã có active request từ DB -> DoneView (không hiện form upload)
  // - Chưa có gì -> upload form
  // Sau khi user đã tương tác trong session (submit hoặc cancel), step được
  // điều khiển bởi local state để tránh xóa mất uploadedFiles giữa chừng.
  const [hasInteracted, setHasInteracted] = useState(false);
  useEffect(() => {
    if (status !== 'success' || hasInteracted) {
      return;
    }
    setStep(hasActiveRequest ? 'done' : 'upload');
  }, [hasActiveRequest, hasInteracted, status]);

  // Khi user vừa cancel xong (active request biến mất trong session) -> quay lại upload
  useEffect(() => {
    if (!hasInteracted || status !== 'success') {
      return;
    }
    if (!hasActiveRequest && step === 'done') {
      setStep('upload');
      setUploadedFiles([]);
      setSubmitError(null);
    }
  }, [hasActiveRequest, hasInteracted, step, status]);

  async function handleFileUpload(slot: DocumentSlotConfig, file: File) {
    setUploadError(null);
    setUploadingSlot(slot.key);
    try {
      const result = await verificationApi.uploadFile(userId, file);
      setUploadedFiles((prev) => [
        ...prev,
        {
          fileId: result.fileId,
          fileName: result.fileName,
          fileUrl: result.fileUrl,
          fileSize: result.fileSize,
          mimeType: result.mimeType,
          slotKey: slot.key,
          documentType: slot.documentType,
        },
      ]);
    } catch (err) {
      setUploadError(
        err instanceof Error ? err.message : 'Tải tệp thất bại. Vui lòng thử lại.',
      );
    } finally {
      setUploadingSlot(null);
    }
  }

  function removeFile(slot: DocumentSlotConfig['key'], fileId: number) {
    setUploadedFiles((prev) =>
      prev.filter((f) => !(f.slotKey === slot && f.fileId === fileId)),
    );
  }

  const missingRequired = useMemo(
    () =>
      slots.filter(
        (s) =>
          s.required &&
          !uploadedFiles.some((f) => f.slotKey === s.key),
      ),
    [slots, uploadedFiles],
  );

  const requiredComplete = missingRequired.length === 0;

  async function handleSubmit() {
    if (!requiredComplete) {
      setSubmitError(
        `Vui lòng tải lên: ${missingRequired.map((s) => s.label).join(', ')}.`,
      );
      return;
    }

    // Bắt buộc đã quét + lưu thông tin CCCD (đọc QR) trước khi gửi xác minh.
    let cccdComplete = false;
    try {
      cccdComplete = Boolean((await profileApi.getMyCccd()).data.complete);
    } catch {
      cccdComplete = false;
    }
    if (!cccdComplete) {
      setSubmitError(
        'Vui lòng quét mã QR CCCD và bấm "Lưu thông tin CCCD" ở mục CCCD phía trên trước khi gửi xác minh.',
      );
      return;
    }
    if (!cccdConfirmed) {
      setSubmitError('Vui lòng tích xác nhận thông tin CCCD quét được là chính xác trước khi gửi.');
      return;
    }

    setSubmitError(null);
    try {
      const documents: DocumentUpload[] = uploadedFiles.map((f) => ({
        documentType: f.documentType,
        fileId: f.fileId,
      }));

      await submitVerification({
        verificationType,
        documents,
      });
      setStep('done');
      setUploadedFiles([]);
      setHasInteracted(true);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        const msg = (err.response.data as { message?: string } | undefined)?.message;
        setSubmitError(
          msg ?? 'Bạn đang có một yêu cầu xác minh đang xử lý. Vui lòng hủy yêu cầu cũ trước khi gửi yêu cầu mới.',
        );
      } else {
        setSubmitError(
          err instanceof Error
            ? err.message
            : 'Gửi yêu cầu thất bại. Vui lòng thử lại.',
        );
      }
    }
  }

  function startResubmit() {
    setStep('upload');
    setUploadedFiles([]);
    setSubmitError(null);
    setHasInteracted(true);
  }

  return (
    <div className="tcs-page verification-page">
      <VerificationHeader />
      <main>
        <div className="tcs-container verification-page__container">
          <header className="verification-page__header">
            <span className="verification-page__eyebrow">Hồ sơ / Xác minh</span>
            <h1 className="verification-page__title">
              {isCenter ? 'Xác minh trung tâm gia sư' : 'Xác minh gia sư'}
            </h1>
            <p className="verification-page__subtitle">
              {isCenter
                ? 'Nộp các chứng từ pháp lý bắt buộc để xác minh trung tâm và nhận huy hiệu đã xác minh theo quy định Việt Nam.'
                : 'Nộp các giấy tờ tùy thân và bằng cấp của bạn để nhận huy hiệu đã xác minh và tạo niềm tin với phụ huynh, học sinh.'}
            </p>
          </header>

          {redirectNotice && (
            <div className="verification-alert verification-alert--info">{redirectNotice}</div>
          )}
          {error && (
            <div className="verification-alert verification-alert--error">{error}</div>
          )}

          <div className="verification-layout">
            <div className="verification-card">
              <div className="verification-card__head">
                <h2 className="verification-card__title">
                  {step === 'done' ? 'Đã gửi yêu cầu xác minh' : 'Nộp giấy tờ'}
                </h2>
                {step === 'done' && (
                  <button
                    className="verification-btn verification-btn--ghost"
                    type="button"
                    onClick={() => void reload()}
                  >
                    Làm mới
                  </button>
                )}
              </div>

              <div className="verification-card__body">
                {step === 'done' ? (
                  <DoneView
                    latest={latest}
                    hasActiveRequest={hasActiveRequest}
                    onStartNew={startResubmit}
                  />
                ) : (
                  <>
                    {/* Quét CCCD (đọc QR tự điền) — bắt buộc trước khi gửi xác minh. */}
                    <CccdSection
                      title={
                        isCenter
                          ? 'Thông tin CCCD (người đại diện pháp luật)'
                          : 'Thông tin CCCD (dùng khi ký hợp đồng)'
                      }
                    />
                    <label
                      style={{
                        display: 'flex',
                        gap: 8,
                        alignItems: 'flex-start',
                        margin: '4px 0 16px',
                        fontSize: 14,
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={cccdConfirmed}
                        onChange={(e) => setCccdConfirmed(e.target.checked)}
                        style={{ marginTop: 3 }}
                      />
                      <span>
                        Tôi đã kiểm tra và <strong>xác nhận thông tin CCCD</strong> quét được ở trên là
                        chính xác.
                      </span>
                    </label>
                    <UploadView
                    slots={slots}
                    uploadedFiles={uploadedFiles}
                    uploadingSlot={uploadingSlot}
                    uploadError={uploadError}
                    dragSlot={dragSlot}
                    isSubmitting={isSubmitting}
                    submitError={submitError}
                    onFileUpload={(slot, file) => void handleFileUpload(slot, file)}
                    onRemoveFile={(slot, fileId) => removeFile(slot, fileId)}
                    onSubmit={() => void handleSubmit()}
                    onDragEnter={(slot) => setDragSlot(slot)}
                    onDragLeave={() => setDragSlot(null)}
                  />
                  </>
                )}

                {step === 'done' && latest?.status === 'REJECTED' && !hasActiveRequest && (
                  <button
                    className="verification-btn verification-btn--primary"
                    type="button"
                    style={{ marginTop: 16 }}
                    onClick={startResubmit}
                  >
                    Gửi lại yêu cầu xác minh
                  </button>
                )}
              </div>
            </div>

            <div className="verification-card">
              <div className="verification-card__head">
                <h2 className="verification-card__title">Lịch sử xác minh</h2>
              </div>
              <div className="verification-card__body">
                <VerificationHistory
                  verifications={verifications}
                  status={status}
                  onCancel={async (id) => {
                    await cancelVerification(id);
                    setHasInteracted(true);
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function DoneView({ latest, hasActiveRequest, onStartNew }: DoneViewProps) {
  if (!latest) {
    return <div className="verification-empty">Chưa có yêu cầu xác minh nào.</div>;
  }

  const statusInfo = mapVerificationStatus(latest.status);
  const palette = getPalette(statusInfo.variant);
  const submittedLabel = latest.submittedAt
    ? new Date(latest.submittedAt).toLocaleString()
    : '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div className="verification-alert verification-alert--info">
        Yêu cầu xác minh của bạn đã được gửi và đang chờ quản trị viên xét duyệt.
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <span
          className="verification-badge"
          style={{ background: palette.bg, color: palette.color }}
        >
          {statusInfo.label}
        </span>
        <span style={{ fontSize: 13, color: '#666' }}>{submittedLabel}</span>
      </div>
      {latest.rejectionReason && (
        <div className="verification-alert verification-alert--error">
          <strong>Lý do từ chối:</strong> {latest.rejectionReason}
        </div>
      )}
      <p style={{ fontSize: 14, color: '#555', margin: 0 }}>
        Bạn sẽ nhận được thông báo ngay khi quản trị viên hoàn tất xét duyệt hồ sơ.
      </p>
      {!hasActiveRequest && (
        <button
          className="verification-btn verification-btn--primary"
          type="button"
          style={{ alignSelf: 'flex-start' }}
          onClick={onStartNew}
        >
          Gửi yêu cầu xác minh mới
        </button>
      )}
    </div>
  );
}

function UploadView({
  slots,
  uploadedFiles,
  uploadingSlot,
  uploadError,
  dragSlot,
  isSubmitting,
  submitError,
  onFileUpload,
  onRemoveFile,
  onSubmit,
  onDragEnter,
  onDragLeave,
}: UploadViewProps) {
  return (
    <div className="verification-form">
      {slots.map((slot) => (
        <SlotField
          key={slot.key}
          slot={slot}
          uploadedFiles={uploadedFiles.filter((f) => f.slotKey === slot.key)}
          isUploading={uploadingSlot === slot.key}
          isDragActive={dragSlot === slot.key}
          onFileUpload={onFileUpload}
          onRemoveFile={onRemoveFile}
          onDragEnter={onDragEnter}
          onDragLeave={onDragLeave}
        />
      ))}

      {uploadError && (
        <div className="verification-alert verification-alert--error">
          {uploadError}
        </div>
      )}
      {submitError && (
        <div className="verification-alert verification-alert--error">
          {submitError}
        </div>
      )}

      <div
        className="verification-form__footer"
        style={{ display: 'flex', gap: 8 }}
      >
        <button
          className="verification-btn verification-btn--primary"
          type="button"
          disabled={isSubmitting}
          onClick={onSubmit}
        >
          {isSubmitting ? 'Đang gửi…' : 'Gửi xác minh'}
        </button>
      </div>
    </div>
  );
}

const HIDDEN_INPUT_STYLE: React.CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0,0,0,0)',
  whiteSpace: 'nowrap',
  border: 0,
};

function readSingleFile(
  e: React.ChangeEvent<HTMLInputElement>,
  onPick: (file: File) => void,
) {
  const file = e.target.files?.[0];
  if (file) onPick(file);
  e.target.value = '';
}

function SlotField({
  slot,
  uploadedFiles,
  isUploading,
  isDragActive,
  onFileUpload,
  onRemoveFile,
  onDragEnter,
  onDragLeave,
}: SlotFieldProps) {
  const requiredTag = slot.required ? '(bắt buộc)' : '(không bắt buộc)';
  const zoneClass = [
    'verification-upload-zone',
    isDragActive ? 'verification-upload-zone--drag' : '',
  ]
    .filter(Boolean)
    .join(' ');
  const slotInputId = `upload-${slot.key}`;
  const dropLabel = slot.hint;

  function handleDrop(e: React.DragEvent<HTMLLabelElement>) {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) onFileUpload(slot, file);
  }

  return (
    <div className="verification-field">
      <label htmlFor={slotInputId}>
        {slot.label} <span>{requiredTag}</span>
      </label>
      <span style={{ fontSize: 12, color: '#888' }}>{slot.hint}</span>

      {uploadedFiles.length > 0 ? (
        <div
          style={{
            marginTop: 6,
            display: 'flex',
            flexDirection: 'column',
            gap: 6,
          }}
        >
          {uploadedFiles.map((file) => (
            <div key={file.fileId} className="verification-doc-item">
              <FileThumbnail
                src={file.fileUrl}
                fileName={file.fileName}
                mimeType={file.mimeType}
                fileSize={file.fileSize}
                actions={
                  <>
                    <span className="verification-doc-item__type">
                      {mapDocumentType(file.documentType)}
                    </span>
                    <button
                      className="verification-doc-item__remove"
                      type="button"
                      onClick={() => onRemoveFile(slot.key, file.fileId)}
                    >
                      ×
                    </button>
                  </>
                }
              />
            </div>
          ))}
          {slot.multi && (
            <UploadButton slot={slot} onFileUpload={onFileUpload} />
          )}
        </div>
      ) : (
        <label
          htmlFor={slotInputId}
          className={zoneClass}
          style={{ marginTop: 6, cursor: 'pointer' }}
          onDragOver={(e) => e.preventDefault()}
          onDragEnter={() => onDragEnter(slot.key)}
          onDragLeave={onDragLeave}
          onDrop={handleDrop}
          aria-label={dropLabel}
        >
          <input
            id={slotInputId}
            type="file"
            accept={ACCEPTED_EXTS}
            multiple={false}
            onChange={(e) => readSingleFile(e, (f) => onFileUpload(slot, f))}
            style={HIDDEN_INPUT_STYLE}
          />
          {isUploading ? (
            <span style={{ color: '#888' }}>Đang tải lên…</span>
          ) : (
            <>
              <span className="verification-upload-zone__label">
                Nhấn hoặc kéo thả tệp vào đây
              </span>
              <span className="verification-upload-zone__hint">
                PDF, JPG, PNG, WEBP — tối đa 10MB
              </span>
            </>
          )}
        </label>
      )}
    </div>
  );
}

function UploadButton({
  slot,
  onFileUpload,
}: {
  readonly slot: DocumentSlotConfig;
  readonly onFileUpload: (slot: DocumentSlotConfig, file: File) => void;
}) {
  const id = `upload-${slot.key}-add`;
  return (
    <>
      <input
        id={id}
        type="file"
        accept={ACCEPTED_EXTS}
        multiple={false}
        onChange={(e) => readSingleFile(e, (f) => onFileUpload(slot, f))}
        style={HIDDEN_INPUT_STYLE}
      />
      <label
        htmlFor={id}
        className="verification-btn verification-btn--ghost"
        style={{ alignSelf: 'flex-start', cursor: 'pointer' }}
      >
        + Thêm tệp khác
      </label>
    </>
  );
}

function VerificationHistory({ verifications, status, onCancel }: HistoryProps) {
  if (status === 'loading') {
    return <div className="verification-state">Đang tải lịch sử…</div>;
  }

  if (verifications.length === 0) {
    return (
      <div className="verification-empty">
        Chưa có yêu cầu xác minh nào.
      </div>
    );
  }

  return (
    <ul className="verification-history-list">
      {verifications.map((v) => {
        const info = mapVerificationStatus(v.status);
        const palette = getPalette(info.variant);
        const submittedLabel = v.submittedAt
          ? new Date(v.submittedAt).toLocaleString()
          : 'N/A';
        const canCancel = v.status === 'SUBMITTED';
        const typeLabel =
          v.verificationType === 'TUTOR_CENTER_LICENSE'
            ? 'Trung tâm gia sư'
            : 'Gia sư';

        return (
          <li key={v.verificationId} className="verification-history-item">
            <div className="verification-history-item__timeline">
              <div
                className="verification-history-item__dot"
                style={{ background: palette.color }}
              />
            </div>
            <div className="verification-history-item__content">
              <div className="verification-history-item__header">
                <span className="verification-history-item__type">
                  {typeLabel}
                </span>
                <span
                  className="verification-badge"
                  style={{ background: palette.bg, color: palette.color }}
                >
                  {info.label}
                </span>
                {canCancel && (
                  <button
                    type="button"
                    className="verification-history-item__cancel"
                    onClick={() => {
                      if (window.confirm('Hủy yêu cầu xác minh này? Hành động không thể hoàn tác.')) {
                        onCancel(v.verificationId);
                      }
                    }}
                    title="Hủy yêu cầu này"
                    aria-label="Hủy yêu cầu xác minh"
                    style={{ marginLeft: 'auto' }}
                  >
                    ×
                  </button>
                )}
              </div>
              <div className="verification-history-item__date">
                {submittedLabel}
              </div>
              {v.rejectionReason && (
                <div className="verification-history-item__reason">
                  Lý do từ chối: {v.rejectionReason}
                </div>
              )}
              {v.adminNotes && (
                <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                  Ghi chú: {v.adminNotes}
                </div>
              )}
              {v.documents.length > 0 && (
                <div className="verification-history-item__docs">
                  {v.documents.map((doc) => (
                    <FileThumbnail
                      key={doc.documentId}
                      src={doc.fileUrl}
                      fileName={doc.fileName}
                      mimeType={doc.mimeType}
                      fileSize={doc.fileSize}
                    />
                  ))}
                </div>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}