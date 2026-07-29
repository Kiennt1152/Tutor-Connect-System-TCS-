import axiosClient from '../../../shared/api/axiosClient';
import type {
  ContractApiResponse,
  ContractSignatureListApiResponse,
  GenerateContractApiRequest,
  SendOtpApiResponse,
  SignWithOtpApiRequest,
} from '../types/contractTypes';

const BASE = '/contract';

export const contractApi = {
  getMyContracts() {
    return axiosClient.get<ContractApiResponse[]>(`${BASE}/my`);
  },

  getContract(contractId: number) {
    return axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
  },

  getSignatures(contractId: number) {
    return axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signatures`,
    );
  },

  sendOtp(contractId: number) {
    return axiosClient.post<SendOtpApiResponse>(`${BASE}/${contractId}/send-otp`);
  },

  signWithOtp(contractId: number, payload: SignWithOtpApiRequest) {
    return axiosClient.post<ContractApiResponse>(
      `${BASE}/${contractId}/sign`,
      payload,
    );
  },

  generateContract(payload: GenerateContractApiRequest) {
    return axiosClient.post<ContractApiResponse>(`${BASE}/generate`, payload);
  },
};
