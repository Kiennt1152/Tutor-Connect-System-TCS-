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

export default axiosClient;
