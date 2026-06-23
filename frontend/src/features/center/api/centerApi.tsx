import axiosClient from '../../../shared/api/axiosClient';

export const CENTER_API_BASE = '/center';

export const centerApi = {
  http: axiosClient,
  basePath: CENTER_API_BASE,
};
