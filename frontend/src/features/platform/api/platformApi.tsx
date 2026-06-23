import axiosClient from '../../../shared/api/axiosClient';

export const PLATFORM_API_BASE = '/platform';

export const platformApi = {
  http: axiosClient,
  basePath: PLATFORM_API_BASE,
};
