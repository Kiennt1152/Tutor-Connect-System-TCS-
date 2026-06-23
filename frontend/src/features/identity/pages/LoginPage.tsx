import { useState, type FormEvent } from 'react';
import { useIdentity } from '../hooks/useIdentity';
import './LoginPage.css';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { user, loading, error, login, logout } = useIdentity();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await login({ email, password });
      window.location.href = '/';
    } catch {
      // Lỗi đã được hook lưu vào `error`, không cần xử lý thêm
    }
  };

  if (user) {
    return (
      <div className="auth-wrap">
        <div className="auth-card">
          <h1 className="auth-title">Xin chào 👋</h1>
          <p className="auth-sub">
            Bạn đã đăng nhập với <strong>{user.email}</strong>
          </p>
          <a className="auth-btn" href="/">
            Về trang chủ
          </a>
          <button className="auth-btn auth-btn--ghost" type="button" onClick={logout}>
            Đăng xuất
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={handleSubmit}>
        <a className="auth-logo" href="/">
          <span className="auth-logo__mark">TC</span> Tutor Connect
        </a>
        <h1 className="auth-title">Đăng nhập</h1>

        <label className="auth-label" htmlFor="email">
          Email
        </label>
        <input
          id="email"
          className="auth-input"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="ban@example.com"
          autoComplete="email"
          required
        />

        <label className="auth-label" htmlFor="password">
          Mật khẩu
        </label>
        <input
          id="password"
          className="auth-input"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Nhập mật khẩu"
          autoComplete="current-password"
          required
        />

        {error && <p className="auth-error">{error}</p>}

        <button className="auth-btn" type="submit" disabled={loading}>
          {loading ? 'Đang xử lý…' : 'Đăng nhập'}
        </button>
      </form>
    </div>
  );
}
