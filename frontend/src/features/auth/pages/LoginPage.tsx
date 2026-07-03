import { useState, type FormEvent } from 'react';
import { useLogin } from '../hooks/useLogin';
import type { LoginFormValues } from '../types/authTypes';
import './RegisterPage.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/** Phat hien ky tu ngoai ASCII in duoc (vd: dau tieng Viet, khoang trang). */
const NON_ASCII_REGEX = /[^\x21-\x7E]/;

type FieldErrors = Partial<Record<keyof LoginFormValues, string>>;

function validate(values: LoginFormValues): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.email.trim()) {
    errors.email = 'Vui lòng nhập email';
  } else if (!EMAIL_REGEX.test(values.email.trim())) {
    errors.email = 'Email không hợp lệ';
  }
  if (!values.password) {
    errors.password = 'Vui lòng nhập mật khẩu';
  } else if (NON_ASCII_REGEX.test(values.password)) {
    errors.password = 'Mật khẩu chỉ gồm chữ, số và ký tự đặc biệt (không dấu, không khoảng trắng)';
  }
  return errors;
}

function Header() {
  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <a className="tcs-logo" href="/">
          <span className="tcs-logo__mark">TC</span>
          <span className="tcs-logo__text">Tutor Connect</span>
        </a>
        <div className="tcs-header__actions">
          <a className="tcs-btn tcs-btn--ghost" href="/register">
            Đăng ký
          </a>
        </div>
      </div>
    </header>
  );
}

function EyeIcon({ off }: { off: boolean }) {
  return off ? (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export default function LoginPage() {
  const { submitting, formError, login } = useLogin();
  const [values, setValues] = useState<LoginFormValues>({ email: '', password: '' });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const update = (name: keyof LoginFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [name]: value }));
    if (touched) {
      setErrors(validate({ ...values, [name]: value }));
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    const found = validate(values);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      return;
    }
    const result = await login(values);
    if (result) {
      window.location.href = '/';
    }
  };

  return (
    <div className="tcs-page auth-page">
      <Header />
      <main className="auth-main">
        <form className="auth-card auth-card--narrow" onSubmit={handleSubmit} noValidate>
          <div className="auth-card__head">
            <h1 className="auth-card__title">Đăng nhập</h1>
          </div>

          <div className="auth-field auth-field--full">
            <input
              id="email"
              className={`auth-input ${touched && errors.email ? 'auth-input--error' : ''}`}
              type="email"
              value={values.email}
              onChange={(e) => update('email', e.target.value)}
              placeholder="Nhập email của bạn"
              autoComplete="email"
            />
            {touched && errors.email && <span className="auth-field__error">{errors.email}</span>}
          </div>

          <div className="auth-field auth-field--full">
            <div className="auth-input-wrap">
              <input
                id="password"
                className={`auth-input auth-input--with-toggle ${
                  touched && errors.password ? 'auth-input--error' : ''
                }`}
                type={showPassword ? 'text' : 'password'}
                value={values.password}
                onChange={(e) => update('password', e.target.value)}
                placeholder="Nhập mật khẩu"
                autoComplete="current-password"
              />
              <button
                className="auth-eye"
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              >
                <EyeIcon off={showPassword} />
              </button>
            </div>
            {touched && errors.password && (
              <span className="auth-field__error">{errors.password}</span>
            )}
          </div>

          {formError && <div className="auth-alert auth-alert--error">{formError}</div>}

          <button className="tcs-btn tcs-btn--primary auth-submit" type="submit" disabled={submitting}>
            {submitting ? 'Đang xử lý…' : 'Đăng nhập'}
          </button>

          <div className="auth-foot">
            Chưa có tài khoản?{' '}
            <a className="auth-link" href="/register">
              Đăng ký ngay
            </a>
          </div>
        </form>
      </main>
    </div>
  );
}
