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
  SaveRefundPayoutRequest,
  SignContractRequest,
  SignWithOtpApiRequest,
} from '../types/contractTypes';

export const CONTRACT_API_BASE = '/contract';
const BASE = CONTRACT_API_BASE;

type DataWrapped<T> = T & { data: T };

function withData<T>(payload: T): DataWrapped<T> {
  return Object.assign(payload as object, { data: payload }) as DataWrapped<T>;
}

function normalizeSignatureList(
  response: ContractSignatureListApiResponse,
): ContractSignatureListApiResponse {
  const currentUserId = authStorage.getUser()?.userId ?? null;
  const totalRequired = response.totalRequired ?? response.requiredSignatures ?? 0;
  const fullySigned = response.fullySigned ?? response.hasAllSignatures ?? false;

  return {
    ...response,
    totalRequired,
    requiredSignatures: response.requiredSignatures ?? totalRequired,
    fullySigned,
    hasAllSignatures: response.hasAllSignatures ?? fullySigned,
    signatures: response.signatures.map((signature) => ({
      ...signature,
      signerRole: signature.signerRole ?? signature.partyLabel,
      isCurrentUser:
        signature.isCurrentUser ?? (currentUserId != null && signature.signerId === currentUserId),
    })),
  };
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

  async getMyContracts(): Promise<DataWrapped<ContractResponse[]>> {
    const res = await axiosClient.get<ContractResponse[]>(BASE);
    return withData(res.data);
  },

  async getContractRaw(contractId: number) {
    return axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
  },

  async getContract(contractId: number): Promise<DataWrapped<ContractApiResponse>> {
    const res = await axiosClient.get<ContractApiResponse>(`${BASE}/${contractId}`);
    return withData(res.data);
  },

  async getSignatures(contractId: number): Promise<DataWrapped<ContractSignatureListApiResponse>> {
    const res = await axiosClient.get<ContractSignatureListApiResponse>(
      `${BASE}/${contractId}/signatures`,
    );
    return withData(normalizeSignatureList(res.data));
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

  // BF-03: gia sư từ chối thỏa thuận hợp tác chưa ký.
  async declineContract(contractId: number): Promise<{ message: string }> {
    const res = await axiosClient.post<{ message: string }>(`${BASE}/${contractId}/decline`);
    return res.data;
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

  async saveRefundPayoutInfo(
    contractId: number,
    payload: SaveRefundPayoutRequest,
  ): Promise<DataWrapped<ContractApiResponse>> {
    const res = await axiosClient.post<ContractApiResponse>(
      `${BASE}/${contractId}/refund-payout`,
      payload,
    );
    return withData(res.data);
  },

  async getSignatureStatus(contractId: number): Promise<SignatureStatusResponse> {
    const response = await this.getSignatures(contractId);
    return toSignatureStatus(response.data);
  },
};
