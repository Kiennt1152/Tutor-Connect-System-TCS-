import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { platformApi } from '../api/platformApi';
import { mapPageUserList } from '../mappers/platformMapper';
import type { PageUserList, UserListFilters } from '../types/platformTypes';

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useUserList(initialFilters: UserListFilters) {
  const [filters, setFilters] = useState<UserListFilters>(initialFilters);
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [data, setData] = useState<PageUserList | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await platformApi.getUsers(filters);
      setData(mapPageUserList(response.data));
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải danh sách người dùng:', error);
      if (axios.isAxiosError(error)) {
        if (!error.response) {
          setErrorMessage(
            'Không kết nối được backend. Hãy chạy BackendApplication (port 8080) rồi bấm Thử lại.',
          );
        } else if (error.response.status === 401 || error.response.status === 403) {
          setErrorMessage(
            typeof error.response.data?.message === 'string'
              ? error.response.data.message
              : 'Phiên đăng nhập hết hạn hoặc không có quyền admin.',
          );
        } else {
          const apiMessage =
            typeof error.response.data?.message === 'string' ? error.response.data.message : null;
          setErrorMessage(apiMessage ?? `Lỗi máy chủ (${error.response.status}).`);
        }
      } else {
        setErrorMessage('Không tải được danh sách người dùng.');
      }
      setStatus('error');
    }
  }, [filters]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { status, data, filters, setFilters, reload, errorMessage };
}
