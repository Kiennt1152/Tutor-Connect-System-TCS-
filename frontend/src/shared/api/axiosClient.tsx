import axios, { AxiosError } from 'axios';
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

let isRedirectingToLogin = false;

axiosClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url ?? '';

    const isAuthEndpoint =
      requestUrl.includes('/identity/login') ||
      requestUrl.includes('/identity/register') ||
      requestUrl.includes('/identity/password/');

    if ((status === 401 || status === 403) && !isAuthEndpoint && !isRedirectingToLogin) {
      isRedirectingToLogin = true;
      authStorage.clearAll();
      const next = encodeURIComponent(window.location.pathname + window.location.search);
      window.location.assign(`/login?next=${next}`);
    }

    if (error.code === 'ERR_NETWORK' || !error.response) {
      return Promise.reject(
        new Error(
          'Không thể kết nối đến máy chủ. Vui lòng kiểm tra backend đang chạy và CORS đã được cấu hình.',
        ),
      );
    }

    return Promise.reject(error);
  },
);

export default axiosClient;
