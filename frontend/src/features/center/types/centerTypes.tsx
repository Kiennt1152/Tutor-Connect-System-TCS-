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
}
