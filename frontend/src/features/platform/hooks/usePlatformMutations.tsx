import { useCallback, useState } from 'react';
import axios from 'axios';
import { platformApi } from '../api/platformApi';
import { buildUpdateStatusPayload } from '../mappers/platformMapper';
import type { UserStatus } from '../types/platformTypes';

export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export function useUpdateUserStatus() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const updateStatus = useCallback(async (userId: string, nextStatus: UserStatus) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await platformApi.updateUserStatus(userId, buildUpdateStatusPayload(nextStatus));
      setStatus('success');
      return true;
    } catch (error) {
      console.error('Lỗi cập nhật trạng thái:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không thể cập nhật trạng thái người dùng.');
      setStatus('error');
      return false;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, updateStatus, reset };
}
