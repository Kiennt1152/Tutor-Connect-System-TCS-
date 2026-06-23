import axiosClient from '../../../shared/api/axiosClient';
import type { AuthResponse, LoginRequest } from '../types/identityTypes';

export const IDENTITY_API_BASE = '/identity';

export const identityApi = {
  http: axiosClient,
  basePath: IDENTITY_API_BASE,

  login: (payload: LoginRequest) =>
    axiosClient
      .post<AuthResponse>(`${IDENTITY_API_BASE}/login`, payload)
      .then((response) => response.data),
};
