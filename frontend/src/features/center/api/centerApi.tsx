import axiosClient from '../../../shared/api/axiosClient';
import type {
  RecruitmentApplication,
  RecruitmentPost,
  SaveRecruitmentPostRequest,
} from '../types/centerTypes';

export const CENTER_API_BASE = '/center';

export const centerApi = {
  // ----- Phía trung tâm (FT-33) -----
  getMyPosts() {
    return axiosClient.get<RecruitmentPost[]>(`${CENTER_API_BASE}/recruitment/my-posts`);
  },
  createPost(payload: SaveRecruitmentPostRequest) {
    return axiosClient.post<RecruitmentPost>(`${CENTER_API_BASE}/recruitment`, payload);
  },
  updatePost(recruitmentId: number, payload: SaveRecruitmentPostRequest) {
    return axiosClient.put<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}`,
      payload,
    );
  },
  publishPost(recruitmentId: number) {
    return axiosClient.post<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/publish`,
    );
  },
  closePost(recruitmentId: number) {
    return axiosClient.post<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/close`,
    );
  },
  getApplications(recruitmentId: number) {
    return axiosClient.get<RecruitmentApplication[]>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/applications`,
    );
  },
  decideApplication(recruitmentAppId: number, approve: boolean) {
    return axiosClient.post<RecruitmentApplication>(
      `${CENTER_API_BASE}/recruitment/applications/${recruitmentAppId}/decision`,
      { approve },
    );
  },

  // ----- Phía gia sư -----
  getOpenPosts() {
    return axiosClient.get<RecruitmentPost[]>(`${CENTER_API_BASE}/recruitment`);
  },
  apply(recruitmentId: number, coverLetter: string) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/apply`,
      { coverLetter },
    );
  },
  getMyApplications() {
    return axiosClient.get<RecruitmentApplication[]>(
      `${CENTER_API_BASE}/recruitment/applications/mine`,
    );
  },
};
