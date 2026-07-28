import { useCallback, useEffect, useState } from 'react';
import { contractApi } from '../api/contractApi';
import type {
  ContractResponse,
  SignatureStatusResponse,
  OtpSentResponse,
} from '../types/contractTypes';

interface UseContractListResult {
  contracts: ContractResponse[];
  loading: boolean;
  error: string | null;
  reload: () => Promise<void>;
}

interface UseContractDetailResult {
  contract: ContractResponse | null;
  signatures: SignatureStatusResponse | null;
  loading: boolean;
  error: string | null;
  reload: (contractId: number) => Promise<void>;
}

interface UseSignContractResult {
  otpSent: OtpSentResponse | null;
  signing: boolean;
  sendingOtp: boolean;
  error: string | null;
  sendOtp: (contractId: number) => Promise<OtpSentResponse | null>;
  sign: (contractId: number, otp: string) => Promise<ContractResponse | null>;
}

export function useContractList(): UseContractListResult {
  const [contracts, setContracts] = useState<ContractResponse[]>([]);
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
  const [contract, setContract] = useState<ContractResponse | null>(null);
  const [signatures, setSignatures] = useState<SignatureStatusResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async (contractId: number) => {
    setLoading(true);
    setError(null);
    try {
      const [c, s] = await Promise.all([
        contractApi.getContract(contractId),
        contractApi.getSignatureStatus(contractId),
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
  const [otpSent, setOtpSent] = useState<OtpSentResponse | null>(null);
  const [signing, setSigning] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sendOtp = useCallback(async (contractId: number) => {
    setSendingOtp(true);
    setError(null);
    try {
      const result = await contractApi.sendSignOtp(contractId);
      setOtpSent(result);
      return result;
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
      const result = await contractApi.signContract(contractId, { otpCode: otp });
      return result;
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
