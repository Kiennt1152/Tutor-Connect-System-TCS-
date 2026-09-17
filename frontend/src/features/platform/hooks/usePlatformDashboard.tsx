import { useCallback, useEffect, useState } from 'react';
import { platformApi } from '../api/platformApi';
import { mapDashboardResponse } from '../mappers/platformMapper';
import type { PlatformDashboard } from '../types/platformTypes';

export type PlatformDashboardStatus = 'loading' | 'success' | 'error';

export function usePlatformDashboard(from?: string, to?: string, granularity: string = 'DAY') {
  const [status, setStatus] = useState<PlatformDashboardStatus>('loading');
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [data, setData] = useState<PlatformDashboard | null>(null);

  const reload = useCallback(() => {
    setData((prev) => {
      if (!prev) {
        setStatus('loading');
      } else {
        setIsRefreshing(true);
      }
      return prev;
    });
    platformApi
      .getDashboard(from, to, granularity)
      .then((response) => {
        setData(mapDashboardResponse(response.data));
        setStatus('success');
        setIsRefreshing(false);
      })
      .catch((error) => {
        console.error('Lỗi tải dashboard:', error);
        setStatus('error');
        setIsRefreshing(false);
      });
  }, [from, to, granularity]);

  useEffect(() => {
    const initialId = window.setTimeout(reload, 0);
    const intervalId = window.setInterval(reload, 60_000);
    return () => {
      window.clearTimeout(initialId);
      window.clearInterval(intervalId);
    };
  }, [reload, from, to, granularity]);

  return { status, data, reload, isRefreshing };
}
