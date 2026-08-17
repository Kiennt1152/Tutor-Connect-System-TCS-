import axiosClient from '../../../shared/api/axiosClient';
import type { PublicTutorProfile } from '../types/homeTypes';

export const tutorProfileApi = {
  getPublicProfile(tutorId: number | string) {
    return axiosClient.get<PublicTutorProfile>(`/profile/tutor/${tutorId}`);
  },
};
