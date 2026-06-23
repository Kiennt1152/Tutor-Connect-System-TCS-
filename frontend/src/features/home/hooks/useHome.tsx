import { useCallback, useEffect, useState } from 'react';
import { homeApi } from '../api/homeApi';
import { mapHomeResponse } from '../mappers/homeMapper';
import type { HomeData } from '../types/homeTypes';

export type HomeStatus = 'loading' | 'success' | 'error';

export function useHome() {
  const [status, setStatus] = useState<HomeStatus>('loading');
  const [data, setData] = useState<HomeData | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    homeApi
      .getHomeData()
      .then((response) => {
        setData(mapHomeResponse(response));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải trang chủ:', error);
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, data, reload };
}
