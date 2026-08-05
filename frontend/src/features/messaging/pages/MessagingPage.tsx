import { MessagingPanel } from '../components/MessagingPanel';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import '../components/MessagingPanel.css';

type MessagingPageProps = {
  initialTab?: 'tickets';
};

export default function MessagingPage({ initialTab }: MessagingPageProps) {
  void initialTab;

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="msg-page">
        <div className="msg-page__body">
          <MessagingPanel />
        </div>
      </div>
    </div>
  );
}
