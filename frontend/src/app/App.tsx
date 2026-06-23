import HomePage from '../features/home/pages/HomePage';
import LoginPage from '../features/identity/pages/LoginPage';

export default function App() {
  const path = window.location.pathname;

  if (path.startsWith('/login')) {
    return <LoginPage />;
  }

  return <HomePage />;
}
