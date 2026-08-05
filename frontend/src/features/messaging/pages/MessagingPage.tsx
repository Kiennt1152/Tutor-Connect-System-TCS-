import { Link } from 'react-router-dom';
import { MessagingPanel } from '../components/MessagingPanel';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import '../components/MessagingPanel.css';

type MessagingPageProps = {
  initialTab?: 'tickets';
};

export default function MessagingPage({ initialTab }: MessagingPageProps) {
  const { user } = useAuth();
  void initialTab;

  return (
    <div className="msg-page">
      <nav className="msg-page__topbar">
        <Link to={APP_ROUTES.home} className="msg-page__brand">
          Tutor Connect
        </Link>
        <div className="msg-page__tabs">
          <span className="msg-page__tab msg-page__tab--active">Ho tro</span>
        </div>
        <div className="msg-page__topbar-right">
          {user && (
            <Link to={APP_ROUTES.help} className="tcs-btn tcs-btn--ghost" style={{ fontSize: '0.85rem' }}>
              Help Center
            </Link>
          )}
          <Link to={APP_ROUTES.home} className="tcs-btn tcs-btn--ghost" style={{ fontSize: '0.85rem' }}>
            Home
          </Link>
        </div>
      </nav>
      <div className="msg-page__body">
        <MessagingPanel />
      </div>
    </div>
  );
}
