import HomePage from '../features/home/pages/HomePage';
import RegisterPage from '../features/auth/pages/RegisterPage';

export default function App() {
  const path = window.location.pathname;

  if (path.startsWith('/register')) {
    return <RegisterPage />;
  }

  return <HomePage />;
}
