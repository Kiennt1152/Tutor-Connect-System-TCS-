import { BrowserRouter, Route, Routes } from 'react-router-dom';
import HomePage from '../features/home/pages/HomePage';
import CatalogPage from '../features/catalog/pages/CatalogPage';
import LoginPage from '../features/identity/pages/LoginPage';
import RegisterPage from '../features/identity/pages/RegisterPage';
import VerificationPage from '../features/identity/pages/VerificationPage';
import PlatformDashboardPage from '../features/platform/pages/PlatformDashboardPage';
import PlatformProfilePage from '../features/platform/pages/PlatformProfilePage';
import PlatformReportsPage from '../features/platform/pages/PlatformReportsPage';
import PlatformUsersPage from '../features/platform/pages/PlatformUsersPage';
import PlatformVerificationsPage from '../features/platform/pages/PlatformVerificationsPage';
import PlatformTicketsPage from '../features/platform/pages/PlatformTicketsPage';
import PlatformFaqPage from '../features/platform/pages/PlatformFaqPage';
import PlatformParametersPage from '../features/platform/pages/PlatformParametersPage';
import PlatformPenaltiesPage from '../features/platform/pages/PlatformPenaltiesPage';
import PlatformAuditLogsPage from '../features/platform/pages/PlatformAuditLogsPage';
import PlatformAnnouncementsPage from '../features/platform/pages/PlatformAnnouncementsPage';
import CenterPage from '../features/center/pages/CenterPage';
import CenterSchedulePage from '../features/center/pages/CenterSchedulePage';
import CenterReschedulesPage from '../features/center/pages/CenterReschedulesPage';
import TutorSchedulePage from '../features/tutor/pages/TutorSchedulePage';
import TutorAttendancePage from '../features/tutor/pages/TutorAttendancePage';
import ProfilePage from '../features/profile/pages/ProfilePage';
import FinancePage from '../features/finance/pages/FinancePage';
import MarketplacePage from '../features/marketplace/pages/MarketplacePage';
import MarketplaceClassDetailPage from '../features/marketplace/pages/MarketplaceClassDetailPage';
import ContractPage from '../features/contract/pages/ContractPage';
import MessagingPage from '../features/messaging/pages/MessagingPage';
import HelpPage from '../features/help/pages/HelpPage';
import ForbiddenPage from '../shared/pages/ForbiddenPage';
import { ProtectedRoute } from '../shared/auth/ProtectedRoute';
import { ErrorBoundary } from '../shared/components/ErrorBoundary';
import { APP_ROUTES } from '../shared/constants/routes';
import { lazy, Suspense } from 'react';

const PlatformTasksPage = lazy(() => import('../features/platform/pages/PlatformTasksPage'));
const PlatformAnalyticsPage = lazy(() => import('../features/platform/pages/PlatformAnalyticsPage'));
const AiAssistantPage = lazy(() => import('../features/ai/pages/AiAssistantPage'));
import AiFloatingWidget from '../features/ai/components/AiFloatingWidget';

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path={APP_ROUTES.home} element={<HomePage />} />
          <Route path={APP_ROUTES.login} element={<LoginPage />} />
          <Route path={APP_ROUTES.register} element={<RegisterPage />} />
          <Route path={APP_ROUTES.forbidden} element={<ForbiddenPage />} />
          <Route path={APP_ROUTES.catalog} element={<CatalogPage />} />
          <Route
            path={APP_ROUTES.verification}
            element={
              <ProtectedRoute roles={['TUTOR', 'TUTOR_CENTER']}>
                <VerificationPage />
              </ProtectedRoute>
            }
          />

          <Route
            path={APP_ROUTES.profile}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN']}>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.finance}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <FinancePage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.marketplace}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <MarketplacePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/marketplace/classes/:classId"
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <MarketplaceClassDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.contract}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <ContractPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.messaging}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN']}>
                <MessagingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.center}
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/center/schedule"
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterSchedulePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/center/reschedules"
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterReschedulesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tutor/schedule"
            element={
              <ProtectedRoute roles={['TUTOR']}>
                <TutorSchedulePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tutor/classes/:classId/attendance"
            element={
              <ProtectedRoute roles={['TUTOR']}>
                <TutorAttendancePage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platform}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformTasks}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <Suspense fallback={<div>Loading...</div>}>
                  <PlatformTasksPage />
                </Suspense>
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformAnalytics}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <Suspense fallback={<div>Loading...</div>}>
                  <PlatformAnalyticsPage />
                </Suspense>
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformUsers}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformUsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformVerifications}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformVerificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformReports}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformReportsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformTickets}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformTicketsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformFaq}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformFaqPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformParameters}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformParametersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformAnnouncements}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformAnnouncementsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformPenalties}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformPenaltiesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformAuditLogs}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformAuditLogsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformProfile}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformProfilePage />
              </ProtectedRoute>
            }
          />
          <Route path={APP_ROUTES.help} element={<HelpPage />} />
          <Route path={APP_ROUTES.aiAssistant} element={<Suspense fallback={<div style={{ padding: '2rem', textAlign: 'center', color: '#8b949e' }}>⏳ Đang tải trợ lý AI...</div>}><AiAssistantPage /></Suspense>} />
          <Route
            path={APP_ROUTES.messagingTickets}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <MessagingPage initialTab="tickets" />
              </ProtectedRoute>
            }
          />
        </Routes>
        <AiFloatingWidget />
      </BrowserRouter>
    </ErrorBoundary>
  );
}
