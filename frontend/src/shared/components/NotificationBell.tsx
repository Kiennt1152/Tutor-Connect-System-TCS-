import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../../features/messaging/hooks/useNotifications';
import { notificationLink } from '../../features/messaging/notificationLink';
import { useAuth } from '../auth/AuthProvider';
import type { NotificationItem } from '../../features/messaging/api/notificationsApi';
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

export function NotificationBell({ enabled }: { readonly enabled: boolean }) {
  const { items, unread, markRead } = useNotifications(enabled);
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [openedIds, setOpenedIds] = useState<number[]>([]);
  const ref = useRef<HTMLDivElement>(null);

  const historyReadCount = items.filter(
    (n) => n.isRead && !openedIds.includes(n.notificationId),
  ).length;
  const visible = showRead
    ? items
    : items.filter((n) => !n.isRead || openedIds.includes(n.notificationId));

  function togglePanel() {
    if (!open) {
      const unreadIds = items.filter((n) => !n.isRead).map((n) => n.notificationId);
      setOpenedIds(unreadIds);
      setShowRead(false);
      unreadIds.forEach((id) => void markRead(id));
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

  if (!enabled) return null;

  return (
    <div className="ntf" ref={ref}>
      <button
        type="button"
        className="ntf__btn"
        aria-label={`Thông báo${unread > 0 ? ` (${unread} chưa đọc)` : ''}`}
        onClick={togglePanel}
      >
        🔔
        {unread > 0 && <span className="ntf__badge">{unread > 9 ? '9+' : unread}</span>}
      </button>

      {open && (
        <div className="ntf__panel" role="menu">
          <div className="ntf__head">Thông báo</div>
          {visible.length === 0 ? (
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
            </ul>
          )}
          {historyReadCount > 0 && (
            <button type="button" className="ntf__toggle" onClick={() => setShowRead((v) => !v)}>
              {showRead ? 'Ẩn thông báo đã đọc' : `Xem thông báo đã đọc (${historyReadCount})`}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
