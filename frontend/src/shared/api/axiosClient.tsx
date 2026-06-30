import axios from 'axios';
import { authStorage } from '../auth/authStorage';

const baseURL =
  (import.meta.env.VITE_API_URL as string | undefined) ??
  (import.meta.env.DEV ? '/api' : 'http://localhost:8080/api');

const axiosClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

axiosClient.interceptors.request.use((config) => {
  const token = authStorage.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const requestUrl = error.config?.url ?? '';
      const isPublicAuthRequest =
        requestUrl.includes('/identity/login') ||
        requestUrl.includes('/identity/register') ||
        requestUrl.includes('/identity/password/');

      if (!isPublicAuthRequest) {
        authStorage.clearAll();
        const returnPath = window.location.pathname;
        if (returnPath !== '/login') {
          sessionStorage.setItem('auth_redirect', returnPath);
          window.location.assign('/login');
        }
      }
    }
    return Promise.reject(error);
  },
);

export default axiosClient;
