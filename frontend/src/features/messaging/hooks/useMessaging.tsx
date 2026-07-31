import { useCallback, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { messagingApi } from '../api/messagingApi';
import type { NotificationItem, SubmitDisputeEvidenceRequest } from '../types/messagingTypes';

export type MessagingMutationStatus = 'idle' | 'loading' | 'success' | 'error';

export function useMessaging() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [mutationStatus, setMutationStatus] = useState<MessagingMutationStatus>('idle');
  const [mutationError, setMutationError] = useState<string | null>(null);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const data = await messagingApi.getNotifications();
      setNotifications(data ?? []);
      return data ?? [];
    } catch (error) {
      console.error('Lỗi tải thông báo:', error);
      setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách thông báo.'));
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  const markAsRead = useCallback(async (notificationId: number) => {
    setMutationStatus('loading');
    setMutationError(null);
    try {
      await messagingApi.markNotificationRead(notificationId);
      setNotifications((current) =>
        current.map((item) =>
          item.notificationId === notificationId ? { ...item, isRead: true } : item,
        ),
      );
      setMutationStatus('success');
      return true;
    } catch (error) {
      console.error('Lỗi đánh dấu thông báo:', error);
      setMutationError(getApiErrorMessage(error, 'Không thể đánh dấu thông báo đã đọc.'));
      setMutationStatus('error');
      return false;
    }
  }, []);

  const submitDisputeEvidence = useCallback(
    async (disputeId: number, payload: SubmitDisputeEvidenceRequest) => {
      setMutationStatus('loading');
      setMutationError(null);
      try {
        await messagingApi.submitDisputeEvidence(disputeId, payload);
        setMutationStatus('success');
        await fetchNotifications();
        return true;
      } catch (error) {
        console.error('Lỗi gửi bằng chứng bổ sung:', error);
        setMutationError(getApiErrorMessage(error, 'Không thể gửi bằng chứng bổ sung.'));
        setMutationStatus('error');
        return false;
      }
    },
    [fetchNotifications],
  );

  const resetMutation = useCallback(() => {
    setMutationStatus('idle');
    setMutationError(null);
  }, []);

  return {
    notifications,
    loading,
    errorMessage,
    mutationStatus,
    mutationError,
    fetchNotifications,
    markAsRead,
    submitDisputeEvidence,
    resetMutation,
  };
}
