import axiosClient from '../../../shared/api/axiosClient';

export const CATALOG_API_BASE = '/catalog';

export const catalogApi = {
  http: axiosClient,
  basePath: CATALOG_API_BASE,
};
