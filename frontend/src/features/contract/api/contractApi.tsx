import axiosClient from '../../../shared/api/axiosClient';
import type {
  ContractResponse,
  SignatureStatusResponse,
  OtpSentResponse,
  SignContractRequest,
} from '../types/contractTypes';

export const CONTRACT_API_BASE = '/contract';

export const contractApi = {
  http: axiosClient,
  basePath: CONTRACT_API_BASE,

  async getMyContracts(): Promise<ContractResponse[]> {
    const res = await axiosClient.get<ContractResponse[]>('/contract');
    return res.data;
  },

  async getContract(contractId: number): Promise<ContractResponse> {
    const res = await axiosClient.get<ContractResponse>(`/contract/${contractId}`);
    return res.data;
  },

  async generateForAssignment(assignmentId: number): Promise<ContractResponse> {
    const res = await axiosClient.post<ContractResponse>(
      `/contract/generate/assignment/${assignmentId}`,
    );
    return res.data;
  },

  async generateForEnrollment(classStudentId: number): Promise<ContractResponse> {
    const res = await axiosClient.post<ContractResponse>(
      `/contract/generate/enrollment/${classStudentId}`,
    );
    return res.data;
  },

  async sendSignOtp(contractId: number): Promise<OtpSentResponse> {
    const res = await axiosClient.post<OtpSentResponse>(
      `/contract/${contractId}/send-otp`,
    );
    return res.data;
  },

  async signContract(
    contractId: number,
    payload: SignContractRequest,
  ): Promise<ContractResponse> {
    const res = await axiosClient.post<ContractResponse>(
      `/contract/${contractId}/sign`,
      payload,
    );
    return res.data;
  },

  async getSignatureStatus(contractId: number): Promise<SignatureStatusResponse> {
    const res = await axiosClient.get<SignatureStatusResponse>(
      `/contract/${contractId}/signatures`,
    );
    return res.data;
  },
};
