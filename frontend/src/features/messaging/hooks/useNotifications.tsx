import { useCallback, useEffect, useState } from 'react';
import { notificationsApi, type NotificationItem } from '../api/notificationsApi';

/** Hook thông báo của người đăng nhập: danh sách, số chưa đọc, tải lại và đánh dấu đã đọc. */
export function useNotifications(enabled: boolean) {
  const [items, setItems] = useState<NotificationItem[]>([]);

  /** Tải danh sách thông báo (chỉ khi đã bật). */
  const reload = useCallback(() => {
    if (!enabled) return;
    notificationsApi
      .list()
      .then(setItems)
      .catch(() => {
      });
  }, [enabled]);

  useEffect(() => {
    reload();
    if (!enabled) return;
    const timer = setInterval(reload, 60000);
    return () => clearInterval(timer);
  }, [reload, enabled]);

  const unread = items.filter((i) => !i.isRead).length;

  /** Đánh dấu đã đọc ngay trên giao diện rồi mới gọi API (lỗi thì bỏ qua). */
  const markRead = useCallback(async (id: number) => {
    setItems((prev) => prev.map((i) => (i.notificationId === id ? { ...i, isRead: true } : i)));
    try {
      await notificationsApi.markRead(id);
    } catch {
    }
  }, []);

  return { items, unread, reload, markRead };
}
