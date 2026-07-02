import axios from 'axios';
import { authStorage } from '../auth/authStorage';
import { APP_ROUTES } from '../constants/routes';

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
    const status = error.response?.status;
    const path = window.location.pathname;

    if (status === 401 && path !== APP_ROUTES.login && path !== APP_ROUTES.register) {
      authStorage.clearAll();
      window.location.assign(`${APP_ROUTES.login}?session=expired`);
    }

    if (status === 403 && path !== APP_ROUTES.forbidden) {
      window.location.assign(APP_ROUTES.forbidden);
    }

    return Promise.reject(error);
  },
);

export default axiosClient;
