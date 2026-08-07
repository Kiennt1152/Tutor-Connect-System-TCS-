import axiosClient from '../../../shared/api/axiosClient';
import type {
  AvatarUploadResponse,
  CatalogItem,
  ChildProfile,
  ChildProfileRequest,
  DependentLinkStatus,
  GuardianProfile,
  LinkGuardianRequest,
  LinkChildAccountRequest,
  ProfileResponse,
  UpdateProfileRequest,
  UpdateChildProfileRequest,
} from '../types/profileTypes';
import type { GuardianApproval } from '../types/guardianApprovalTypes';

export const PROFILE_API_BASE = '/profile';

export const profileApi = {
  http: axiosClient,
  basePath: PROFILE_API_BASE,

  async getMyProfile(): Promise<ProfileResponse> {
    const res = await axiosClient.get<ProfileResponse>(`${PROFILE_API_BASE}/me`);
    return res.data;
  },

  async updateMyProfile(payload: UpdateProfileRequest): Promise<ProfileResponse> {
    const res = await axiosClient.put<ProfileResponse>(`${PROFILE_API_BASE}/me`, payload);
    return res.data;
  },

  async uploadAvatar(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axiosClient.post<AvatarUploadResponse>(`${PROFILE_API_BASE}/me/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.avatarUrl;
  },

  getDependentLinkStatus: () =>
    axiosClient.get<DependentLinkStatus>(`${PROFILE_API_BASE}/dependent-status`),

  getMyChildren: () => axiosClient.get<ChildProfile[]>(`${PROFILE_API_BASE}/children`),

  createChild: (body: ChildProfileRequest) =>
    axiosClient.post<ChildProfile>(`${PROFILE_API_BASE}/children`, body),

  getChildById: (childProfileId: number) =>
    axiosClient.get<ChildProfile>(`${PROFILE_API_BASE}/children/${childProfileId}`),

  updateChild: (childProfileId: number, body: UpdateChildProfileRequest) =>
    axiosClient.put<ChildProfile>(`${PROFILE_API_BASE}/children/${childProfileId}`, body),

  deleteChild: (childProfileId: number) =>
    axiosClient.delete(`${PROFILE_API_BASE}/children/${childProfileId}`),

  linkChildAccount: (body: LinkChildAccountRequest) =>
    axiosClient.post<ChildProfile>(`${PROFILE_API_BASE}/children/link-account`, body),

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
