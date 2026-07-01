import axiosClient from '../../../shared/api/axiosClient';
import type { SignContractRequest, SignContractResponse } from '../types/contractTypes';

export const CONTRACT_API_BASE = '/contract';

export const contractApi = {
  signContract: (body: SignContractRequest) =>
    axiosClient.post<SignContractResponse>(`${CONTRACT_API_BASE}/sign`, body),
};
