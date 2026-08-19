import { Link } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminEscrowQueue } from '../components/AdminEscrowQueue';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './PlatformEscrowPage.css';

export default function PlatformEscrowPage() {
  return (
    <AdminLayout
      title="Giao dịch escrow"
      subtitle="Theo dõi toàn bộ khoản escrow trong hệ thống. Quyết định chia tiền được xử lý tại màn Báo cáo & tranh chấp."
    >
      <section className="adm-card pe-info">
        <div>
          <p className="pe-info__eyebrow">Theo dõi tài chính</p>
          <h2 className="pe-info__title">Escrow chỉ dùng để quan sát và đối soát</h2>
          <p className="pe-info__desc">
            Admin xem trạng thái ký quỹ, người thanh toán, người nhận và mã tham chiếu tại đây.
            Nếu cần giải ngân, hoàn tiền hoặc chia tỷ lệ sau tranh chấp, hãy xử lý trong màn báo cáo.
          </p>
        </div>
        <Link className="tcs-btn tcs-btn--primary" to={APP_ROUTES.platformReports}>
          Mở báo cáo
        </Link>
      </section>

      <AdminEscrowQueue />
    </AdminLayout>
  );
}
