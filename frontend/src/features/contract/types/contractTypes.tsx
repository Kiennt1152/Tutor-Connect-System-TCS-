export type ContractStatus = 'PENDING' | 'DRAFT' | 'SIGNED' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED';
export type ContractSourceType = 'PRIVATE' | 'CENTER';
export type ContractSignatureStatus = 'PENDING' | 'SIGNED' | 'EXPIRED';
export type PartyRole = 'CLIENT' | 'TUTOR' | 'CENTER';

<<<<<<< HEAD
// ─── API Response Types ──────────────────────────────────────────────────────
=======
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
  signerUserId: number | null;
  signerName: string | null;
  signerRole: string;
  signedAt: string | null;
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
  maskedEmail?: string;
  message: string;
  expiresInMinutes?: number;
  maxAttempts?: number;
}

export interface SignContractRequest {
  otpCode: string;
}
>>>>>>> origin/main

export interface ContractApiResponse {
  contractId: number;
  contractNo: string;
  status: ContractStatus;
  sourceType: ContractSourceType;
  assignmentId: number | null;
  classId: number | null;
  classStudentId: number | null;
  clientId: number | null;
  clientName: string | null;
  clientEmail: string | null;
  tutorId: number | null;
  tutorName: string | null;
  tutorEmail: string | null;
  centerId: number | null;
  centerName: string | null;
  centerEmail: string | null;
  templateId: number | null;
  templateName: string | null;
  termsSummary: string | null;
  contractFileUrl: string | null;
  hasAllSignatures: boolean;
  signedCount: number;
  requiredSignatures: number;
  signedAt: string | null;
  expiresAt: string | null;
  confirmedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractSignatureApiResponse {
  signatureId: number;
  partyRole: PartyRole;
  partyLabel: string;
  signerId: number | null;
  signerName: string | null;
  signerEmail: string | null;
  signatureStatus: ContractSignatureStatus;
  signedAt: string | null;
  otpExpiresAt: string | null;
  remainingOtpAttempts: number;
  isOtpExpired: boolean;
}

export interface ContractSignatureListApiResponse {
  contractId: number;
  contractNo: string;
  hasAllSignatures: boolean;
  signedCount: number;
  requiredSignatures: number;
  signatures: ContractSignatureApiResponse[];
}

export interface SendOtpApiResponse {
  message: string;
  expiresInMinutes: number;
  maxAttempts: number;
}

export interface SignWithOtpApiRequest {
  otpCode: string;
}

export interface GenerateContractApiRequest {
  assignmentId: number;
}

export interface Contract {
  id: string;
  contractNo: string;
  status: ContractStatus;
  statusLabel: string;
  sourceType: ContractSourceType;
  sourceTypeLabel: string;
  assignmentId: number | null;
  classId: number | null;
  classStudentId: number | null;
  clientId: number | null;
  clientName: string | null;
  clientEmail: string | null;
  tutorId: number | null;
  tutorName: string | null;
  tutorEmail: string | null;
  centerId: number | null;
  centerName: string | null;
  centerEmail: string | null;
  templateId: number | null;
  templateName: string | null;
  termsSummary: string | null;
  contractFileUrl: string | null;
  hasAllSignatures: boolean;
  signedCount: number;
  requiredSignatures: number;
  signedAt: string | null;
  expiresAt: string | null;
  confirmedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractSignature {
  id: number;
  partyRole: PartyRole;
  partyLabel: string;
  signerId: number | null;
  signerName: string | null;
  signerEmail: string | null;
  signatureStatus: ContractSignatureStatus;
  signatureStatusLabel: string;
  signedAt: string | null;
  otpExpiresAt: string | null;
  remainingOtpAttempts: number;
  isOtpExpired: boolean;
}

export interface ContractSignatureList {
  contractId: number;
  contractNo: string;
  hasAllSignatures: boolean;
  signedCount: number;
  requiredSignatures: number;
  signatures: ContractSignature[];
}

export interface OtpSendResult {
  message: string;
  expiresInMinutes: number;
  maxAttempts: number;
}
