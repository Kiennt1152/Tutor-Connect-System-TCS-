import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { mapPageAdminWithdrawalList } from '../mappers/platformMapper';
import type { PageAdminWithdrawalList, WithdrawalListFilters } from '../types/platformTypes';

export type WithdrawalListStatus = 'idle' | 'loading' | 'success' | 'error';

export function useWithdrawalList(initialFilters: WithdrawalListFilters) {
  const [filters, setFilters] = useState<WithdrawalListFilters>(initialFilters);
  const [status, setStatus] = useState<WithdrawalListStatus>('idle');
  const [data, setData] = useState<PageAdminWithdrawalList | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await platformApi.getWithdrawals(filters);
      setData(mapPageAdminWithdrawalList(response.data));
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải yêu cầu rút tiền:', error);
      setErrorMessage(getApiErrorMessage(error, 'Không tải được danh sách yêu cầu rút tiền.'));
      setStatus('error');
    }
  }, [filters]);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { status, data, filters, setFilters, reload, errorMessage };
}
