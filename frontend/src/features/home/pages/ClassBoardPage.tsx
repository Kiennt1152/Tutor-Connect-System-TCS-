import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { useOpenClasses } from '../hooks/useOpenClasses';
import { ClassesSection } from './HomePage';
import './HomePage.css';

/**
 * Danh sách lớp (/danh-sach-lop): tổng hợp mọi lớp do client đăng và đang mở.
 * Khi hai bên ký thỏa thuận hợp đồng xong -> lớp được kích hoạt (rời trạng thái OPEN)
 * nên tự động biến mất khỏi danh sách này.
 */
export default function ClassBoardPage() {
  const { isAuthenticated } = useAuth();
  const { status, classes, reload } = useOpenClasses();

  return (
    <div className="tcs-page tcs-classboard">
      <SiteHeader />
      <main>
        <section className="tcs-home-hero">
          <div className="tcs-container">
            <div className="tcs-hero__panel">
              <h1 className="tcs-find-title" style={{ marginBottom: 0 }}>
                <span className="tcs-find-title__icon">📋</span>
                <span className="tcs-find-title__text">Danh sách lớp đã đăng</span>
              </h1>
            </div>
          </div>
        </section>
        <ClassesSection
          classes={classes}
          status={status}
          isAuthenticated={isAuthenticated}
          onRetry={reload}
          hideHeading
        />
      </main>
      <SiteFooter />
    </div>
  );
}
