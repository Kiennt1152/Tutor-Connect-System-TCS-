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
import { APP_ROUTES } from '../shared/constants/routes';

export default function AppRouter() {
  return (
    <Routes>
      <Route path={APP_ROUTES.home} element={<HomePage />} />
      <Route path={APP_ROUTES.catalog} element={<CatalogPage />} />
      <Route path={APP_ROUTES.identity} element={<IdentityPage />} />
      <Route path={APP_ROUTES.verification} element={<VerificationPage />} />
      <Route path={APP_ROUTES.profile} element={<ProfilePage />} />
      <Route path={APP_ROUTES.finance} element={<FinancePage />} />
      <Route path={APP_ROUTES.center} element={<CenterPage />} />
      <Route path={APP_ROUTES.marketplace} element={<MarketplacePage />} />
      <Route path={APP_ROUTES.contract} element={<ContractPage />} />
      <Route path={APP_ROUTES.messaging} element={<MessagingPage />} />
      <Route path={APP_ROUTES.platform} element={<PlatformPage />} />
      <Route path="*" element={<Navigate to={APP_ROUTES.home} replace />} />
    </Routes>
  );
}