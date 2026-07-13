import { useCallback, useEffect, useState } from 'react';
import { openClassesApi } from '../api/openClassesApi';
import { mapOpenClassItem } from '../mappers/openClassMapper';
import type { OpenClassItem } from '../types/openClassTypes';

export type OpenClassesStatus = 'loading' | 'success' | 'error';

export function useOpenClasses() {
  const [status, setStatus] = useState<OpenClassesStatus>('loading');
  const [classes, setClasses] = useState<OpenClassItem[]>([]);

  const reload = useCallback(() => {
    setStatus('loading');
    openClassesApi
      .listOpen()
      .then((response) => {
        setClasses(response.map(mapOpenClassItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải lớp đang mở:', error);
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, classes, reload };
}
