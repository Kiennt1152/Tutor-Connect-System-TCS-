import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { platformApi } from '../api/platformApi';
import type { VerificationListItem } from '../types/platformTypes';
import type { LoadStatus } from './useUserList';

export function useVerifications() {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [items, setItems] = useState<VerificationListItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await platformApi.getVerifications();
      setItems(response.data);
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải danh sách xác minh:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không tải được danh sách yêu cầu xác minh.');
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { status, items, reload, errorMessage };
}
