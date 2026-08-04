import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { contractApi } from '../api/contractApi';
import { mapContract, mapSignatureList } from '../mappers/contractMapper';
import type { Contract, ContractSignatureList } from '../types/contractTypes';

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useContract(contractId: number) {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [data, setData] = useState<Contract | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const loadingRef = useRef(false);

  const reload = useCallback(async () => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await contractApi.getContractRaw(contractId);
      setData(mapContract(response.data));
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải hợp đồng:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không tải được thông tin hợp đồng.');
      setStatus('error');
    } finally {
      loadingRef.current = false;
    }
  }, [contractId]);

  useEffect(() => {
    if (contractId > 0) {
      const timer = setTimeout(() => { void reload(); }, 0);
      return () => clearTimeout(timer);
    }
  }, [contractId, reload]);

  return { status, data, reload, errorMessage };
}

export function useContractSignatures(contractId: number) {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [data, setData] = useState<ContractSignatureList | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const loadingRef = useRef(false);

  const reload = useCallback(async () => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setStatus('loading');
    setErrorMessage(null);
    try {
      const response = await contractApi.getSignatures(contractId);
      setData(mapSignatureList(response.data));
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải trạng thái ký:', error);
      const apiMessage =
        axios.isAxiosError(error) && typeof error.response?.data?.message === 'string'
          ? error.response.data.message
          : null;
      setErrorMessage(apiMessage ?? 'Không tải được trạng thái ký.');
      setStatus('error');
    } finally {
      loadingRef.current = false;
    }
  }, [contractId]);

  useEffect(() => {
    if (contractId > 0) {
      const timer = setTimeout(() => { void reload(); }, 0);
      return () => clearTimeout(timer);
    }
  }, [contractId, reload]);

  return { status, data, reload, errorMessage };
}
