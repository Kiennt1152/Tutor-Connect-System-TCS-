import type { ProfileVerificationStatus, UserRole } from '../types/profileTypes';

export const VIETNAM_PHONE = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

export const VERIFICATION_LABEL: Record<ProfileVerificationStatus, string> = {
  UNDER_VERIFY: 'Đang chờ xét duyệt',
  VERIFIED: 'Đã xác minh',
  REJECTED: 'Bị từ chối',
};

export const ROLE_LABEL: Record<UserRole, string> = {
  PLATFORM_ADMIN: 'Quản trị viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm gia sư',
  CLIENT: 'Khách hàng (Phụ huynh / Học sinh)',
  UNKNOWN: 'Không xác định',
};
