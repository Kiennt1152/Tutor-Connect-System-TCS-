import axiosClient from '../../../shared/api/axiosClient';
import type {
  ContractApiResponse,
  ContractResponse,
  ContractSignatureListApiResponse,
  GenerateContractApiRequest,
  OtpSentResponse,
  SendOtpApiResponse,
  SignatureStatusResponse,
  SignContractRequest,
  SignWithOtpApiRequest,
} from '../types/contractTypes';

const BASE = '/contract';

export const CONTRACT_API_BASE = BASE;

export const contractApi = {
  http: axiosClient,
  basePath: BASE,

  async getMyContracts(): Promise<ContractResponse[]> {
    const response = await axiosClient.get<ContractResponse[]>(BASE);
    return response.data;
  },

  async getContract(contractId: number): Promise<ContractResponse> {
    const response = await axiosClient.get<ContractResponse>(`${BASE}/${contractId}`);
    return response.data;
  },

  getContractRaw(contractId: number) {
    return axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
  },

  async generateForAssignment(assignmentId: number): Promise<ContractResponse> {
    const response = await axiosClient.post<ContractResponse>(
      `${BASE}/generate/assignment/${assignmentId}`,
    );
    return response.data;
  },

  async generateForEnrollment(classStudentId: number): Promise<ContractResponse> {
    const response = await axiosClient.post<ContractResponse>(
      `${BASE}/generate/enrollment/${classStudentId}`,
    );
    return response.data;
  },

  generateContract(payload: GenerateContractApiRequest) {
    return axiosClient.post<ContractApiResponse>(`${BASE}/generate`, payload);
  },

  async sendSignOtp(contractId: number): Promise<OtpSentResponse> {
    const response = await axiosClient.post<OtpSentResponse>(`${BASE}/${contractId}/send-otp`);
    return response.data;
  },

  sendOtp(contractId: number) {
    return axiosClient.post<SendOtpApiResponse>(`${BASE}/${contractId}/send-otp`);
  },

  async signContract(
    contractId: number,
    payload: SignContractRequest,
  ): Promise<ContractResponse> {
    const response = await axiosClient.post<ContractResponse>(
      `${BASE}/${contractId}/sign`,
      payload,
    );
    return response.data;
  },

  signWithOtp(contractId: number, payload: SignWithOtpApiRequest) {
    return axiosClient.post<ContractApiResponse>(
      `${BASE}/${contractId}/sign`,
      payload,
    );
  },

  async getSignatureStatus(contractId: number): Promise<SignatureStatusResponse> {
    const response = await axiosClient.get<SignatureStatusResponse>(
      `${BASE}/${contractId}/signatures`,
    );
    return response.data;
  },

  getSignatures(contractId: number) {
    return axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signature-details`,
    );
  },
};
