import { useCallback, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { buildUpdateStatusPayload, buildReviewVerificationPayload } from '../mappers/platformMapper';
import type {
  AdminDisputeReviewApiResponse,
  DisputeStatus,
  ExecuteSettlementApiRequest,
  UserStatus,
} from '../types/platformTypes';

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
      setErrorMessage(getApiErrorMessage(error, 'Không thể cập nhật trạng thái người dùng.'));
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

export function useReviewVerification() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const review = useCallback(
    async (
      verificationId: string,
      decision: 'VERIFIED' | 'REJECTED',
      adminNotes?: string,
      expectedUpdatedAt?: string,
    ) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        await platformApi.reviewVerification(
          verificationId,
          buildReviewVerificationPayload(decision, adminNotes, expectedUpdatedAt),
        );
        setStatus('success');
        return true;
      } catch (error) {
        console.error('Lỗi duyệt xác minh:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể cập nhật xác minh.'));
        setStatus('error');
        return false;
      }
    },
    [],
  );

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, review, reset };
}

export function useResolveDispute() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const resolveDispute = useCallback(
    async (
      disputeId: string,
      nextStatus: Exclude<DisputeStatus, 'OPEN'>,
      resolution: string,
    ): Promise<AdminDisputeReviewApiResponse | null> => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        const response = await platformApi.resolveDispute(disputeId, {
          status: nextStatus,
          resolution: resolution.trim(),
        });
        setStatus('success');
        return response.data;
      } catch (error) {
        console.error('Lỗi xử lý tranh chấp:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể lưu quyết định xử lý tranh chấp.'));
        setStatus('error');
        return null;
      }
    },
    [],
  );

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, resolveDispute, reset };
}

export function useExecuteSettlement() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const executeSettlement = useCallback(async (payload: ExecuteSettlementApiRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await platformApi.executeSettlement(payload);
      setStatus('success');
      return response.data;
    } catch (error) {
      console.error('Lỗi giải ngân escrow:', error);
      setErrorMessage(getApiErrorMessage(error, 'Không thể giải ngân escrow.'));
      setStatus('error');
      return null;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, executeSettlement, reset };
}
