import { useCallback, useState } from 'react';
import axios from 'axios';
import { authApi } from '../api/authApi';
import type { RegisterFormValues, RegisterPayload, RegisterRole } from '../types/authTypes';

/** Backend khoa khi nhap sai OTP qua so lan cho phep. */
const OTP_LOCKED_HINT = 'quá số lần';

export type OtpMessage = { type: 'error' | 'info'; text: string };

/** Trich xuat thong bao loi tieng Viet tu phan hoi cua backend. */
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

function buildPayload(values: RegisterFormValues, token: string): RegisterPayload {
  return {
    token,
    role: values.role,
    email: values.email.trim(),
    password: values.password,
    confirmPassword: values.confirmPassword,
    displayName: values.displayName.trim(),
    phone: values.phone.trim(),
  };
}

export function useRegister() {
  const [otpSent, setOtpSent] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [otpMessage, setOtpMessage] = useState<OtpMessage | null>(null);
  const [otpLocked, setOtpLocked] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [done, setDone] = useState(false);
  const [doneEmail, setDoneEmail] = useState('');

  const sendOtp = useCallback(async (email: string, role: RegisterRole) => {
    setSending(true);
    setOtpMessage(null);
    try {
      const response = await authApi.sendOtp({ email: email.trim(), role });
      setOtpSent(true);
      setOtpLocked(false);
      setResendCooldown(response.resendCooldownSeconds);
      setOtpMessage({ type: 'info', text: response.message });
      return true;
    } catch (err) {
      setOtpMessage({ type: 'error', text: extractError(err) });
      return false;
    } finally {
      setSending(false);
    }
  }, []);

  const verifyOtp = useCallback(async (email: string, code: string) => {
    setVerifying(true);
    setOtpMessage(null);
    try {
      const response = await authApi.verifyOtp({ email: email.trim(), code });
      setEmailVerified(true);
      setToken(response.verifiedEmailToken);
      setOtpMessage({ type: 'info', text: response.message });
      return true;
    } catch (err) {
      const text = extractError(err);
      if (text.includes(OTP_LOCKED_HINT)) {
        setOtpLocked(true);
      }
      setOtpMessage({ type: 'error', text });
      return false;
    } finally {
      setVerifying(false);
    }
  }, []);

  const register = useCallback(
    async (values: RegisterFormValues) => {
      if (!token) {
        setFormError('Vui lòng xác thực email trước khi đăng ký.');
        return false;
      }
      setSubmitting(true);
      setFormError(null);
      try {
        const response = await authApi.register(buildPayload(values, token));
        setDoneEmail(response.email);
        setDone(true);
        return true;
      } catch (err) {
        setFormError(extractError(err));
        return false;
      } finally {
        setSubmitting(false);
      }
    },
    [token],
  );

  /** Khi nguoi dung sua email sau khi da xac thuc -> huy trang thai da xac thuc. */
  const resetVerification = useCallback(() => {
    setOtpSent(false);
    setEmailVerified(false);
    setToken(null);
    setResendCooldown(0);
    setOtpMessage(null);
    setOtpLocked(false);
  }, []);

  return {
    otpSent,
    emailVerified,
    resendCooldown,
    sending,
    verifying,
    submitting,
    otpMessage,
    otpLocked,
    formError,
    done,
    doneEmail,
    sendOtp,
    verifyOtp,
    register,
    resetVerification,
  };
}
