import axiosClient from '../../../shared/api/axiosClient';
import type { OpenClassApiResponse } from '../types/openClassTypes';

export const openClassesApi = {
  listOpen: () =>
    axiosClient
      .get<OpenClassApiResponse[]>('/marketplace/classes', { params: { status: 'OPEN' } })
      .then((response) => response.data),
};
