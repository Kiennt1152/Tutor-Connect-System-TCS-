import axiosClient from '../../../shared/api/axiosClient';

export const PROFILE_API_BASE = '/profile';

export const profileApi = {
  http: axiosClient,
  basePath: PROFILE_API_BASE,
};
