import axiosClient from '../../../shared/api/axiosClient';
import type {
  CatalogItem,
  ChildProfile,
  ChildProfileRequest,
  DependentLinkStatus,
  GuardianProfile,
  LinkGuardianRequest,
  ProfileResponse,
  UpdateProfileRequest,
} from '../types/profileTypes';
import type { GuardianApproval } from '../types/guardianApprovalTypes';

export const PROFILE_API_BASE = '/profile';

export const profileApi = {
  getMyProfile: () => axiosClient.get<ProfileResponse>(`${PROFILE_API_BASE}/me`),

  updateMyProfile: (body: UpdateProfileRequest) =>
    axiosClient.put<ProfileResponse>(`${PROFILE_API_BASE}/me`, body),

  getDependentLinkStatus: () =>
    axiosClient.get<DependentLinkStatus>(`${PROFILE_API_BASE}/dependent-status`),

  getMyChildren: () => axiosClient.get<ChildProfile[]>(`${PROFILE_API_BASE}/children`),

  createChild: (body: ChildProfileRequest) =>
    axiosClient.post<ChildProfile>(`${PROFILE_API_BASE}/children`, body),

  getMyGuardian: () => axiosClient.get<GuardianProfile | null>(`${PROFILE_API_BASE}/guardian`),

  linkGuardian: (body: LinkGuardianRequest) =>
    axiosClient.post<GuardianProfile>(`${PROFILE_API_BASE}/guardian/link`, body),

  getPendingGuardianApprovals: () =>
    axiosClient.get<GuardianApproval[]>(`${PROFILE_API_BASE}/guardian/approvals/pending`),

  getSubmittedGuardianApprovals: () =>
    axiosClient.get<GuardianApproval[]>(`${PROFILE_API_BASE}/guardian/approvals/submitted`),

  approveGuardianRequest: (approvalId: number) =>
    axiosClient.post<GuardianApproval>(`${PROFILE_API_BASE}/guardian/approvals/${approvalId}/approve`),

  rejectGuardianRequest: (approvalId: number) =>
    axiosClient.post<GuardianApproval>(`${PROFILE_API_BASE}/guardian/approvals/${approvalId}/reject`),

  getGrades: () => axiosClient.get<CatalogItem[]>('/catalog/grades'),
};
