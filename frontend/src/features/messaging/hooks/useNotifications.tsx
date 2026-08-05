import { useCallback, useEffect, useState } from 'react';
import { notificationsApi, type NotificationItem } from '../api/notificationsApi';

export function useNotifications(enabled: boolean) {
  const [items, setItems] = useState<NotificationItem[]>([]);

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

  const markRead = useCallback(async (id: number) => {
    setItems((prev) => prev.map((i) => (i.notificationId === id ? { ...i, isRead: true } : i)));
    try {
      await notificationsApi.markRead(id);
    } catch {
    }
  }, []);

  return { items, unread, reload, markRead };
}
