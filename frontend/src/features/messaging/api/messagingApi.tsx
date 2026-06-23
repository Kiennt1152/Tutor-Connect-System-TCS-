import axiosClient from '../../../shared/api/axiosClient';

export const MESSAGING_API_BASE = '/messaging';

export const messagingApi = {
  http: axiosClient,
  basePath: MESSAGING_API_BASE,
};
