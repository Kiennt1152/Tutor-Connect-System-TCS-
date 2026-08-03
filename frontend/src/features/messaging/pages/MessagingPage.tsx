import { type ChangeEvent, type FormEvent, useEffect, useMemo, useState } from 'react';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import type { EvidenceUploadResponse } from '../../dispute/types/disputeTypes';
import { messagingApi } from '../api/messagingApi';
import { useMessaging } from '../hooks/useMessaging';
import type { NotificationItem, SubmitDisputeEvidenceRequest } from '../types/messagingTypes';
import './MessagingPage.css';

const NOTIFICATION_TYPE_LABELS: Record<NotificationItem['type'], string> = {
  PAYMENT: 'Thanh toán',
  APPLICATION: 'Ứng tuyển',
  SYSTEM: 'Hệ thống',
  CLASS: 'Lớp học',
  VERIFICATION: 'Xác minh',
  CHAT: 'Trao đổi',
};

const MAX_EVIDENCE_FILES = 5;
const MAX_EVIDENCE_SIZE = 10 * 1024 * 1024;
const EVIDENCE_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function formatDateTime(value: string | null) {
  if (!value) {
    return 'Chưa có thời gian';
  }
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value));
}

function isEvidenceRequest(notification: NotificationItem) {
  const source = `${notification.title ?? ''} ${notification.content}`.toLowerCase();
  return notification.referenceType === 'DISPUTE' && source.includes('bằng chứng');
}

function buildEvidenceUrls(files: EvidenceUploadResponse[]) {
  return files.map((file) => file.fileUrl).join('\n');
}

interface NotificationCardProps {
  notification: NotificationItem;
  submitting: boolean;
  onMarkAsRead: (notificationId: number) => Promise<boolean>;
  onSubmitEvidence: (
    disputeId: number,
    payload: SubmitDisputeEvidenceRequest,
  ) => Promise<boolean>;
}

function referenceLabel(notification: NotificationItem) {
  if (!notification.referenceType || !notification.referenceId) {
    return null;
  }
  if (notification.referenceType === 'DISPUTE') {
    return `Tranh chấp #${notification.referenceId}`;
  }
  return `${notification.referenceType} #${notification.referenceId}`;
}

function NotificationCard({
  notification,
  submitting,
  onMarkAsRead,
  onSubmitEvidence,
}: Readonly<NotificationCardProps>) {
  const [evidenceFiles, setEvidenceFiles] = useState<EvidenceUploadResponse[]>([]);
  const [uploadingEvidence, setUploadingEvidence] = useState(false);
  const [note, setNote] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  const canSubmitEvidence = isEvidenceRequest(notification) && notification.referenceId != null;

  const referenceText = referenceLabel(notification);

  async function handleEvidenceFilesChange(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.currentTarget.value = '';
    if (files.length === 0) return;

    setFormError(null);
    setFormSuccess(null);

    if (evidenceFiles.length + files.length > MAX_EVIDENCE_FILES) {
      setFormError(`Mỗi lần bổ sung chỉ nên đính kèm tối đa ${MAX_EVIDENCE_FILES} ảnh.`);
      return;
    }

    const invalidFile = files.find((file) => !EVIDENCE_IMAGE_TYPES.has(file.type));
    if (invalidFile) {
      setFormError(`"${invalidFile.name}" không đúng định dạng. Vui lòng chọn ảnh JPG, PNG hoặc WEBP.`);
      return;
    }

    const oversizedFile = files.find((file) => file.size > MAX_EVIDENCE_SIZE);
    if (oversizedFile) {
      setFormError(`"${oversizedFile.name}" vượt quá 10MB.`);
      return;
    }

    setUploadingEvidence(true);
    try {
      const uploadedFiles = await Promise.all(files.map((file) => messagingApi.uploadEvidenceImage(file)));
      setEvidenceFiles((current) => [...current, ...uploadedFiles]);
    } catch (error) {
      console.error('Lỗi tải ảnh bằng chứng:', error);
      setFormError('Không thể tải ảnh bằng chứng. Vui lòng thử lại.');
    } finally {
      setUploadingEvidence(false);
    }
  }

  function removeEvidenceFile(fileId: number) {
    setEvidenceFiles((current) => current.filter((file) => file.fileId !== fileId));
  }

  async function handleSubmitEvidence(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    const trimmedEvidence = buildEvidenceUrls(evidenceFiles);
    if (!trimmedEvidence) {
      setFormError('Vui lòng tải lên ít nhất một ảnh bằng chứng.');
      return;
    }
    if (uploadingEvidence) {
      setFormError('Vui lòng chờ tải ảnh bằng chứng xong trước khi gửi.');
      return;
    }
    if (notification.referenceId == null) {
      setFormError('Thông báo chưa có mã tranh chấp hợp lệ.');
      return;
    }

    const success = await onSubmitEvidence(notification.referenceId, {
      evidenceUrls: trimmedEvidence,
      note: note.trim() || undefined,
    });
    if (success) {
      setEvidenceFiles([]);
      setNote('');
      setFormSuccess('Đã gửi bằng chứng bổ sung.');
      if (!notification.isRead) {
        await onMarkAsRead(notification.notificationId);
      }
    }
  }

  return (
    <article
      className={`messaging-card${notification.isRead ? '' : ' messaging-card--unread'}`}
    >
      <div className="messaging-card__main">
        <div className="messaging-card__top">
          <span className="messaging-card__type">
            {NOTIFICATION_TYPE_LABELS[notification.type] ?? 'Thông báo'}
          </span>
          {!notification.isRead ? (
            <span className="messaging-card__unread">Chưa đọc</span>
          ) : (
            <span className="messaging-card__read">Đã đọc</span>
          )}
        </div>
        <h2>{notification.title || 'Thông báo mới'}</h2>
        <p>{notification.content}</p>
        <div className="messaging-card__meta">
          <span>{formatDateTime(notification.createdAt)}</span>
          {referenceText ? <span>{referenceText}</span> : null}
        </div>
      </div>

      <div className="messaging-card__actions">
        {!notification.isRead ? (
          <button
            type="button"
            className="messaging-btn messaging-btn--ghost"
            onClick={() => onMarkAsRead(notification.notificationId)}
            disabled={submitting}
          >
            Đánh dấu đã đọc
          </button>
        ) : null}
      </div>

      {canSubmitEvidence ? (
        <form className="messaging-evidence" onSubmit={handleSubmitEvidence}>
          <label className="messaging-evidence__upload">
            <span>Bằng chứng bổ sung</span>
            <span className="messaging-upload">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                disabled={submitting || uploadingEvidence}
                onChange={handleEvidenceFilesChange}
              />
              <strong>{uploadingEvidence ? 'Đang tải ảnh...' : 'Chọn ảnh bằng chứng'}</strong>
              <small>JPG, PNG hoặc WEBP, tối đa 10MB/ảnh.</small>
            </span>
          </label>
          <label>
            <span>Ghi chú</span>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Tóm tắt bằng chứng mới"
              rows={2}
            />
          </label>
          {evidenceFiles.length > 0 ? (
            <div className="messaging-evidence__files" aria-label="Ảnh bằng chứng đã tải lên">
              {evidenceFiles.map((file) => (
                <FileThumbnail
                  key={file.fileId}
                  src={file.fileUrl}
                  fileName={file.fileName}
                  mimeType={file.mimeType}
                  fileSize={file.fileSize}
                  actions={
                    <button
                      className="messaging-evidence__remove"
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
          ) : null}
          {formError ? <div className="messaging-alert messaging-alert--error">{formError}</div> : null}
          {formSuccess ? (
            <div className="messaging-alert messaging-alert--success">{formSuccess}</div>
          ) : null}
          <button
            type="submit"
            className="messaging-btn messaging-btn--primary"
            disabled={submitting || uploadingEvidence}
          >
            {submitting ? 'Đang gửi...' : 'Gửi bằng chứng'}
          </button>
        </form>
      ) : null}
    </article>
  );
}

export default function MessagingPage() {
  const {
    notifications,
    loading,
    errorMessage,
    mutationStatus,
    mutationError,
    fetchNotifications,
    markAsRead,
    submitDisputeEvidence,
  } = useMessaging();

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const unreadCount = useMemo(
    () => notifications.filter((notification) => !notification.isRead).length,
    [notifications],
  );

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="messaging-page">
        <header className="messaging-page__header">
          <div>
            <p className="messaging-page__eyebrow">Trung tâm thông báo</p>
            <h1>Thông báo của tôi</h1>
          </div>
          <button
            type="button"
            className="messaging-btn messaging-btn--ghost"
            onClick={fetchNotifications}
            disabled={loading}
          >
            Làm mới
          </button>
        </header>

        <section className="messaging-summary" aria-label="Tổng quan thông báo">
          <div>
            <span>{notifications.length}</span>
            <p>Tổng thông báo</p>
          </div>
          <div>
            <span>{unreadCount}</span>
            <p>Chưa đọc</p>
          </div>
        </section>

        {errorMessage ? <div className="messaging-alert messaging-alert--error">{errorMessage}</div> : null}
        {mutationError ? (
          <div className="messaging-alert messaging-alert--error">{mutationError}</div>
        ) : null}

        <section className="messaging-list" aria-label="Danh sách thông báo">
          {loading ? <div className="messaging-empty">Đang tải thông báo...</div> : null}
          {!loading && notifications.length === 0 ? (
            <div className="messaging-empty">Chưa có thông báo mới.</div>
          ) : null}
          {!loading
            ? notifications.map((notification) => (
                <NotificationCard
                  key={notification.notificationId}
                  notification={notification}
                  submitting={mutationStatus === 'loading'}
                  onMarkAsRead={markAsRead}
                  onSubmitEvidence={submitDisputeEvidence}
                />
              ))
            : null}
        </section>
      </main>
    </div>
  );
}
