export type VerificationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'VERIFIED' | 'REJECTED';

export type VerificationType = 'TUTOR_PROFILE' | 'TUTOR_CENTER_LICENSE';

export type VerificationDocumentType = 'ID_CARD' | 'DEGREE' | 'CERTIFICATE';

export interface VerificationDocument {
  documentId: number;
  fileId: number;
  fileName: string;
  fileUrl: string;
  mimeType: string | null;
  fileSize: number | null;
  documentType: VerificationDocumentType;
  uploadedAt: string;
}

export interface Verification {
  verificationId: number;
  userId: number;
  userEmail: string;
  verificationType: VerificationType;
  status: VerificationStatus;
  submittedAt: string | null;
  reviewedAt: string | null;
  reviewedByAdminEmail: string | null;
  adminNotes: string | null;
  rejectionReason: string | null;
  documents: VerificationDocument[];
  resubmittable: boolean;
}

export interface VerificationSubmission {
  verificationType: VerificationType;
  documents: DocumentUpload[];
}

export interface DocumentUpload {
  documentType: VerificationDocumentType;
  fileId: number;
}

export interface VerificationDecision {
  decision: 'APPROVE' | 'REJECT';
  note: string;
}

export interface DocumentSlotConfig {
  key: 'ID_FRONT' | 'ID_BACK' | 'OTHER_CERTIFICATES';
  label: string;
  hint: string;
  required: boolean;
  documentType: VerificationDocumentType;
  multi: boolean;
}

export const DOCUMENT_SLOTS: DocumentSlotConfig[] = [
  {
    key: 'ID_FRONT',
    label: 'ID Card — Front Side (CCCD/CMND mặt trước)',
    hint: 'Citizen ID card or passport — front side, clearly showing photo and full name',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
  {
    key: 'ID_BACK',
    label: 'ID Card — Back Side (CCCD/CMND mặt sau)',
    hint: 'Citizen ID card or passport — back side, clearly showing barcode and address',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
  {
    key: 'OTHER_CERTIFICATES',
    label: 'Other Credentials (Bằng cấp, chứng chỉ khác)',
    hint: 'Optional. Upload graduation certificates, teaching certificates, IELTS/TOEFL scores, or other qualifications',
    required: false,
    documentType: 'CERTIFICATE',
    multi: true,
  },
];

