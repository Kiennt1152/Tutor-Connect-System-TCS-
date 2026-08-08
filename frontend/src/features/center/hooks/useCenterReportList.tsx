import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { centerApi } from '../api/centerApi';
import { mapReportItem } from '../../platform/mappers/platformMapper';
import type { ReportItem } from '../../platform/types/platformTypes';

export type CenterReportListStatus = 'loading' | 'success' | 'error';

export function useCenterReportList() {
  const [status, setStatus] = useState<CenterReportListStatus>('loading');
  const [items, setItems] = useState<ReportItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    centerApi
      .getReports()
      .then((response) => {
        setItems(response.data.map(mapReportItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải báo cáo trung tâm:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách báo cáo của trung tâm.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}
