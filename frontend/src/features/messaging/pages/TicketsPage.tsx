import { MessagingPanel } from '../components/MessagingPanel';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';

export default function TicketsPage() {
  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div style={{ maxWidth: '1200px', margin: '2rem auto', padding: '0 1rem' }}>
        <MessagingPanel />
      </div>
    </div>
  );
}
