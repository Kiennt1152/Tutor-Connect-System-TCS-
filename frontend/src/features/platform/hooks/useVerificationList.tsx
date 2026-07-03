import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { platformApi } from '../api/platformApi';
import { mapVerificationItem } from '../mappers/platformMapper';
import type { VerificationRequestItem } from '../types/platformTypes';

export type ListStatus = 'loading' | 'success' | 'error';

export function useVerificationList() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<VerificationRequestItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getVerifications()
      .then((response) => {
        setItems(response.data.map(mapVerificationItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải xác minh:', error);
        const apiMessage =
          axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
            ? error.response.data.message
            : null;
        setErrorMessage(apiMessage ?? 'Không thể tải danh sách xác minh.');
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}
