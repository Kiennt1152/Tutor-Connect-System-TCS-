import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { profileApi } from '../api/profileApi';
import { extractApiErrorMessage } from '../hooks/useDependentProfile';
import type { GuardianApproval } from '../types/guardianApprovalTypes';

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useGuardianApprovals(mode: 'pending' | 'submitted') {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [approvals, setApprovals] = useState<GuardianApproval[]>([]);
  const [actionStatus, setActionStatus] = useState<LoadStatus>('idle');

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const res =
        mode === 'pending'
          ? await profileApi.getPendingGuardianApprovals()
          : await profileApi.getSubmittedGuardianApprovals();
      setApprovals(res.data);
      setStatus('success');
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, 'Không tải được danh sách yêu cầu.'));
      setStatus('error');
    }
  }, [mode]);

  useEffect(() => {
    reload();
  }, [reload]);

  const approve = useCallback(
    async (approvalId: number) => {
      setActionStatus('loading');
      try {
        await profileApi.approveGuardianRequest(approvalId);
        await reload();
        setActionStatus('success');
        return true;
      } catch (error) {
        setErrorMessage(extractApiErrorMessage(error, 'Không thể xác nhận yêu cầu.'));
        setActionStatus('error');
        return false;
      }
    },
    [reload],
  );

  const reject = useCallback(
    async (approvalId: number) => {
      setActionStatus('loading');
      try {
        await profileApi.rejectGuardianRequest(approvalId);
        await reload();
        setActionStatus('success');
        return true;
      } catch (error) {
        setErrorMessage(extractApiErrorMessage(error, 'Không thể từ chối yêu cầu.'));
        setActionStatus('error');
        return false;
      }
    },
    [reload],
  );

  return { status, errorMessage, approvals, actionStatus, reload, approve, reject };
}

export function formatApprovalAction(approval: GuardianApproval) {
  if (approval.actionType === 'DEPOSIT') {
    return `Nạp tiền ${approval.amount?.toLocaleString('vi-VN')} VND`;
  }
  const base = `Ký hợp đồng với gia sư ${approval.tutorName}`;
  return approval.subjectName ? `${base} (${approval.subjectName})` : base;
}

export function statusLabel(status: GuardianApproval['status']) {
  switch (status) {
    case 'PENDING':
      return 'Chờ phụ huynh xác nhận';
    case 'APPROVED':
      return 'Đã xác nhận';
    case 'REJECTED':
      return 'Đã từ chối';
    default:
      return status;
  }
}

export function isForbidden(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 403;
}
