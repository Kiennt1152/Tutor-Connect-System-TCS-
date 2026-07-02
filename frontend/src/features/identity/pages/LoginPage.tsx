import type { FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { imageAssets } from '../../../assets/images/ImageAssets';
import type { RegisterRole } from '../types/identityTypes';
import './RegisterPage.css';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
const GSI_SRC = 'https://accounts.google.com/gsi/client';
// Dau so di dong VN theo nha mang (loai 095, 054, 050... khong ton tai). Chap nhan ca dang 0... va +84...
const PHONE_REGEX = /^(0|\+84)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-46-9])\d{7}$/;

const ROLE_OPTIONS: { value: RegisterRole; label: string }[] = [
  { value: 'CLIENT', label: 'Học viên / Phụ huynh' },
  { value: 'TUTOR', label: 'Gia sư' },
  { value: 'TUTOR_CENTER', label: 'Trung tâm' },
];

type TokenResponse = { access_token?: string; error?: string };
type TokenClient = { requestAccessToken: () => void };
type GoogleOAuth2 = {
  initTokenClient: (config: {
    client_id: string;
    scope: string;
    callback: (response: TokenResponse) => void;
  }) => TokenClient;
};
type GoogleGsi = { accounts: { oauth2: GoogleOAuth2 } };

/** Nap script Google Identity Services mot lan (idempotent). */
function loadGsiScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${GSI_SRC}"]`)) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = GSI_SRC;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Không tải được Google Identity Services'));
    document.head.appendChild(script);
  });
}

function Header() {
  return (
    <header className="reg-header">
      <div className="reg-header__inner">
        <Link to="/" className="reg-logo" aria-label="Tutor Connect System">
          <img className="reg-logo__image" src={imageAssets.logo} alt="" />
          <span className="reg-logo__text">Tutor Connect System</span>
        </Link>
        <Link to="/" className="reg-header__login">
          Trang chủ
        </Link>
      </div>
    </header>
  );
}

function EyeIcon({ off }: { off: boolean }) {
  const common = {
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
  };
  return off ? (
    <svg {...common}>
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
    <svg {...common}>
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18z"
      />
      <path fill="#FBBC05" d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3.01-2.33z" />
      <path
        fill="#EA4335"
        d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.47.9 11.43 0 9 0A9 9 0 0 0 .96 4.95l3.01 2.33C4.68 5.16 6.66 3.58 9 3.58z"
      />
    </svg>
  );
}

export default function LoginPage() {
  const { login, loginWithGoogle, completeGoogleSignup, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const tokenClientRef = useRef<TokenClient | null>(null);

  // Ho so con thieu (role + SDT) khi Google login lan dau chua co tai khoan.
  const [googlePending, setGooglePending] = useState<{ accessToken: string; email: string } | null>(
    null,
  );
  const [completeRole, setCompleteRole] = useState<RegisterRole>('CLIENT');
  const [completePhone, setCompletePhone] = useState('');
  const [completeSubmitting, setCompleteSubmitting] = useState(false);

  // Khoi tao Google OAuth2 token client (luong popup) cho nut Google tuy chinh.
  function getTokenClient(): TokenClient {
    if (tokenClientRef.current) {
      return tokenClientRef.current;
    }
    const google = (window as unknown as { google?: GoogleGsi }).google;
    if (!google) {
      throw new Error('Google Identity Services chưa sẵn sàng.');
    }
    tokenClientRef.current = google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID as string,
      scope: 'openid email profile',
      callback: async (response) => {
        if (response.error || !response.access_token) {
          setError('Đăng nhập Google bị hủy hoặc thất bại.');
          return;
        }
        setError('');
        setLoading(true);
        try {
          const result = await loginWithGoogle({ accessToken: response.access_token });
          if (result.newUser) {
            // Tai khoan Google chua ton tai -> yeu cau chon vai tro + SDT truoc khi tao tai khoan.
            setGooglePending({ accessToken: response.access_token as string, email: result.email });
          } else {
            navigate(from, { replace: true });
          }
        } catch {
          setError('Đăng nhập Google thất bại. Vui lòng thử lại.');
        } finally {
          setLoading(false);
        }
      },
    });
    return tokenClientRef.current;
  }

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) {
      return;
    }
    loadGsiScript()
      .then(() => getTokenClient())
      .catch(() => {
        // Chua tai xong luc mount thi de nguoi dung bam nut se tu thu lai (handleGoogleClick).
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleGoogleClick() {
    if (!GOOGLE_CLIENT_ID) {
      setError('Đăng nhập Google chưa được cấu hình (thiếu VITE_GOOGLE_CLIENT_ID).');
      return;
    }
    setError('');
    if (tokenClientRef.current) {
      tokenClientRef.current.requestAccessToken();
      return;
    }
    setGoogleLoading(true);
    try {
      await loadGsiScript();
      getTokenClient().requestAccessToken();
    } catch {
      setError('Không tải được đăng nhập Google. Vui lòng kiểm tra kết nối mạng và thử lại.');
    } finally {
      setGoogleLoading(false);
    }
  }

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login({ email, password });
      navigate(from, { replace: true });
    } catch {
      setError('Email hoặc mật khẩu không đúng');
    } finally {
      setLoading(false);
    }
  }

  async function handleCompleteGoogleSignup(e: FormEvent) {
    e.preventDefault();
    if (!googlePending) {
      return;
    }
    setError('');
    if (!completePhone.trim()) {
      setError('Vui lòng nhập số điện thoại');
      return;
    }
    if (!PHONE_REGEX.test(completePhone.trim())) {
      setError('Số điện thoại không hợp lệ');
      return;
    }
    setCompleteSubmitting(true);
    try {
      await completeGoogleSignup({
        accessToken: googlePending.accessToken,
        role: completeRole,
        phone: completePhone.trim(),
      });
      navigate(from, { replace: true });
    } catch {
      setError('Hoàn tất đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setCompleteSubmitting(false);
    }
  }

  if (googlePending) {
    return (
      <div className="reg-page">
        <Header />
        <main className="reg-main">
          <section className="reg-card reg-card--narrow">
            <form onSubmit={handleCompleteGoogleSignup} noValidate>
              <div className="reg-card__head">
                <h1 className="reg-card__title">Hoàn tất đăng ký</h1>
                <p className="reg-card__sub">
                  Đây là lần đầu <strong>{googlePending.email}</strong> đăng nhập bằng Google. Chọn loại
                  tài khoản và nhập số điện thoại để tiếp tục.
                </p>
              </div>

              <div className="reg-roles">
                {ROLE_OPTIONS.map((o) => (
                  <button
                    type="button"
                    key={o.value}
                    className={`reg-role${completeRole === o.value ? ' reg-role--active' : ''}`}
                    onClick={() => setCompleteRole(o.value)}
                  >
                    <span className="reg-role__label">{o.label}</span>
                  </button>
                ))}
              </div>

              <label className="reg-field">
                <span className="reg-label">Số điện thoại</span>
                <input
                  className="reg-input"
                  value={completePhone}
                  onChange={(e) => setCompletePhone(e.target.value)}
                  placeholder="0901234567"
                  autoComplete="tel"
                  required
                />
              </label>

              {error && <div className="reg-alert reg-alert--error">{error}</div>}

              <button type="submit" className="reg-btn reg-btn--block" disabled={completeSubmitting}>
                {completeSubmitting ? 'Đang xử lý…' : 'Hoàn tất đăng ký'}
              </button>

              <p className="reg-foot">
                <button
                  type="button"
                  className="reg-link"
                  style={{ background: 'none', border: 'none', padding: 0, font: 'inherit', cursor: 'pointer' }}
                  onClick={() => {
                    setGooglePending(null);
                    setError('');
                  }}
                >
                  Quay lại
                </button>
              </p>
            </form>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="reg-page">
      <Header />
      <main className="reg-main">
        <section className="reg-card reg-card--narrow">
          <form onSubmit={handleSubmit} noValidate>
            <div className="reg-card__head">
              <h1 className="reg-card__title">Đăng nhập</h1>
              <p className="reg-card__sub">Chào mừng bạn quay lại Tutor Connect System.</p>
            </div>

            <label className="reg-field">
              <span className="reg-label">Email</span>
              <input
                className="reg-input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ban@email.com"
                autoComplete="email"
                required
              />
            </label>

            <label className="reg-field">
              <span className="reg-label">Mật khẩu</span>
              <div className="reg-input-wrap">
                <input
                  className="reg-input reg-input--toggle"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="reg-eye"
                  tabIndex={-1}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  onClick={() => setShowPassword((s) => !s)}
                >
                  <EyeIcon off={showPassword} />
                </button>
              </div>
            </label>

            {error && <div className="reg-alert reg-alert--error">{error}</div>}

            <button type="submit" className="reg-btn reg-btn--block" disabled={loading}>
              {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
            </button>

            <div className="reg-divider">
              <span>hoặc</span>
            </div>

            <button
              type="button"
              className="reg-btn reg-btn--ghost reg-btn--block reg-google-fallback"
              onClick={handleGoogleClick}
              disabled={loading || googleLoading}
            >
              <GoogleIcon />
              {googleLoading ? 'Đang tải Google…' : 'Đăng nhập với Google'}
            </button>

            <p className="reg-foot">
              Chưa có tài khoản?{' '}
              <Link to="/register" className="reg-link">
                Đăng ký
              </Link>
            </p>
          </form>
        </section>
      </main>
    </div>
  );
}
