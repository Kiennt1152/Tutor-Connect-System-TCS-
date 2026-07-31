import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { mapRefundRequestItem } from '../mappers/platformMapper';
import type { RefundRequestItem, RefundRequestStatus } from '../types/platformTypes';

export type RefundRequestListStatus = 'loading' | 'success' | 'error';

export function useRefundRequestList(statusFilter?: RefundRequestStatus) {
  const [status, setStatus] = useState<RefundRequestListStatus>('loading');
  const [items, setItems] = useState<RefundRequestItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getRefundRequests(statusFilter)
      .then((response) => {
        setItems(response.data.map(mapRefundRequestItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải yêu cầu hoàn tiền:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách yêu cầu hoàn tiền.'));
        setStatus('error');
      });
  }, [statusFilter]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}
