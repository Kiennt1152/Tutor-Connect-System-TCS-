import { useCallback, useEffect, useState } from 'react';
import { platformApi } from '../api/platformApi';
import { mapDashboardResponse } from '../mappers/platformMapper';
import type { PlatformDashboard } from '../types/platformTypes';

export type PlatformDashboardStatus = 'loading' | 'success' | 'error';

export function usePlatformDashboard() {
  const [status, setStatus] = useState<PlatformDashboardStatus>('loading');
  const [data, setData] = useState<PlatformDashboard | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    platformApi
      .getDashboard()
      .then((response) => {
        setData(mapDashboardResponse(response.data));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải dashboard:', error);
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, data, reload };
}
