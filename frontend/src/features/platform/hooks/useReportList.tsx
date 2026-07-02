import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { platformApi } from '../api/platformApi';
import { mapReportItem } from '../mappers/platformMapper';
import type { ReportItem } from '../types/platformTypes';

export type ListStatus = 'loading' | 'success' | 'error';

export function useReportList() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<ReportItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getReports()
      .then((response) => {
        setItems(response.data.map(mapReportItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải báo cáo:', error);
        const apiMessage =
          axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
            ? error.response.data.message
            : null;
        setErrorMessage(apiMessage ?? 'Không thể tải danh sách báo cáo.');
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}
