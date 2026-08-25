import axios, { AxiosError } from 'axios';
import { authStorage } from '../auth/authStorage';
import { APP_ROUTES } from '../constants/routes';

const baseURL =
  (import.meta.env.VITE_API_URL as string | undefined) ?? '/api';

const axiosClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

function isAuthEndpoint(url: string) {
  return (
    url.includes('/identity/login') ||
    url.includes('/identity/register') ||
    url.includes('/identity/password/')
  );
}

function redirectToExpiredSession() {
  const path = window.location.pathname;
  if (path === APP_ROUTES.login || isRedirectingToLogin) {
    return;
  }
  isRedirectingToLogin = true;
  authStorage.clearAll();
  const next = encodeURIComponent(window.location.pathname + window.location.search);
  window.location.assign(`${APP_ROUTES.login}?session=expired&next=${next}`);
}

axiosClient.interceptors.request.use((config) => {
  const requestUrl = config.url ?? '';
  const token = authStorage.getToken();
  if (token && !isAuthEndpoint(requestUrl) && authStorage.isSessionExpired()) {
    redirectToExpiredSession();
    return Promise.reject(new Error('Phiên đăng nhập đã hết hạn.'));
  }
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
    const path = window.location.pathname;
    const requestUrl = error.config?.url ?? '';

    if (status === 401 && !isAuthEndpoint(requestUrl) && path !== APP_ROUTES.login && !isRedirectingToLogin) {
      redirectToExpiredSession();
    }

    const isAiEndpoint = requestUrl.includes('/api/ai');
    if (status === 403 && !isAuthEndpoint(requestUrl) && !isAiEndpoint && path !== APP_ROUTES.forbidden) {
      window.location.assign(APP_ROUTES.forbidden);
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
