import { MessagingPanel } from '../components/MessagingPanel';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';

export default function TicketsPage() {
  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="msg-page" style={{ minHeight: 'calc(100vh - 70px)' }}>
        <div className="msg-page__body">
          <MessagingPanel />
        </div>
      </div>
    </div>
  );
}
