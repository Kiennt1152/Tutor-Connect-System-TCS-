import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { GraduationCap, Menu, X } from '../../shared/components/icons';
import './PublicLayout.css';

const navItems = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/tutors', label: 'Tìm gia sư' },
  { to: '/about', label: 'Về chúng tôi' },
];

function PublicLayout() {
  const [open, setOpen] = useState(false);

  return (
    <div className="public-shell">
      <header className="public-header">
        <div className="public-header__inner">
          <Link to="/" className="brand" onClick={() => setOpen(false)}>
            <span className="brand__mark">
              <GraduationCap size={22} />
            </span>
            <span className="brand__name">Tutor Connect</span>
          </Link>

          <nav className={`public-nav ${open ? 'is-open' : ''}`}>
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `public-nav__link ${isActive ? 'is-active' : ''}`
                }
                onClick={() => setOpen(false)}
              >
                {item.label}
              </NavLink>
            ))}
            <div className="public-nav__actions">
              <Link to="/login" className="btn btn--ghost" onClick={() => setOpen(false)}>
                Đăng nhập
              </Link>
              <Link to="/register" className="btn btn--primary" onClick={() => setOpen(false)}>
                Đăng ký
              </Link>
            </div>
          </nav>

          <button
            className="public-header__burger"
            aria-label="Mở menu"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </header>

      <main className="public-main">
        <Outlet />
      </main>

      <footer className="public-footer">
        <div className="public-footer__inner">
          <div className="public-footer__brand">
            <span className="brand__mark">
              <GraduationCap size={20} />
            </span>
            <span>Tutor Connect System</span>
          </div>
          <p className="public-footer__copy">
            © 2026 Tutor Connect System — Nền tảng kết nối gia sư.
          </p>
        </div>
      </footer>
    </div>
  );
}

export default PublicLayout;
