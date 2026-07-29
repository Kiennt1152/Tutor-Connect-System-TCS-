import { useState, useEffect, useRef } from 'react';
import { Bell } from 'lucide-react';
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
        // The API returns { data: AnnouncementItem[] } or something?
        // Let's check how the previous SystemAnnouncements worked. It used res.data
        const data = Array.isArray(res.data) ? res.data : (res.data?.content || []);
        setAnnouncements(data);
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
        <Bell size={20} />
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
