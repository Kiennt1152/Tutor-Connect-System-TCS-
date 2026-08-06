export type ContractStatus = 'PENDING' | 'DRAFT' | 'SIGNED' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED';
export type ContractSourceType = 'PRIVATE' | 'CENTER';
export type ContractSignatureStatus = 'PENDING' | 'SIGNED' | 'EXPIRED';
export type PartyRole = 'CLIENT' | 'TUTOR' | 'CENTER';

// ─── API Response Types ──────────────────────────────────────────────────────

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
  escrowPaymentReference: string | null;
  escrowPaymentAmount: number | null;
  escrowPaymentStatus: string | null;
  escrowPaymentQrUrl: string | null;
  escrowPaymentBankName: string | null;
  escrowPaymentAccountNumber: string | null;
  escrowPaymentAccountName: string | null;
  escrowPaymentTransferContent: string | null;
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
  escrowPaymentReference: string | null;
  escrowPaymentAmount: number | null;
  escrowPaymentStatus: string | null;
  escrowPaymentQrUrl: string | null;
  escrowPaymentBankName: string | null;
  escrowPaymentAccountNumber: string | null;
  escrowPaymentAccountName: string | null;
  escrowPaymentTransferContent: string | null;
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
