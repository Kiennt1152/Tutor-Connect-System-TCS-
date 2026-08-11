import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { messagingApi } from '../../features/messaging/api/messagingApi';
import { APP_ROUTES } from '../constants/routes';
import './MessageIcon.css';

const REFRESH_INTERVAL_MS = 60_000;

export function MessageIcon() {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let active = true;

    const reload = async () => {
      try {
        const conversations = await messagingApi.getConversations();
        if (!active) return;

        setUnreadCount(
          conversations.reduce(
            (total, conversation) => total + Math.max(0, conversation.unreadCount || 0),
            0,
          ),
        );
      } catch {
        // Keep the last known count when messaging is temporarily unavailable.
      }
    };

    void reload();
    const timer = window.setInterval(() => void reload(), REFRESH_INTERVAL_MS);
    window.addEventListener('focus', reload);

    return () => {
      active = false;
      window.clearInterval(timer);
      window.removeEventListener('focus', reload);
    };
  }, []);

  const label = `Tin nhắn${unreadCount > 0 ? ` (${unreadCount} chưa đọc)` : ''}`;

  return (
    <Link
      to={APP_ROUTES.messaging}
      className="msg-icon"
      aria-label={label}
      title="Tin nhắn"
    >
      <svg
        className="msg-icon__glyph"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          fill="currentColor"
          d="M12 2C6.48 2 2 6.15 2 11.27c0 2.92 1.46 5.52 3.74 7.22V22l3.42-1.88c.9.25 1.86.38 2.84.38 5.52 0 10-4.15 10-9.23S17.52 2 12 2Z"
        />
        <path
          fill="#fff"
          d="m6.72 14.38 3.9-4.14 2.05 1.79 4.32-2.37-3.9 4.14-2.05-1.79-4.32 2.37Z"
        />
      </svg>
      {unreadCount > 0 ? (
        <span className="msg-icon__badge" aria-hidden="true">
          {unreadCount > 9 ? '9+' : unreadCount}
        </span>
      ) : null}
    </Link>
  );
}
