import { useCallback, useState } from 'react';
import { tutorSearchApi } from '../api/tutorSearchApi';
import { mapTutorSearchItem } from '../mappers/tutorSearchMapper';
import type { TutorSearchItem, TutorSearchParams } from '../types/tutorSearchTypes';

export type TutorSearchStatus = 'idle' | 'loading' | 'success' | 'error';

export function useTutorSearch() {
  const [status, setStatus] = useState<TutorSearchStatus>('idle');
  const [results, setResults] = useState<TutorSearchItem[]>([]);
  const [lastQuery, setLastQuery] = useState('');

  const search = useCallback((params: TutorSearchParams & { label?: string }) => {
    setStatus('loading');
    setLastQuery(params.label ?? params.keyword ?? '');
    tutorSearchApi
      .searchTutors(params)
      .then((response) => {
        setResults(response.data.map(mapTutorSearchItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tìm kiếm gia sư:', error);
        setResults([]);
        setStatus('error');
      });
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setResults([]);
    setLastQuery('');
  }, []);

  return { status, results, lastQuery, search, reset };
};
