import { Link } from 'react-router-dom';

import { AdminLayout } from '../components/AdminLayout';

import { usePlatformDashboard } from '../hooks/usePlatformDashboard';

import { useAuth } from '../../../shared/auth/AuthProvider';

import { APP_ROUTES } from '../../../shared/constants/routes';

import { IconChevronRight } from '../components/AdminIcons';
import type { PlatformDashboard } from '../types/platformTypes';
import './PlatformDashboardPage.css';

const formatCount = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

type StatConfig = {
  key: keyof PlatformDashboard;

  label: string;

  tone: 'default' | 'primary' | 'warn';

  icon: string;

};



const STATS: StatConfig[] = [

  { key: 'totalUsers', label: 'Tổng người dùng', tone: 'default', icon: '👥' },

  { key: 'totalTutors', label: 'Gia sư', tone: 'primary', icon: '🎓' },

  { key: 'totalClasses', label: 'Lớp học', tone: 'default', icon: '📚' },

  { key: 'pendingVerifications', label: 'Xác minh chờ duyệt', tone: 'warn', icon: '🛡️' },

  { key: 'openReports', label: 'Báo cáo đang mở', tone: 'warn', icon: '🚩' },

];



export default function PlatformDashboardPage() {

  const { status, data, reload } = usePlatformDashboard();

  const { user } = useAuth();

  const greetingName = user?.displayName?.trim() || user?.email?.split('@')[0] || 'Admin';



  return (

    <AdminLayout

      title="Tổng quan"

      subtitle="Theo dõi nhanh hoạt động nền tảng và truy cập các chức năng quản trị."

    >

      {status === 'loading' && (

        <div className="adm-state adm-state--loading">

          <span className="adm-spinner" aria-hidden="true" />

          Đang tải dữ liệu...

        </div>

      )}



      {status === 'error' && (

        <div className="adm-card adm-error-card">

          <p className="adm-error-card__title">Không tải được dữ liệu dashboard</p>

          <p className="adm-muted">Vui lòng kiểm tra kết nối backend và thử lại.</p>

          <button type="button" className="tcs-btn tcs-btn--primary" onClick={reload}>

            Thử lại

          </button>

        </div>

      )}



      {status === 'success' && data && (

        <>

          <section className="adm-welcome">

            <div className="adm-welcome__content">

              <p className="adm-welcome__eyebrow">Bảng điều khiển quản trị</p>

              <h2 className="adm-welcome__title">Xin chào, {greetingName}</h2>

              <p className="adm-welcome__desc">

                Hôm nay có{' '}

                <strong>{formatCount(data.pendingVerifications)}</strong> hồ sơ xác minh và{' '}

                <strong>{formatCount(data.openReports)}</strong> báo cáo cần theo dõi.

              </p>

            </div>

            <Link className="tcs-btn tcs-btn--primary adm-welcome__cta" to={APP_ROUTES.platformUsers}>

              Quản lý người dùng

            </Link>

          </section>



          <div className="adm-dashboard-stats">

            {STATS.map((stat) => (

              <article

                key={stat.key}

                className={`adm-stat-card adm-stat-card--${stat.tone}`}

              >

                <div className="adm-stat-card__top">

                  <span className="adm-stat-card__icon" aria-hidden="true">

                    {stat.icon}

                  </span>

                </div>

                <p className="adm-stat-card__label">{stat.label}</p>

                <p className="adm-stat-card__value">{formatCount(data[stat.key])}</p>

              </article>

            ))}

          </div>



          <section className="adm-dashboard-section">

            <div className="adm-dashboard-section__head">

              <h2 className="adm-dashboard-section__title">Truy cập nhanh</h2>

              <p className="adm-dashboard-section__hint">Các chức năng quản trị thường dùng</p>

            </div>

            <div className="adm-quick-links">

              <Link className="adm-quick-link adm-quick-link--active" to={APP_ROUTES.platformUsers}>

                <span className="adm-quick-link__icon">👤</span>

                <div className="adm-quick-link__body">

                  <h3 className="adm-quick-link__title">Quản lý người dùng</h3>

                  <p className="adm-quick-link__desc">

                    Xem danh sách, lọc theo vai trò và cập nhật trạng thái tài khoản.

                  </p>

                </div>

                <IconChevronRight className="adm-quick-link__arrow" />

              </Link>



              <span className="adm-quick-link adm-quick-link--disabled">

                <span className="adm-quick-link__icon">🛡️</span>

                <div className="adm-quick-link__body">

                  <h3 className="adm-quick-link__title">Duyệt xác minh</h3>

                  <p className="adm-quick-link__desc">

                    Xử lý hồ sơ xác minh gia sư và trung tâm.

                  </p>

                </div>

                <span className="adm-quick-link__badge">Sắp có</span>

              </span>



              <span className="adm-quick-link adm-quick-link--disabled">

                <span className="adm-quick-link__icon">🚩</span>

                <div className="adm-quick-link__body">

                  <h3 className="adm-quick-link__title">Xử lý báo cáo</h3>

                  <p className="adm-quick-link__desc">

                    Theo dõi và giải quyết báo cáo vi phạm từ người dùng.

                  </p>

                </div>

                <span className="adm-quick-link__badge">Sắp có</span>

              </span>

            </div>

          </section>

        </>

      )}

    </AdminLayout>

  );

}

