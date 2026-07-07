import { SiteHeader } from '../../../shared/components/SiteHeader';
import { FinancePanel } from '../components/FinancePanel';
import './FinancePage.css';

export default function FinancePage() {
  return (
    <div className="fin-page">
      <SiteHeader />
      <main className="fin-page__main">
        <h1 className="fin-page__title">Ví của tôi</h1>
        <FinancePanel />
      </main>
    </div>
  );
}
