import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
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
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách xác minh.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}
