import { BrowserRouter, Route, Routes } from 'react-router-dom';
import ContractPage from '../features/contract/pages/ContractPage';
import FinancePage from '../features/finance/pages/FinancePage';
import HomePage from '../features/home/pages/HomePage';
import LoginPage from '../features/identity/pages/LoginPage';
import RegisterPage from '../features/identity/pages/RegisterPage';
import DependentProfileLinkerPage from '../features/profile/pages/DependentProfileLinkerPage';
import GuardianApprovalPage from '../features/profile/pages/GuardianApprovalPage';
import PlatformUsersPage from '../features/platform/pages/PlatformUsersPage';
import { DependentProfileGate } from '../shared/auth/DependentProfileGate';
import { ProtectedRoute } from '../shared/auth/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/profile/dependents"
          element={
            <ProtectedRoute roles={['CLIENT']}>
              <DependentProfileLinkerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile/guardian/approvals"
          element={
            <ProtectedRoute roles={['CLIENT']}>
              <GuardianApprovalPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/finance"
          element={
            <ProtectedRoute roles={['CLIENT']}>
              <DependentProfileGate>
                <FinancePage />
              </DependentProfileGate>
            </ProtectedRoute>
          }
        />
        <Route
          path="/contract"
          element={
            <ProtectedRoute roles={['CLIENT']}>
              <DependentProfileGate>
                <ContractPage />
              </DependentProfileGate>
            </ProtectedRoute>
          }
        />
        <Route
          path="/platform/users"
          element={
            <ProtectedRoute roles={['PLATFORM_ADMIN']}>
              <PlatformUsersPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}
