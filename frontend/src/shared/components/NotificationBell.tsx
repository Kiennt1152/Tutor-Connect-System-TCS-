import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../../features/messaging/hooks/useNotifications';
import { notificationLink } from '../../features/messaging/notificationLink';
import { useAuth } from '../auth/AuthProvider';
import { platformApi } from '../../features/platform/api/platformApi';
import type { NotificationItem } from '../../features/messaging/api/notificationsApi';
import type { AnnouncementApiResponse } from '../../features/platform/types/platformTypes';
import './NotificationBell.css';

function timeAgo(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const mins = Math.floor((Date.now() - then) / 60000);
  if (mins < 1) return 'vừa xong';
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

export function NotificationBell({ enabled = false }: { readonly enabled?: boolean }) {
  const { items, unread, markRead } = useNotifications(enabled);
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [announcements, setAnnouncements] = useState<AnnouncementApiResponse[]>([]);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    platformApi
      .getPublicAnnouncements()
      .then((res) => setAnnouncements(res.data))
      .catch((err) => console.error('Failed to load announcements:', err));
  }, []);

  const historyReadCount = items.filter((n) => n.isRead).length;
  const visible = showRead ? items : items.filter((n) => !n.isRead);

  function togglePanel() {
    if (!open) {
      setShowRead(false);
    }
    setOpen((v) => !v);
  }

  function handleItemClick(n: NotificationItem) {
    if (!n.isRead) void markRead(n.notificationId);
    const link = notificationLink(n, user?.role);
    setOpen(false);
    if (link) navigate(link);
  }

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open]);

  const hasDot = unread === 0 && announcements.length > 0;

  return (
    <div className="ntf" ref={ref}>
      <button
        type="button"
        className="ntf__btn"
        aria-label={`Thông báo${unread > 0 ? ` (${unread} chưa đọc)` : ''}`}
        onClick={togglePanel}
      >
        <svg className="ntf__glyph" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 2.25a6.15 6.15 0 0 0-6.15 6.15v2.76c0 1.85-.63 3.64-1.79 5.08l-.72.9A1.15 1.15 0 0 0 4.24 19h15.52a1.15 1.15 0 0 0 .9-1.86l-.72-.9a8.13 8.13 0 0 1-1.79-5.08V8.4A6.15 6.15 0 0 0 12 2.25Zm0 20a2.75 2.75 0 0 0 2.58-1.8H9.42A2.75 2.75 0 0 0 12 22.25Z"
          />
        </svg>
        {unread > 0 && <span className="ntf__badge">{unread > 9 ? '9+' : unread}</span>}
        {hasDot && <span className="ntf__dot" />}
      </button>

      {open && (
        <div className="ntf__panel" role="menu">
          {enabled && (
            <>
              <div className="ntf__head">Thông báo</div>
              {visible.length === 0 && announcements.length === 0 ? (
                <div className="ntf__empty">
                  {showRead ? 'Chưa có thông báo nào.' : 'Không có thông báo mới.'}
                </div>
              ) : (
                <ul className="ntf__list">
                  {visible.map((n) => {
                    const link = notificationLink(n, user?.role);
                    return (
                      <li key={n.notificationId} className="ntf__item-row">
                        <button
                          type="button"
                          className={`ntf__item ${n.isRead ? '' : 'ntf__item--unread'} ${
                            link ? 'ntf__item--link' : ''
                          }`}
                          onClick={() => handleItemClick(n)}
                        >
                          <div className="ntf__item-title">{n.title}</div>
                          <div className="ntf__item-content">{n.content}</div>
                          <div className="ntf__item-time">{timeAgo(n.createdAt)}</div>
                        </button>
                      </li>
                    );
                  })}
                  {announcements.map((ann) => (
                    <li key={`ann-${ann.announcementId}`} className="ntf__item-row">
                      <div className="ntf__item">
                        <div className="ntf__item-title">{ann.title}</div>
                        <div className="ntf__item-content">{ann.content}</div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              {historyReadCount > 0 && (
                <button type="button" className="ntf__toggle" onClick={() => setShowRead((v) => !v)}>
                  {showRead ? 'Ẩn thông báo đã đọc' : `Xem thông báo đã đọc (${historyReadCount})`}
                </button>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
