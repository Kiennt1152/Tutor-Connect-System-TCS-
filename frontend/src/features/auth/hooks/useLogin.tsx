import { useCallback, useState } from 'react';
import axios from 'axios';
import { authApi } from '../api/authApi';
import type { LoginFormValues, LoginResponse } from '../types/authTypes';


function extractError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Record<string, string> | undefined;
    if (data) {
      if (typeof data.message === 'string') {
        return data.message;
      }
      const first = Object.values(data).find((value) => typeof value === 'string');
      if (first) {
        return first;
      }
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Không kết nối được máy chủ. Hãy kiểm tra backend đang chạy ở cổng 8080.';
    }
  }
  return 'Đã có lỗi xảy ra. Vui lòng thử lại.';
}


function persistSession(data: LoginResponse) {
  localStorage.setItem('token', data.token);
  localStorage.setItem(
    'user',
    JSON.stringify({
      userId: data.userId,
      email: data.email,
      status: data.status,
      role: data.role,
      displayName: data.displayName,
    }),
  );
}

export function useLogin() {
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const login = useCallback(async (values: LoginFormValues): Promise<LoginResponse | null> => {
    setSubmitting(true);
    setFormError(null);
    try {
      const response = await authApi.login({
        email: values.email.trim(),
        password: values.password,
      });
      persistSession(response);
      return response;
    } catch (err) {
      setFormError(extractError(err));
      return null;
    } finally {
      setSubmitting(false);
    }
  }, []);

  return { submitting, formError, login };
}
