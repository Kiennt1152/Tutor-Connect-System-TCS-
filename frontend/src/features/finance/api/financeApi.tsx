import axiosClient from '../../../shared/api/axiosClient';

export const FINANCE_API_BASE = '/finance';

export const financeApi = {
  http: axiosClient,
  basePath: FINANCE_API_BASE,
};
