import HomePage from '../features/home/pages/HomePage';
import RegisterPage from '../features/auth/pages/RegisterPage';
import LoginPage from '../features/auth/pages/LoginPage';

export default function App() {
  const path = window.location.pathname;

  if (path.startsWith('/register')) {
    return <RegisterPage />;
  }

  if (path.startsWith('/login')) {
    return <LoginPage />;
  }

  return <HomePage />;
}
