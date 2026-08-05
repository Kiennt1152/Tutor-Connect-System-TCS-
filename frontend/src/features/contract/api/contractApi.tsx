import axiosClient from '../../../shared/api/axiosClient';
import type {
  ContractApiResponse,
  ContractSignatureListApiResponse,
  GenerateContractApiRequest,
  SendOtpApiResponse,
  SignWithOtpApiRequest,
} from '../types/contractTypes';

const BASE = '/contract';

export const CONTRACT_API_BASE = BASE;

export const contractApi = {
  http: axiosClient,
  basePath: BASE,

  getMyContracts() {
    return axiosClient.get<ContractApiResponse[]>(`${BASE}/my`);
  },

  getContract(contractId: number) {
    return axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
  },

  getContractRaw(contractId: number) {
    return axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
  },

  generateContract(payload: GenerateContractApiRequest) {
    return axiosClient.post<ContractApiResponse>(`${BASE}/generate`, payload);
  },

  getSignatures(contractId: number) {
    return axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signatures`,
    );
  },

  getSignatureDetails(contractId: number) {
    return axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signature-details`,
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
};
