import axiosClient from '../../../shared/api/axiosClient';
import type { TutorSearchApiResponse, TutorSearchParams } from '../types/tutorSearchTypes';

export const tutorSearchApi = {
  searchTutors(params: TutorSearchParams) {
    const query = new URLSearchParams();
    if (params.keyword?.trim()) {
      query.set('keyword', params.keyword.trim());
    }
    if (params.subjectId != null) {
      query.set('subjectId', String(params.subjectId));
    }
    const suffix = query.toString();
    return axiosClient.get<TutorSearchApiResponse[]>(
      `/marketplace/tutors/search${suffix ? `?${suffix}` : ''}`,
    );
  },
};
