import { useState, useEffect, useRef } from 'react';
import { platformApi } from '../../features/platform/api/platformApi';
import type { AnnouncementItem } from '../../features/platform/types/platformTypes';
import './NotificationBell.css';

export function NotificationBell() {
  const [announcements, setAnnouncements] = useState<AnnouncementItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    platformApi.getPublicAnnouncements()
      .then((res) => {
        setAnnouncements(res.data);
      })
      .catch((err) => console.error('Failed to load announcements:', err));
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const hasUnread = announcements.length > 0;

  return (
    <div className="tcs-notification-wrapper" ref={wrapperRef}>
      <button 
        className="tcs-notification-btn" 
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Thông báo"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
        {hasUnread && <span className="tcs-notification-dot"></span>}
      </button>

      {isOpen && (
        <div className="tcs-notification-dropdown">
          <div className="tcs-notification-header">
            Thông báo hệ thống
          </div>
          <div className="tcs-notification-list">
            {announcements.length === 0 ? (
              <div className="tcs-notification-empty">
                Không có thông báo nào.
              </div>
            ) : (
              announcements.map((ann) => (
                <div key={ann.announcementId} className="tcs-notification-item">
                  <div className="tcs-notification-item-title">{ann.title}</div>
                  <div className="tcs-notification-item-content">{ann.content}</div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
