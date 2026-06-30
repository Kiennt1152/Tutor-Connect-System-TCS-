import { useMemo, useState } from 'react';
import { useVerification, formatFileSize } from '../hooks/useVerification';
import { verificationApi } from '../api/verificationApi';
import { mapVerificationStatus } from '../mappers/verificationMapper';
import { DOCUMENT_SLOTS } from '../types/verificationTypes';
import type {
  DocumentSlotConfig,
  DocumentUpload,
  Verification,
  VerificationDocumentType,
} from '../types/verificationTypes';
import './VerificationPage.css';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

const MOCK_USER_ID = 1;
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
}

interface HistoryProps {
  readonly verifications: Verification[];
  readonly status: LoadStatus;
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
  const {
    status,
    verifications,
    error,
    isSubmitting,
    reload,
    submitVerification,
  } = useVerification(MOCK_USER_ID);
  const [step, setStep] = useState<SubmissionStep>('upload');
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [uploadingSlot, setUploadingSlot] =
    useState<DocumentSlotConfig['key'] | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [dragSlot, setDragSlot] = useState<DocumentSlotConfig['key'] | null>(
    null,
  );

  const latest = verifications[0] ?? null;

  async function handleFileUpload(slot: DocumentSlotConfig, file: File) {
    setUploadError(null);
    setUploadingSlot(slot.key);
    try {
      const result = await verificationApi.uploadFile(MOCK_USER_ID, file);
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
        err instanceof Error ? err.message : 'Upload failed. Please try again.',
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
      DOCUMENT_SLOTS.filter(
        (s) =>
          s.required &&
          !uploadedFiles.some((f) => f.slotKey === s.key),
      ),
    [uploadedFiles],
  );

  const requiredComplete = missingRequired.length === 0;

  async function handleSubmit() {
    if (!requiredComplete) {
      setSubmitError(
        `Please upload: ${missingRequired.map((s) => s.label).join(', ')}.`,
      );
      return;
    }

    setSubmitError(null);
    try {
      const documents: DocumentUpload[] = uploadedFiles.map((f) => ({
        documentType: f.documentType,
        fileId: f.fileId,
      }));

      await submitVerification({
        verificationType: 'TUTOR_PROFILE',
        documents,
      });
      setStep('done');
      setUploadedFiles([]);
    } catch (err) {
      setSubmitError(
        err instanceof Error
          ? err.message
          : 'Submission failed. Please try again.',
      );
    }
  }

  function startResubmit() {
    setStep('upload');
    setUploadedFiles([]);
    setSubmitError(null);
  }

  return (
    <section className="verification-page">
      <div className="verification-page__header">
        <span className="verification-page__eyebrow">Identity / Verification</span>
        <h1 className="verification-page__title">Tutor Verification</h1>
        <p className="verification-page__subtitle">
          Submit your credentials to earn a verified badge and build trust with
          clients.
        </p>
      </div>

      {error && (
        <div className="verification-alert verification-alert--error">
          {error}
        </div>
      )}

      <div className="verification-layout">
        <div className="verification-card">
          <div className="verification-card__head">
            <h2 className="verification-card__title">
              {step === 'done' ? 'Verification Submitted' : 'Submit Documents'}
            </h2>
            {step === 'done' && (
              <button
                className="verification-btn verification-btn--ghost"
                type="button"
                onClick={() => void reload()}
              >
                Refresh
              </button>
            )}
          </div>

          <div className="verification-card__body">
            {step === 'done' ? (
              <DoneView latest={latest} />
            ) : (
              <UploadView
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
            )}

            {step === 'done' && latest?.status === 'REJECTED' && (
              <button
                className="verification-btn verification-btn--primary"
                type="button"
                style={{ marginTop: 16 }}
                onClick={startResubmit}
              >
                Resubmit Verification
              </button>
            )}
          </div>
        </div>

        <div className="verification-card">
          <div className="verification-card__head">
            <h2 className="verification-card__title">Verification History</h2>
          </div>
          <div className="verification-card__body">
            <VerificationHistory verifications={verifications} status={status} />
          </div>
        </div>
      </div>
    </section>
  );
}

function DoneView({ latest }: DoneViewProps) {
  if (!latest) {
    return <div className="verification-empty">No verification found.</div>;
  }

  const statusInfo = mapVerificationStatus(latest.status);
  const palette = getPalette(statusInfo.variant);
  const submittedLabel = latest.submittedAt
    ? new Date(latest.submittedAt).toLocaleString()
    : '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div className="verification-alert verification-alert--info">
        Your verification has been submitted and is pending review.
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
          <strong>Rejection reason:</strong> {latest.rejectionReason}
        </div>
      )}
      <p style={{ fontSize: 14, color: '#555', margin: 0 }}>
        You will be notified once the platform admin reviews your documents.
      </p>
    </div>
  );
}

function UploadView({
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
      {DOCUMENT_SLOTS.map((slot) => (
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
          {isSubmitting ? 'Submitting…' : 'Submit for Verification'}
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
  const requiredTag = slot.required ? '(required)' : '(optional)';
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
              <span className="verification-doc-item__icon">📄</span>
              <div className="verification-doc-item__info">
                <div className="verification-doc-item__name">
                  {file.fileName}
                </div>
                <div className="verification-doc-item__meta">
                  {formatFileSize(file.fileSize)}
                </div>
              </div>
              <span className="verification-doc-item__type">
                {slot.documentType}
              </span>
              <button
                className="verification-doc-item__remove"
                type="button"
                onClick={() => onRemoveFile(slot.key, file.fileId)}
              >
                ×
              </button>
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
            <span style={{ color: '#888' }}>Uploading…</span>
          ) : (
            <>
              <span className="verification-upload-zone__label">
                Click or drag file here
              </span>
              <span className="verification-upload-zone__hint">
                PDF, JPG, PNG, WEBP — max 10MB
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
        + Add another
      </label>
    </>
  );
}

function VerificationHistory({ verifications, status }: HistoryProps) {
  if (status === 'loading') {
    return <div className="verification-state">Loading history…</div>;
  }

  if (verifications.length === 0) {
    return (
      <div className="verification-empty">No verification submissions yet.</div>
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
                  {v.verificationType}
                </span>
                <span
                  className="verification-badge"
                  style={{ background: palette.bg, color: palette.color }}
                >
                  {info.label}
                </span>
              </div>
              <div className="verification-history-item__date">
                {submittedLabel}
              </div>
              {v.rejectionReason && (
                <div className="verification-history-item__reason">
                  Reason: {v.rejectionReason}
                </div>
              )}
              {v.adminNotes && (
                <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                  Note: {v.adminNotes}
                </div>
              )}
              <div style={{ fontSize: 12, color: '#888', marginTop: 4 }}>
                {v.documents.length} document(s) submitted
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}