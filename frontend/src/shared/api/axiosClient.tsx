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

function isPublicEndpoint(url: string) {
  return (
    url.startsWith('/ai') ||
    url.includes('/ai/') ||
    url.startsWith('/catalog') ||
    url.includes('/catalog/') ||
    url.startsWith('/marketplace') ||
    url.includes('/marketplace/') ||
    url.startsWith('/files/public')
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
  if (token && authStorage.isSessionExpired()) {
    if (isPublicEndpoint(requestUrl)) {
      authStorage.clearAll();
    } else if (!isAuthEndpoint(requestUrl)) {
      redirectToExpiredSession();
      return Promise.reject(new Error('Phiên đăng nhập đã hết hạn.'));
    }
  }
  const currentToken = authStorage.getToken();
  if (currentToken) {
    config.headers.Authorization = `Bearer ${currentToken}`;
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

    const isAiEndpoint = requestUrl.startsWith('/ai') || requestUrl.includes('/ai');

    if (status === 401 && !isAuthEndpoint(requestUrl) && !isAiEndpoint && path !== APP_ROUTES.login && !isRedirectingToLogin) {
      redirectToExpiredSession();
    }

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
