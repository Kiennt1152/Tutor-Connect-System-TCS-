export type VerificationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'VERIFIED' | 'REJECTED';

export type VerificationType = 'TUTOR_PROFILE' | 'TUTOR_CENTER_LICENSE';

/**
 * DB-stored enum values (constrained by chk_verification_documents_type).
 * Reused for both tutor and center verification flows; the slot meaning is
 * resolved by combining with {@link VerificationType}.
 */
export type VerificationDocumentType = 'ID_CARD' | 'DEGREE' | 'CERTIFICATE' | 'LICENSE';

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

export type TutorDocumentSlotKey =
  | 'ID_FRONT'
  | 'ID_BACK'
  | 'OTHER_CERTIFICATES';

export type CenterDocumentSlotKey =
  | 'CENTER_BUSINESS_LICENSE'
  | 'CENTER_EDUCATION_PERMIT'
  | 'CENTER_TAX_CODE'
  | 'LEGAL_REP_ID_CARD'
  | 'LEGAL_REP_ID_CARD_BACK';

export type DocumentSlotKey = TutorDocumentSlotKey | CenterDocumentSlotKey;

export interface DocumentSlotConfig {
  readonly key: DocumentSlotKey;
  readonly label: string;
  readonly hint: string;
  readonly required: boolean;
  readonly documentType: VerificationDocumentType;
  readonly multi: boolean;
}

export const TUTOR_DOCUMENT_SLOTS: readonly DocumentSlotConfig[] = [
  {
    key: 'ID_FRONT',
    label: 'CCCD/CMND mặt trước',
    hint: 'Ảnh chụp rõ CCCD/CMND mặt trước, thấy đầy đủ ảnh và họ tên',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
  {
    key: 'ID_BACK',
    label: 'CCCD/CMND mặt sau',
    hint: 'Ảnh chụp rõ CCCD/CMND mặt sau, thấy đầy đủ mã vạch và địa chỉ thường trú',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
  {
    key: 'OTHER_CERTIFICATES',
    label: 'Bằng cấp và chứng chỉ khác',
    hint: 'Không bắt buộc. Tải lên bằng tốt nghiệp, chứng chỉ sư phạm, chứng chỉ IELTS/TOEFL hoặc các chứng chỉ liên quan khác',
    required: false,
    documentType: 'CERTIFICATE',
    multi: true,
  },
] as const;

/**
 * Four legally required documents for a Vietnamese tutor center to be verified
 * (per Luật Doanh nghiệp 2020, Nghị định 46/2017/NĐ-CP, Luật Quản lý thuế 2019).
 *
 * NOTE: DB CHECK constraint only allows {ID_CARD, DEGREE, CERTIFICATE, LICENSE},
 * so the 4 center slots are mapped onto 3 enum values:
 *   CENTER_BUSINESS_LICENSE   -> LICENSE
 *   CENTER_EDUCATION_PERMIT   -> LICENSE
 *   CENTER_TAX_CODE           -> CERTIFICATE
 *   LEGAL_REP_ID_CARD         -> ID_CARD
 */
export const CENTER_DOCUMENT_SLOTS: readonly DocumentSlotConfig[] = [
  {
    key: 'CENTER_BUSINESS_LICENSE',
    label: 'Giấy chứng nhận đăng ký doanh nghiệp (ĐKKD)',
    hint: 'Do Sở Kế hoạch & Đầu tư cấp. Đối với hộ kinh doanh: Giấy chứng nhận đăng ký hộ kinh doanh.',
    required: true,
    documentType: 'LICENSE',
    multi: false,
  },
  {
    key: 'CENTER_EDUCATION_PERMIT',
    label: 'Giấy phép hoạt động giáo dục',
    hint: 'Theo Nghị định 46/2017/NĐ-CP (trung tâm GD thường xuyên) hoặc giấy phép tương đương do cơ quan có thẩm quyền cấp.',
    required: true,
    documentType: 'LICENSE',
    multi: false,
  },
  {
    key: 'CENTER_TAX_CODE',
    label: 'Mã số thuế / Giấy đăng ký thuế',
    hint: 'Mã số thuế của doanh nghiệp hoặc hộ kinh doanh (theo Luật Quản lý thuế 2019).',
    required: true,
    documentType: 'CERTIFICATE',
    multi: false,
  },
  {
    key: 'LEGAL_REP_ID_CARD',
    label: 'CCCD/CMND người đại diện pháp luật (mặt trước)',
    hint: 'Mặt trước CCCD/CMND của người đại diện theo pháp luật (chủ DN / giám đốc / chủ hộ KD) — thấy rõ ảnh, họ tên và mã QR.',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
  {
    key: 'LEGAL_REP_ID_CARD_BACK',
    label: 'CCCD/CMND người đại diện pháp luật (mặt sau)',
    hint: 'Mặt sau CCCD/CMND của người đại diện theo pháp luật — thấy rõ mã vạch và địa chỉ thường trú.',
    required: true,
    documentType: 'ID_CARD',
    multi: false,
  },
] as const;

export const DOCUMENT_SLOTS: readonly DocumentSlotConfig[] = TUTOR_DOCUMENT_SLOTS;

export function getSlotsForRole(
  role: string,
): readonly DocumentSlotConfig[] {
  return role === 'TUTOR_CENTER' ? CENTER_DOCUMENT_SLOTS : TUTOR_DOCUMENT_SLOTS;
}

export function getVerificationTypeForRole(
  role: string,
): VerificationType {
  return role === 'TUTOR_CENTER' ? 'TUTOR_CENTER_LICENSE' : 'TUTOR_PROFILE';
}

