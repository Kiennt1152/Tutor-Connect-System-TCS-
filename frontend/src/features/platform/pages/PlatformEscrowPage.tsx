/**
 * ====================================================================================================
 * [UC-58] MÀN HÌNH QUẢN TRỊ KÝ QUỸ BẢO CHỨNG ESCROW (PLATFORM ESCROW PAGE)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Giám sát toàn bộ các khoản ký quỹ đang phong tỏa (HELD) và đang tranh chấp (DISPUTED).
 * 2. Can thiệp giải phóng hoặc hoàn tiền khẩn cấp theo quyết định hòa giải.
 * 3. Đối soát số dư tài khoản bảo chứng đảm bảo tính toàn vẹn tài chính toàn sàn.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 * @author Hoàng Minh Đức (mduc1011-swp)
 */
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
