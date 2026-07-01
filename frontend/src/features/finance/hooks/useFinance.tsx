import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { extractApiErrorMessage } from '../../profile/hooks/useDependentProfile';
import { financeApi } from '../api/financeApi';
import type { DepositRequest, PaymentMethodResponse, WalletResponse } from '../types/financeTypes';

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useFinance() {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodResponse[]>([]);
  const [mutationStatus, setMutationStatus] = useState<LoadStatus>('idle');
  const [mutationError, setMutationError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const [walletRes, methodsRes] = await Promise.all([
        financeApi.getWallet(),
        financeApi.getPaymentMethods(),
      ]);
      setWallet(walletRes.data);
      setPaymentMethods(methodsRes.data);
      setStatus('success');
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, 'Không tải được thông tin ví.'));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const deposit = useCallback(
    async (body: DepositRequest) => {
      setMutationStatus('loading');
      setMutationError(null);
      try {
        const res = await financeApi.deposit(body);
        setWallet(res.data);
        await reload();
        setMutationStatus('success');
        return res.data;
      } catch (error) {
        setMutationError(extractApiErrorMessage(error, 'Không thể nạp tiền.'));
        setMutationStatus('error');
        return null;
      }
    },
    [reload],
  );

  return {
    status,
    errorMessage,
    wallet,
    paymentMethods,
    mutationStatus,
    mutationError,
    reload,
    deposit,
  };
}

export function formatCurrency(amount: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

export function isFinanceForbidden(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 403;
}
