import { BrowserRouter, Route, Routes } from 'react-router-dom';
import HomePage from '../features/home/pages/HomePage';
import LoginPage from '../features/identity/pages/LoginPage';
import RegisterPage from '../features/identity/pages/RegisterPage';
import PlatformDashboardPage from '../features/platform/pages/PlatformDashboardPage';
import PlatformProfilePage from '../features/platform/pages/PlatformProfilePage';
import PlatformUsersPage from '../features/platform/pages/PlatformUsersPage';
import { ProtectedRoute } from '../shared/auth/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/platform"
          element={
            <ProtectedRoute roles={['PLATFORM_ADMIN']}>
              <PlatformDashboardPage />
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
        <Route
          path="/platform/profile"
          element={
            <ProtectedRoute roles={['PLATFORM_ADMIN']}>
              <PlatformProfilePage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}
