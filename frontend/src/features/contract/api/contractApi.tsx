import axiosClient from '../../../shared/api/axiosClient';

export const CONTRACT_API_BASE = '/contract';

export const contractApi = {
  http: axiosClient,
  basePath: CONTRACT_API_BASE,
};
