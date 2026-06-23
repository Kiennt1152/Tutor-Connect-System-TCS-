import axiosClient from '../../../shared/api/axiosClient';

export const IDENTITY_API_BASE = '/identity';

export const identityApi = {
  http: axiosClient,
  basePath: IDENTITY_API_BASE,
};
