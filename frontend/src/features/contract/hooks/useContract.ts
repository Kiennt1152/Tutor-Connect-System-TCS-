import { useCallback, useEffect, useState } from 'react';
import { contractApi } from '../api/contractApi';
import type {
  ContractApiResponse,
  ContractSignatureListApiResponse,
  SendOtpApiResponse,
} from '../types/contractTypes';

interface UseContractListResult {
  contracts: ContractApiResponse[];
  loading: boolean;
  error: string | null;
  reload: () => Promise<void>;
}

interface UseContractDetailResult {
  contract: ContractApiResponse | null;
  signatures: ContractSignatureListApiResponse | null;
  loading: boolean;
  error: string | null;
  reload: (contractId: number) => Promise<void>;
}

interface UseSignContractResult {
  otpSent: SendOtpApiResponse | null;
  signing: boolean;
  sendingOtp: boolean;
  error: string | null;
  sendOtp: (contractId: number) => Promise<SendOtpApiResponse | null>;
  sign: (contractId: number, otp: string) => Promise<ContractApiResponse | null>;
}

export function useContractList(): UseContractListResult {
  const [contracts, setContracts] = useState<ContractApiResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await contractApi.getMyContracts();
      setContracts(data);
    } catch (e) {
      setError(extractMessage(e, 'Không thể tải danh sách hợp đồng'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { contracts, loading, error, reload };
}

export function useContractDetail(): UseContractDetailResult {
  const [contract, setContract] = useState<ContractApiResponse | null>(null);
  const [signatures, setSignatures] = useState<ContractSignatureListApiResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async (contractId: number) => {
    setLoading(true);
    setError(null);
    try {
      const [c, s] = await Promise.all([
        contractApi.getContract(contractId).then(res => res.data),
        contractApi.getSignatures(contractId).then(res => res.data),
      ]);
      setContract(c);
      setSignatures(s);
    } catch (e) {
      setError(extractMessage(e, 'Không thể tải hợp đồng'));
    } finally {
      setLoading(false);
    }
  }, []);

  return { contract, signatures, loading, error, reload };
}

export function useSignContract(): UseSignContractResult {
  const [otpSent, setOtpSent] = useState<SendOtpApiResponse | null>(null);
  const [signing, setSigning] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sendOtp = useCallback(async (contractId: number) => {
    setSendingOtp(true);
    setError(null);
    try {
      const result = await contractApi.sendOtp(contractId);
      setOtpSent(result.data);
      return result.data;
    } catch (e) {
      setError(extractMessage(e, 'Không gửi được mã OTP'));
      return null;
    } finally {
      setSendingOtp(false);
    }
  }, []);

  const sign = useCallback(async (contractId: number, otp: string) => {
    setSigning(true);
    setError(null);
    try {
      const result = await contractApi.signWithOtp(contractId, { otpCode: otp });
      return result.data;
    } catch (e) {
      setError(extractMessage(e, 'Ký hợp đồng thất bại'));
      return null;
    } finally {
      setSigning(false);
    }
  }, []);

  return { otpSent, signing, sendingOtp, error, sendOtp, sign };
}

function extractMessage(err: unknown, fallback: string): string {
  if (typeof err === 'object' && err !== null) {
    const ax = err as {
      response?: { data?: { message?: string } };
      message?: string;
    };
    return ax.response?.data?.message ?? ax.message ?? fallback;
  }
  return fallback;
}
