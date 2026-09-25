import axiosClient from '../../../shared/api/axiosClient';
import type { PublicTutorProfile } from '../types/homeTypes';

export const tutorProfileApi = {
  /** Hồ sơ công khai của gia sư (học vấn, chứng chỉ, kinh nghiệm). */
  getPublicProfile(tutorId: number | string) {
    return axiosClient.get<PublicTutorProfile>(`/profile/tutor/${tutorId}`);
  },
};
