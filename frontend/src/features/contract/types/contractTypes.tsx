export type ContractStatus = 'PENDING' | 'DRAFT' | 'SIGNED' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED';
export type ContractSourceType = 'PRIVATE' | 'CENTER';
export type ContractSignatureStatus = 'PENDING' | 'SIGNED' | 'EXPIRED';
export type PartyRole = 'CLIENT' | 'TUTOR' | 'CENTER';
export type EscrowStatus = 'PENDING' | 'FUNDED' | 'RELEASED' | 'REFUNDED' | 'ON_HOLD' | 'DISPUTED';
export type PaymentTransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface ContractPartyInfo {
  userId: number;
  fullName: string;
  email: string;
  phone?: string | null;
}

export interface EscrowPaymentInfo {
  escrowId: number;
  escrowStatus: EscrowStatus;
  paymentTransactionId: number | null;
  paymentStatus: PaymentTransactionStatus | null;
  amount: number | string | null;
  referenceCode: string | null;
  bankName: string | null;
  bankBin: string | null;
  accountNumber: string | null;
  accountName: string | null;
  transferContent: string | null;
  qrUrl: string | null;
  depositedAt: string | null;
  processedAt: string | null;
}

export interface ContractResponse {
  contractId: number;
  contractNo: string;
  status: ContractStatus;
  sourceType?: ContractSourceType | null;
  assignmentId?: number | null;
  classId: number | null;
  classStudentId?: number | null;
  recruitmentApplicationId?: number | null;
  clientId?: number | null;
  clientName?: string | null;
  clientEmail?: string | null;
  tutorId?: number | null;
  tutorName?: string | null;
  tutorEmail?: string | null;
  centerId?: number | null;
  centerName?: string | null;
  centerEmail?: string | null;
  templateId?: number | null;
  templateName?: string | null;
  termsSummary: string | null;
  /** Văn bản hợp đồng đầy đủ (Quốc hiệu + BÊN A + BÊN B + điều khoản). */
  documentText?: string | null;
  contractFileUrl: string | null;
  hasAllSignatures?: boolean;
  signedCount?: number;
  requiredSignatures?: number;
  signedAt: string | null;
  expiresAt?: string | null;
  confirmedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  classTitle: string | null;
  classType: string | null;
  tuitionFee: number | string | null;
  lessonMode: string | null;
  numberOfSessions: number | null;
  tutor: ContractPartyInfo | null;
  client: ContractPartyInfo | null;
  center: ContractPartyInfo | null;
  escrowPayment?: EscrowPaymentInfo | null;
}

export type ContractApiResponse = ContractResponse;

export interface ContractSignatureApiResponse {
  signatureId: number;
  partyRole: PartyRole;
  partyLabel: string;
  signerRole: string;
  signerId: number | null;
  signerName: string | null;
  signerEmail: string | null;
  signatureStatus: ContractSignatureStatus;
  signedAt: string | null;
  otpExpiresAt: string | null;
  remainingOtpAttempts: number;
  isOtpExpired: boolean;
  isCurrentUser: boolean;
}

export interface ContractSignatureListApiResponse {
  contractId: number;
  contractNo: string;
  hasAllSignatures: boolean;
  fullySigned: boolean;
  signedCount: number;
  requiredSignatures: number;
  totalRequired: number;
  signatures: ContractSignatureApiResponse[];
}

export interface SignatureInfo {
  signatureId: number;
  signerUserId: number | null;
  signerName: string;
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

export interface SendOtpApiResponse {
  message: string;
  maskedEmail?: string;
  expiresInMinutes: number;
  maxAttempts: number;
}

export interface OtpSentResponse {
  maskedEmail: string;
  message: string;
}

export interface SignContractRequest {
  otpCode: string;
}

export interface SignWithOtpApiRequest {
  otpCode: string;
}

export interface GenerateContractApiRequest {
  assignmentId?: number;
  classStudentId?: number;
  templateId?: number;
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
  recruitmentApplicationId: number | null;
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
  classTitle: string | null;
  classType: string | null;
  tuitionFee: number | string | null;
  lessonMode: string | null;
  numberOfSessions: number | null;
  tutor: ContractPartyInfo | null;
  client: ContractPartyInfo | null;
  center: ContractPartyInfo | null;
  escrowPayment: EscrowPaymentInfo | null;
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
