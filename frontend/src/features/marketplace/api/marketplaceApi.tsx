import axiosClient from '../../../shared/api/axiosClient';

export const MARKETPLACE_API_BASE = '/marketplace';

export const marketplaceApi = {
  http: axiosClient,
  basePath: MARKETPLACE_API_BASE,
};
