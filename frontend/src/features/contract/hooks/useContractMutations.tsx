import { useCallback, useState } from 'react';
import axios from 'axios';
import { contractApi } from '../api/contractApi';
import { buildSignWithOtpPayload, mapContract } from '../mappers/contractMapper';
import type { Contract, OtpSendResult } from '../types/contractTypes';

export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export function useSendOtp() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [otpInfo, setOtpInfo] = useState<OtpSendResult | null>(null);

  const sendOtp = useCallback(async (contractId: number) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await contractApi.sendOtp(contractId);
      setOtpInfo(response.data);
      setStatus('success');
      return true;
    } catch (error) {
      console.error('Lỗi gửi OTP:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không thể gửi mã OTP. Vui lòng thử lại.');
      setStatus('error');
      return false;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
    setOtpInfo(null);
  }, []);

  return { status, errorMessage, otpInfo, sendOtp, reset };
}

export function useSignWithOtp() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const signWithOtp = useCallback(async (
    contractId: number,
    otpCode: string,
    onSuccess: (contract: Contract) => void,
  ) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await contractApi.signWithOtp(
        contractId,
        buildSignWithOtpPayload(otpCode),
      );
      setStatus('success');
      onSuccess(mapContract(response.data));
      return true;
    } catch (error) {
      console.error('Lỗi ký hợp đồng:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không thể ký hợp đồng. Vui lòng thử lại.');
      setStatus('error');
      return false;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, signWithOtp, reset };
}
