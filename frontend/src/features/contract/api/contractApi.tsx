import axiosClient from '../../../shared/api/axiosClient';
import { authStorage } from '../../../shared/auth/authStorage';
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

export const CONTRACT_API_BASE = '/contract';
const BASE = CONTRACT_API_BASE;

type DataWrapped<T> = T & { data: T };

function withData<T extends object>(payload: T): DataWrapped<T> {
  return Object.assign(payload, { data: payload });
}

function toSignatureStatus(response: ContractSignatureListApiResponse): SignatureStatusResponse {
  const currentUserId = authStorage.getUser()?.userId ?? null;
  return {
    contractId: response.contractId,
    contractNo: response.contractNo,
    fullySigned: response.hasAllSignatures,
    signedCount: response.signedCount,
    totalRequired: response.requiredSignatures,
    signatures: response.signatures
      .filter((signature) => signature.signatureStatus === 'SIGNED')
      .map((signature) => ({
        signatureId: signature.signatureId,
        signerUserId: signature.signerId,
        signerName: signature.signerName ?? signature.signerEmail ?? signature.partyLabel,
        signerRole: signature.partyLabel,
        signedAt: signature.signedAt,
        isCurrentUser: currentUserId != null && signature.signerId === currentUserId,
      })),
  };
}

export const contractApi = {
  http: axiosClient,
  basePath: BASE,

  async getMyContracts(): Promise<ContractResponse[]> {
    const res = await axiosClient.get<ContractResponse[]>(BASE);
    return res.data;
  },

  async getContract(contractId: number): Promise<DataWrapped<ContractApiResponse>> {
    const res = await axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
    return withData(res.data);
  },

  async getSignatures(contractId: number): Promise<DataWrapped<ContractSignatureListApiResponse>> {
    const res = await axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signatures`,
    );
    return withData(res.data);
  },

  async generateForAssignment(assignmentId: number): Promise<ContractResponse> {
    const res = await axiosClient.post<ContractResponse>(
      `${BASE}/generate/assignment/${assignmentId}`,
    );
    return res.data;
  },

  async generateForEnrollment(classStudentId: number): Promise<ContractResponse> {
    const res = await axiosClient.post<ContractResponse>(
      `${BASE}/generate/enrollment/${classStudentId}`,
    );
    return res.data;
  },

  async generateContract(payload: GenerateContractApiRequest): Promise<DataWrapped<ContractApiResponse>> {
    const res = await axiosClient.post<ContractApiResponse>(`${BASE}/generate`, payload);
    return withData(res.data);
  },

  async sendOtp(contractId: number): Promise<DataWrapped<SendOtpApiResponse>> {
    const res = await axiosClient.post<SendOtpApiResponse>(`${BASE}/${contractId}/send-otp`);
    return withData(res.data);
  },

  async sendSignOtp(contractId: number): Promise<OtpSentResponse> {
    const response = await this.sendOtp(contractId);
    return {
      maskedEmail: response.maskedEmail ?? '',
      message: response.message,
    };
  },

  async signWithOtp(
    contractId: number,
    payload: SignWithOtpApiRequest,
  ): Promise<DataWrapped<ContractApiResponse>> {
    const res = await axiosClient.post<ContractApiResponse>(
      `${BASE}/${contractId}/sign`,
      payload,
    );
    return withData(res.data);
  },

  async signContract(
    contractId: number,
    payload: SignContractRequest,
  ): Promise<ContractResponse> {
    const response = await this.signWithOtp(contractId, payload);
    return response.data;
  },

  async getSignatureStatus(contractId: number): Promise<SignatureStatusResponse> {
    const response = await this.getSignatures(contractId);
    return toSignatureStatus(response.data);
  },
};
