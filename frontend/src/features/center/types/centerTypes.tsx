export type RecruitmentPostStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED';

export type RecruitmentApplicationStatus =
  | 'APPLIED'
  | 'SCREENING'
  | 'INTERVIEW'
  | 'PASSED'
  | 'HIRED'
  | 'REJECTED'
  | 'WITHDRAWN';

/** Một tin tuyển gia sư (FT-33). */
export interface RecruitmentPost {
  recruitmentId: number;
  centerId: number;
  centerName: string | null;
  title: string;
  description: string;
  requirements: string | null;
  benefits: string | null;
  requiredExperience: number | null;
  maxPositions: number | null;
  subjectId: number | null;
  subjectName: string | null;
  locationId: number | null;
  locationLabel: string | null;
  provinceName: string | null;
  wardName: string | null;
  addressDetail: string | null;
  status: RecruitmentPostStatus;
  publishedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
  applicationCount: number;
}

/** Dữ liệu tạo/sửa tin. */
export interface SaveRecruitmentPostRequest {
  title: string;
  description: string;
  requirements?: string;
  benefits?: string;
  requiredExperience?: number | null;
  maxPositions?: number | null;
  subjectName?: string;
  provinceName?: string;
  wardName?: string;
  addressDetail?: string;
}

/** Một đơn ứng tuyển. */
export interface RecruitmentApplication {
  recruitmentAppId: number;
  recruitmentId: number;
  postTitle: string | null;
  centerName: string | null;
  tutorId: number;
  tutorName: string | null;
  tutorPhone: string | null;
  tutorAvatar: string | null;
  experienceYears: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  coverLetter: string | null;
  status: RecruitmentApplicationStatus;
  appliedAt: string;
  reviewedAt: string | null;
  /** Bằng cấp / chứng chỉ đã xác minh gia sư đã nộp (không gồm ảnh CCCD). */
  certificates?: CertificateInfo[];
}

export interface CertificateInfo {
  fileName: string;
  fileUrl: string;
  mimeType: string | null;
  fileSize: number | null;
}

export type MembershipStatus = 'ACTIVE' | 'INACTIVE' | 'TERMINATED';

/** Một tin tuyển dụng của trung tâm mà gia sư đã ứng tuyển. */
export interface AppliedPost {
  recruitmentId: number;
  postTitle: string | null;
  applicationStatus: RecruitmentApplicationStatus;
  appliedAt: string;
}

/** Một gia sư là thành viên của trung tâm. */
export interface CenterMember {
  membershipId: number;
  tutorId: number;
  tutorName: string | null;
  tutorPhone: string | null;
  tutorAvatar: string | null;
  experienceYears: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  joinedAt: string;
  status: MembershipStatus;
  appliedPosts?: AppliedPost[];
}
