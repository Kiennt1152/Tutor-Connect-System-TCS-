import { NavLink, useLocation } from 'react-router-dom';
import type { PropsWithChildren } from 'react';
import { APP_ROUTES } from '../shared/constants/routes';
import './AppShell.css';

const NAV_ITEMS = [
  { to: APP_ROUTES.home, label: 'Home' },
  { to: APP_ROUTES.catalog, label: 'Catalog' },
  { to: APP_ROUTES.marketplace, label: 'Marketplace' },
  { to: APP_ROUTES.profile, label: 'Profile' },
  { to: APP_ROUTES.identity, label: 'Identity' },
  { to: APP_ROUTES.verification, label: 'Verification' },
  { to: APP_ROUTES.center, label: 'Center' },
  { to: APP_ROUTES.finance, label: 'Finance' },
  { to: APP_ROUTES.contract, label: 'Contract' },
  { to: APP_ROUTES.messaging, label: 'Messaging' },
  { to: APP_ROUTES.platform, label: 'Platform' },
];

export default function AppShell({ children }: Readonly<PropsWithChildren>) {
  const location = useLocation();
  const isHome = location.pathname === APP_ROUTES.home;

  return (
    <div className="app-shell">
      {!isHome && (
        <header className="app-shell__header">
          <NavLink to={APP_ROUTES.home} className="app-shell__brand">
            Tutor Connect
          </NavLink>
          <nav className="app-shell__nav" aria-label="Primary">
            {NAV_ITEMS.filter((item) => item.to !== APP_ROUTES.home).map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `app-shell__nav-link${isActive ? ' app-shell__nav-link--active' : ''}`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>
      )}
      <main className="app-shell__main">{children}</main>
    </div>
  );
}