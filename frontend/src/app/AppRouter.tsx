import { Routes, Route, Navigate } from 'react-router-dom';
import HomePage from '../features/home/pages/HomePage';
import CatalogPage from '../features/catalog/pages/CatalogPage';
import IdentityPage from '../features/identity/pages/IdentityPage';
import VerificationPage from '../features/identity/pages/VerificationPage';
import ProfilePage from '../features/profile/pages/ProfilePage';
import FinancePage from '../features/finance/pages/FinancePage';
import CenterPage from '../features/center/pages/CenterPage';
import MarketplacePage from '../features/marketplace/pages/MarketplacePage';
import ContractPage from '../features/contract/pages/ContractPage';
import MessagingPage from '../features/messaging/pages/MessagingPage';
import PlatformPage from '../features/platform/pages/PlatformPage';
import ForbiddenPage from '../shared/pages/ForbiddenPage';
import { APP_ROUTES } from '../shared/constants/routes';
import { AccessGuard, getRouteAccess } from '../shared/auth/routeAccess';

function guard(path: string, element: React.ReactNode) {
  return <AccessGuard access={getRouteAccess(path)}>{element}</AccessGuard>;
}

export default function AppRouter() {
  return (
    <Routes>
      <Route path={APP_ROUTES.home} element={guard(APP_ROUTES.home, <HomePage />)} />
      <Route path={APP_ROUTES.catalog} element={guard(APP_ROUTES.catalog, <CatalogPage />)} />
      <Route path={APP_ROUTES.identity} element={guard(APP_ROUTES.identity, <IdentityPage />)} />
      <Route
        path={APP_ROUTES.verification}
        element={guard(APP_ROUTES.verification, <VerificationPage />)}
      />
      <Route path={APP_ROUTES.profile} element={guard(APP_ROUTES.profile, <ProfilePage />)} />
      <Route path={APP_ROUTES.finance} element={guard(APP_ROUTES.finance, <FinancePage />)} />
      <Route path={APP_ROUTES.center} element={guard(APP_ROUTES.center, <CenterPage />)} />
      <Route
        path={APP_ROUTES.marketplace}
        element={guard(APP_ROUTES.marketplace, <MarketplacePage />)}
      />
      <Route path={APP_ROUTES.contract} element={guard(APP_ROUTES.contract, <ContractPage />)} />
      <Route
        path={APP_ROUTES.messaging}
        element={guard(APP_ROUTES.messaging, <MessagingPage />)}
      />
      <Route path={APP_ROUTES.platform} element={guard(APP_ROUTES.platform, <PlatformPage />)} />
      <Route path={APP_ROUTES.forbidden} element={<ForbiddenPage />} />
      <Route path="*" element={<Navigate to={APP_ROUTES.home} replace />} />
    </Routes>
  );
}