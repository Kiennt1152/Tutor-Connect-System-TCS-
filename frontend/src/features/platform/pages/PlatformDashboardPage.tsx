import { Link } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { usePlatformDashboard } from '../hooks/usePlatformDashboard';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import {
  ADMIN_OPERATION_MODULES,
  ADMIN_PRIORITY_ITEMS,
  ADMIN_STATS,
} from '../config/adminNavConfig';
import { AdminIcon, IconChevronRight } from '../components/AdminIcons';
import './PlatformDashboardPage.css';

const formatCount = (value: any) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(typeof value === 'number' ? value : 0);

export default function PlatformDashboardPage() {
  const { status, data, reload } = usePlatformDashboard();
  const { user } = useAuth();
  const greetingName = user?.displayName?.trim() || user?.email?.split('@')[0] || 'Admin';

  return (
    <AdminLayout
      title="Bảng điều khiển"
      subtitle="Theo dõi số liệu nền tảng và xử lý các tác vụ vận hành."
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
          <button type="button" className="tcs-btn tcs-btn--market" onClick={reload}>
            Thử lại
          </button>
        </div>
      )}

      {status === 'success' && data && (
        <>
          <section className="adm-welcome">
            <div className="adm-welcome__content">
              <p className="adm-welcome__eyebrow">Tutor Connect System · Admin</p>
              <h2 className="adm-welcome__title">Xin chào, {greetingName}</h2>
              <p className="adm-welcome__desc">
                Hôm nay có <strong>{formatCount(data.pendingVerifications)}</strong> hồ sơ xác minh và{' '}
                <strong>{formatCount(data.openReports)}</strong> báo cáo cần theo dõi.
              </p>
            </div>
            <div className="adm-welcome__actions">
              {data.pendingVerifications > 0 ? (
                <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.platformVerifications}>
                  Xử lý xác minh
                </Link>
              ) : null}
              {data.openReports > 0 ? (
                <Link className="tcs-btn tcs-btn--ghost" to={APP_ROUTES.platformReports}>
                  Xem báo cáo
                </Link>
              ) : null}
            </div>
          </section>

          {data.alerts && data.alerts.length > 0 && (
            <section className="adm-alerts-section">
              <h2 className="adm-alerts-section__title">Cảnh báo hệ thống</h2>
              <div className="adm-alerts-grid">
                {data.alerts.map((alert, idx) => (
                  <div key={idx} className={`adm-alert-card adm-alert-card--${alert.type.toLowerCase()}`}>
                    <div className="adm-alert-card__body">
                      <h3 className="adm-alert-card__title">{alert.title}</h3>
                      <p className="adm-alert-card__message">{alert.message}</p>
                    </div>
                    {alert.actionUrl && (
                      <Link to={alert.actionUrl} className="tcs-btn tcs-btn--sm tcs-btn--outline">
                        Xử lý ngay →
                      </Link>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {(data.pendingVerifications > 0 || data.openReports > 0 || (data.openDisputes && data.openDisputes > 0) || (data.pendingWithdrawals && data.pendingWithdrawals > 0) || (data.openTickets && data.openTickets > 0)) && (
            <section className="adm-priority">
              <h2 className="adm-priority__title">Cần xử lý</h2>
              <div className="adm-priority__grid">
                {ADMIN_PRIORITY_ITEMS.map((item) =>
                  data[item.key] && data[item.key] > 0 ? (
                    <Link key={item.key} className="adm-priority-card" to={item.to}>
                      <span className="adm-priority-card__icon">
                        <AdminIcon name={item.icon} size="lg" />
                      </span>
                      <div>
                        <p className="adm-priority-card__label">{item.label}</p>
                        <p className="adm-priority-card__value">{formatCount(data[item.key])}</p>
                      </div>
                    </Link>
                  ) : null,
                )}
              </div>
            </section>
          )}

          <div className="adm-dashboard-stats">
            {ADMIN_STATS.map((stat) => {
              const card = (
                <article key={stat.key} className={`adm-stat-card adm-stat-card--${stat.tone}`}>
                  <div className="adm-stat-card__top">
                    <span className="adm-stat-card__icon" aria-hidden="true">
                      <AdminIcon name={stat.icon} size="md" />
                    </span>
                  </div>
                  <p className="adm-stat-card__label">{stat.label}</p>
                  <p className="adm-stat-card__value">{formatCount(data[stat.key] || 0)}</p>
                </article>
              );
              return stat.to ? (
                <Link key={stat.key} to={stat.to} className="adm-stat-card-link">
                  {card}
                </Link>
              ) : (
                card
              );
            })}
          </div>

          <section className="adm-revenue-banner">
            <div className="adm-revenue-card">
              <span className="adm-revenue-card__label">Tổng giá trị giao dịch sàn (GMV)</span>
              <strong className="adm-revenue-card__value">
                {new Intl.NumberFormat('vi-VN').format(data.totalRevenue || 0)} VND
              </strong>
            </div>
            <div className="adm-revenue-card adm-revenue-card--highlight">
              <span className="adm-revenue-card__label">Doanh thu phí dịch vụ sàn (10%)</span>
              <strong className="adm-revenue-card__value">
                {new Intl.NumberFormat('vi-VN').format(data.platformFeeRevenue || 0)} VND
              </strong>
            </div>
          </section>

          <section className="adm-dashboard-section">
            <div className="adm-dashboard-section__head">
              <h2 className="adm-dashboard-section__title">Chức năng quản trị</h2>
              <p className="adm-dashboard-section__hint">Truy cập nhanh các module vận hành</p>
            </div>
            <div className="adm-quick-links">
              {ADMIN_OPERATION_MODULES.map((link) => (
                <Link key={link.to} className="adm-quick-link adm-quick-link--active" to={link.to}>
                  <span className="adm-quick-link__icon">
                    <AdminIcon name={link.icon} size="md" />
                  </span>
                  <div className="adm-quick-link__body">
                    <h3 className="adm-quick-link__title">{link.title}</h3>
                    <p className="adm-quick-link__desc">{link.description}</p>
                  </div>
                  <IconChevronRight className="adm-quick-link__arrow" />
                </Link>
              ))}
            </div>
          </section>
        </>
      )}
    </AdminLayout>
  );
}