export type ContractStatus = 'DRAFT' | 'SIGNED' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED';

export interface ContractPartyInfo {
  userId: number;
  fullName: string;
  email: string;
  phone?: string | null;
}

export interface ContractResponse {
  contractId: number;
  contractNo: string;
  status: ContractStatus;
  termsSummary: string | null;
  contractFileUrl: string | null;
  signedAt: string | null;
  createdAt: string;
  updatedAt: string;
  classId: number | null;
  classTitle: string | null;
  classType: string | null;
  tuitionFee: number | string | null;
  lessonMode: string | null;
  numberOfSessions: number | null;
  tutor: ContractPartyInfo | null;
  client: ContractPartyInfo | null;
  center: ContractPartyInfo | null;
}

export interface SignatureInfo {
  signatureId: number;
  signerUserId: number;
  signerName: string;
  signerRole: string;
  signedAt: string;
  isCurrentUser: boolean;
}

export interface SignatureStatusResponse {
  contractId: number;
  contractNo: string;
  fullySigned: boolean;
  signedCount: number;
  totalRequired: number;
  signatures: SignatureInfo[];
}

export interface OtpSentResponse {
  maskedEmail: string;
  message: string;
}

export interface SignContractRequest {
  otpCode: string;
}
