import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
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
  const [evidenceUrls, setEvidenceUrls] = useState('');
  const [note, setNote] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  const canSubmitEvidence = isEvidenceRequest(notification) && notification.referenceId != null;

  const referenceText = referenceLabel(notification);

  async function handleSubmitEvidence(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    const trimmedEvidence = evidenceUrls.trim();
    if (!trimmedEvidence) {
      setFormError('Vui lòng nhập ít nhất một đường dẫn bằng chứng.');
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
      setEvidenceUrls('');
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
          <label>
            <span>Bằng chứng bổ sung</span>
            <textarea
              value={evidenceUrls}
              onChange={(event) => setEvidenceUrls(event.target.value)}
              placeholder="https://..."
              rows={3}
            />
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
          {formError ? <div className="messaging-alert messaging-alert--error">{formError}</div> : null}
          {formSuccess ? (
            <div className="messaging-alert messaging-alert--success">{formSuccess}</div>
          ) : null}
          <button type="submit" className="messaging-btn messaging-btn--primary" disabled={submitting}>
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
