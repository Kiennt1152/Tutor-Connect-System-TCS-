import { BrowserRouter, Route, Routes } from 'react-router-dom';
import HomePage from '../features/home/pages/HomePage';
import FindTutorPage from '../features/home/pages/FindTutorPage';
import PostTutorRequestPage from '../features/home/pages/PostTutorRequestPage';
import FindClassPage from '../features/home/pages/FindClassPage';
import TutorPublicProfilePage from '../features/home/pages/TutorPublicProfilePage';
import TutorReviewsPage from '../features/home/pages/TutorReviewsPage';
import LoginPage from '../features/identity/pages/LoginPage';
import RegisterPage from '../features/identity/pages/RegisterPage';
import ForgotPasswordPage from '../features/identity/pages/ForgotPasswordPage';
import ResetPasswordPage from '../features/identity/pages/ResetPasswordPage';
import VerificationPage from '../features/identity/pages/VerificationPage';
import PlatformDashboardPage from '../features/platform/pages/PlatformDashboardPage';
import PlatformEscrowPage from '../features/platform/pages/PlatformEscrowPage';
import PlatformProfilePage from '../features/platform/pages/PlatformProfilePage';
import PlatformReportsPage from '../features/platform/pages/PlatformReportsPage';
import PlatformReviewsPage from '../features/platform/pages/PlatformReviewsPage';
import PlatformUsersPage from '../features/platform/pages/PlatformUsersPage';
import PlatformVerificationsPage from '../features/platform/pages/PlatformVerificationsPage';
import PlatformWithdrawalsPage from '../features/platform/pages/PlatformWithdrawalsPage';
import PlatformTicketsPage from '../features/platform/pages/PlatformTicketsPage';
import PlatformFaqPage from '../features/platform/pages/PlatformFaqPage';
import PlatformParametersPage from '../features/platform/pages/PlatformParametersPage';
import PlatformPenaltiesPage from '../features/platform/pages/PlatformPenaltiesPage';
import PlatformAuditLogsPage from '../features/platform/pages/PlatformAuditLogsPage';
import PlatformAnnouncementsPage from '../features/platform/pages/PlatformAnnouncementsPage';
import PlatformNotificationTemplatesPage from '../features/platform/pages/PlatformNotificationTemplatesPage';
import PlatformCircumventionPage from '../features/platform/pages/PlatformCircumventionPage';
import CenterPage from '../features/center/pages/CenterPage';
import CenterReportsPage from '../features/center/pages/CenterReportsPage';
import CenterRecruitmentPage from '../features/center/pages/CenterRecruitmentPage';
import CenterSchedulePage from '../features/center/pages/CenterSchedulePage';
import CenterReschedulesPage from '../features/center/pages/CenterReschedulesPage';
import TutorSchedulePage from '../features/tutor/pages/TutorSchedulePage';
import TutorAttendancePage from '../features/tutor/pages/TutorAttendancePage';
import CenterTutorsPage from '../features/center/pages/CenterTutorsPage';
import CenterContractTemplatesPage from '../features/center/pages/CenterContractTemplatesPage';
import CentersPage from '../features/home/pages/CentersPage';
import RecruitmentPage from '../features/recruitment/pages/RecruitmentPage';
import ProfilePage from '../features/profile/pages/ProfilePage';
import DependentProfileLinkerPage from '../features/profile/pages/DependentProfileLinkerPage';
import ChildProfileDetailPage from '../features/profile/pages/ChildProfileDetailPage';
import GuardianApprovalPage from '../features/profile/pages/GuardianApprovalPage';
import FinancePage from '../features/finance/pages/FinancePage';
import MarketplacePage from '../features/marketplace/pages/MarketplacePage';
import ClassFinderPage from '../features/marketplace/pages/ClassFinderPage';
import MarketplaceClassDetailPage from '../features/marketplace/pages/MarketplaceClassDetailPage';
import CatalogPage from '../features/catalog/pages/CatalogPage';
import ContractListPage from '../features/contract/pages/ContractListPage';
import ContractDetailPage from '../features/contract/pages/ContractDetailPage';
import MessagingPage from '../features/messaging/pages/MessagingPage';
import TicketsPage from '../features/messaging/pages/TicketsPage';
import MyReviewsPage from '../features/reviews/pages/MyReviewsPage';
import MyReputationPage from '../features/reviews/pages/MyReputationPage';
import TeachingPage from '../features/teaching/pages/TeachingPage';
import ContractSigningPage from '../features/teaching/pages/ContractSigningPage';
import HelpPage from '../features/help/pages/HelpPage';
import ForbiddenPage from '../shared/pages/ForbiddenPage';
import { ProtectedRoute } from '../shared/auth/ProtectedRoute';
import { ErrorBoundary } from '../shared/components/ErrorBoundary';
import { APP_ROUTES } from '../shared/constants/routes';
import { lazy, Suspense } from 'react';
import { ScrollToHash } from './ScrollToHash';

const PlatformTasksPage = lazy(() => import('../features/platform/pages/PlatformTasksPage'));
const PlatformAnalyticsPage = lazy(() => import('../features/platform/pages/PlatformAnalyticsPage'));
const AiAssistantPage = lazy(() => import('../features/ai/pages/AiAssistantPage'));
import AiFloatingWidget from '../features/ai/components/AiFloatingWidget';

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <ScrollToHash />
        <Routes>
          <Route path={APP_ROUTES.home} element={<HomePage />} />
          <Route path={APP_ROUTES.findTutor} element={<FindTutorPage />} />
          <Route path={APP_ROUTES.postTutorRequest} element={<PostTutorRequestPage />} />
          <Route path={APP_ROUTES.findClass} element={<FindClassPage />} />
          <Route path={APP_ROUTES.classFinder} element={<ClassFinderPage />} />
          <Route path={APP_ROUTES.tutorProfile} element={<TutorPublicProfilePage />} />
          <Route path={APP_ROUTES.tutorReviews} element={<TutorReviewsPage />} />
          <Route path={APP_ROUTES.login} element={<LoginPage />} />
          <Route path={APP_ROUTES.register} element={<RegisterPage />} />
          <Route path={APP_ROUTES.forgotPassword} element={<ForgotPasswordPage />} />
          <Route path={APP_ROUTES.resetPassword} element={<ResetPasswordPage />} />
          <Route path={APP_ROUTES.forbidden} element={<ForbiddenPage />} />
          <Route
            path={APP_ROUTES.catalog}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <CatalogPage />
              </ProtectedRoute>
            }
          />
          {/* Trang "Trung tâm" cong khai: ai cung xem duoc; gia su thay them tin tuyen dung. */}
          <Route path={APP_ROUTES.centers} element={<CentersPage />} />
          <Route
            path={APP_ROUTES.verification}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <VerificationPage />
              </ProtectedRoute>
            }
          />

          <Route
            path={APP_ROUTES.teaching}
            element={
              <ProtectedRoute roles={['TUTOR', 'CLIENT']}>
                <TeachingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.signContract}
            element={
              <ProtectedRoute roles={['TUTOR', 'CLIENT']}>
                <ContractSigningPage />
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
            path={APP_ROUTES.profileDependents}
            element={
              <ProtectedRoute roles={['CLIENT']}>
                <DependentProfileLinkerPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.childProfile(':childProfileId')}
            element={
              <ProtectedRoute roles={['CLIENT']}>
                <ChildProfileDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.guardianApprovals}
            element={
              <ProtectedRoute roles={['CLIENT']}>
                <GuardianApprovalPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.finance}
            element={
              <ProtectedRoute roles={['TUTOR', 'TUTOR_CENTER']}>
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
                <ContractListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={`${APP_ROUTES.contract}/:contractId`}
            element={
              <ProtectedRoute roles={['CLIENT', 'TUTOR', 'TUTOR_CENTER']}>
                <ContractDetailPage />
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
            path={APP_ROUTES.feedback}
            element={
              <ProtectedRoute roles={['CLIENT']}>
                <MyReviewsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.myReputation}
            element={
              <ProtectedRoute roles={['TUTOR']}>
                <MyReputationPage />
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
            path={APP_ROUTES.centerReports}
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterReportsPage />
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
            path="/center/recruitment"
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterRecruitmentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/center/tutors"
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterTutorsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/center/contract-templates"
            element={
              <ProtectedRoute roles={['TUTOR_CENTER']}>
                <CenterContractTemplatesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.recruitment}
            element={
              <ProtectedRoute roles={['TUTOR']}>
                <RecruitmentPage />
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
            path={APP_ROUTES.platformEscrows}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformEscrowPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformWithdrawals}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformWithdrawalsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformReviews}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformReviewsPage />
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
            path={APP_ROUTES.platformNotificationTemplates}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformNotificationTemplatesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={APP_ROUTES.platformCircumvention}
            element={
              <ProtectedRoute roles={['PLATFORM_ADMIN']}>
                <PlatformCircumventionPage />
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
                <TicketsPage />
              </ProtectedRoute>
            }
          />
        </Routes>
        <AiFloatingWidget />
      </BrowserRouter>
    </ErrorBoundary>
  );
}
