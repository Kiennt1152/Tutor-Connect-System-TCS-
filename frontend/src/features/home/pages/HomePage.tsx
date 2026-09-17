import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { centerProfilePath } from '../../../shared/constants/routes';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { ClassListingCard } from '../components/ClassListingCard';
import { getAuthenticatedHeroCopy } from '../config/homeQuickActions';
import { useReveal } from '../hooks/useReveal';
import { useCardSpotlight } from '../hooks/useCardSpotlight';
import { CountUp } from '../components/CountUp';
import { TutorListSkeleton } from '../components/HomeSkeleton';
import { HeroSlideshow } from '../components/HeroSlideshow';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type { CenterSummary } from '../../marketplace/types/marketplaceTypes';
import type { FeaturedTutor, HomeData } from '../types/homeTypes';
import type { OpenClassItem } from '../types/openClassTypes';
import AdminHomePage from './AdminHomePage';
import './HomePage.css';

function HomeHeroSection({
  data,
  isAuthenticated,
  displayName,
  role,
}: {
  data: HomeData | null;
  isAuthenticated: boolean;
  displayName?: string;
  role?: string;
}) {
  const copy = role ? getAuthenticatedHeroCopy(role) : null;
  const firstName = displayName?.trim().split(/\s+/)[0] || displayName;

  return (
    <section className="tcs-home-hero">
      <div className="tcs-container">
        <div className="tcs-hero__panel">
          <div className="tcs-hero__col">
            <div className="tcs-hero__intro">
              {isAuthenticated && copy ? (
                <>
                  <p className="tcs-hero__eyebrow">{copy.eyebrow}</p>
                  <h1 className="tcs-hero__title">Xin chào, {firstName}</h1>
                  <p className="tcs-hero__subtitle">{copy.subtitle}</p>
                </>
              ) : (
                <>
                  <h1 className="tcs-hero__title">Kết nối gia sư uy tín</h1>
                  <p className="tcs-hero__subtitle">
                    Tìm gia sư theo môn học và khu vực — quy trình minh bạch, thanh toán an toàn qua
                    ký quỹ.
                  </p>
                  <HeroStats data={data} />
                </>
              )}
            </div>
          </div>

          <HeroSlideshow />
        </div>
      </div>
    </section>
  );
}

function TutorListSection({
  tutors,
  isAuthenticated,
}: {
  tutors: FeaturedTutor[];
  isAuthenticated: boolean;
}) {
  return (
    <section id="find-tutor" className="tcs-section tcs-section--tutors" data-reveal>
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Tìm gia sư</h2>
            <p className="tcs-section-bar__subtitle">
              Lọc theo môn học, khu vực và xem gia sư phù hợp ngay trên nền tảng.
            </p>
          </div>
        </div>

        {tutors.length === 0 ? (
          <p className="tcs-empty">Chưa có gia sư nào để hiển thị.</p>
        ) : (
          <div className="tcs-listing-grid">
            {tutors.map((tutor) => (
              <TutorListingCard key={tutor.id} tutor={tutor} isAuthenticated={isAuthenticated} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function HeroStats({ data }: { data: HomeData | null }) {
  return (
    <div className="tcs-hero__stats">
      <div className="tcs-stat">
        <span className="tcs-stat__value">
          <CountUp value={data ? data.totalTutors : null} />
        </span>
        <span className="tcs-stat__label">Gia sư</span>
      </div>
      <div className="tcs-stat">
        <span className="tcs-stat__value">
          <CountUp value={data ? data.totalSubjects : null} />
        </span>
        <span className="tcs-stat__label">Môn học</span>
      </div>
      <div className="tcs-stat">
        <span className="tcs-stat__value">
          <CountUp value={data ? data.totalClasses : null} />
        </span>
        <span className="tcs-stat__label">Lớp học</span>
      </div>
    </div>
  );
}

/** Khối "lớp đang mở" trên trang chủ. */
export function ClassesSection({
  classes,
  status,
  isAuthenticated,
  onRetry,
  hideHeading = false,
}: {
  classes: OpenClassItem[];
  status: 'loading' | 'success' | 'error';
  isAuthenticated: boolean;
  onRetry: () => void;
  hideHeading?: boolean;
}) {
  return (
    <section id="classes" className="tcs-section tcs-section--listing" data-reveal>
      <div className="tcs-container">
        {(!hideHeading || (status === 'success' && classes.length > 0)) && (
          <div className="tcs-section-bar">
            {hideHeading ? (
              <div />
            ) : (
              <div>
                <h2 className="tcs-section-bar__title">Tìm lớp</h2>
                <p className="tcs-section-bar__subtitle">
                  Các lớp học đang mở — học viên có thể đăng ký hoặc gia sư có thể ứng tuyển.
                </p>
              </div>
            )}
            {status === 'success' && classes.length > 0 ? (
              <span className="tcs-section-bar__count">{classes.length} lớp</span>
            ) : null}
          </div>
        )}

        {status === 'loading' && (
          <div className="tcs-search-results__state">
            <span className="tcs-spinner" aria-hidden="true" />
            Đang tải danh sách lớp...
          </div>
        )}

        {status === 'error' && (
          <div className="tcs-search-results__state tcs-search-results__state--error">
            Không thể tải danh sách lớp.
            <button type="button" className="tcs-btn tcs-btn--ghost tcs-btn--sm" onClick={onRetry}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && classes.length === 0 && (
          <p className="tcs-empty">Hiện chưa có lớp học nào đang mở.</p>
        )}

        {status === 'success' && classes.length > 0 && (
          <div className="tcs-class-list">
            {classes.map((classItem) => (
              <ClassListingCard
                key={classItem.id}
                classItem={classItem}
                isAuthenticated={isAuthenticated}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function CentersSection() {
  // Lấy trung tâm THẬT đã xác minh (không dùng dữ liệu mẫu bịa sẵn).
  const [centers, setCenters] = useState<CenterSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .listCenters()
      .then((res) => {
        if (alive) setCenters(res.data);
      })
      .catch(() => {
        if (alive) setCenters([]);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);

  const featured = centers.slice(0, 3);

  return (
    <section id="centers" className="tcs-section tcs-section--centers" data-reveal>
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Trung tâm</h2>
            <p className="tcs-section-bar__subtitle">
              Các trung tâm gia sư đã được xác minh trên nền tảng.
            </p>
          </div>
          {centers.length > 3 && (
            <a className="tcs-btn tcs-btn--ghost tcs-btn--sm" href="/centers">
              Xem tất cả
            </a>
          )}
        </div>

        {loading ? (
          <p className="tcs-section-bar__subtitle">Đang tải danh sách trung tâm…</p>
        ) : featured.length === 0 ? (
          <p className="tcs-section-bar__subtitle">
            Chưa có trung tâm nào được xác minh. Vui lòng quay lại sau.
          </p>
        ) : (
          <div className="tcs-center-grid">
            {featured.map((center) => (
              <article key={center.centerId} className="tcs-center-card">
                <h3 className="tcs-center-card__name">
                  <Link
                    className="tcs-center-card__link"
                    to={centerProfilePath()}
                    state={{ centerId: center.centerId }}
                  >
                    {center.companyName}
                  </Link>
                </h3>
                {center.description && (
                  <p className="tcs-center-card__desc">{center.description}</p>
                )}
                {center.address && (
                  <span className="tcs-center-card__meta">📍 {center.address}</span>
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="tcs-state">
      <div className="tcs-state__icon tcs-state__icon--error">!</div>
      <p>Không kết nối được máy chủ. Hãy kiểm tra backend đang chạy ở cổng 8080.</p>
      <button className="tcs-btn tcs-btn--market" onClick={onRetry}>
        Thử lại
      </button>
    </div>
  );
}

function HomePage() {
  const { status, data, reload } = useHome();
  const { user, isAuthenticated } = useAuth();
  const isEmpty = useMemo(
    () =>
      status === 'success' &&
      data !== null &&
      data.featuredTutors.length === 0 &&
      data.subjects.length === 0,
    [status, data],
  );

  // Quét lại các section mỗi khi dữ liệu đổi, vì phần lớn chỉ được render sau khi tải xong.
  useReveal([status, data]);
  useCardSpotlight();

  if (hasRole(user?.role, 'PLATFORM_ADMIN')) {
    return <AdminHomePage />;
  }

  const displayName = user?.displayName?.trim() || user?.email || 'bạn';
  const role = user?.role ?? 'UNKNOWN';

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main>
        <HomeHeroSection
          data={data}
          isAuthenticated={isAuthenticated}
          displayName={isAuthenticated ? displayName : undefined}
          role={isAuthenticated ? role : undefined}
        />

        {status === 'loading' && <TutorListSkeleton />}
        {status === 'error' && <ErrorState onRetry={reload} />}

        {status === 'success' && (
          <TutorListSection
            tutors={data?.featuredTutors ?? []}
            isAuthenticated={isAuthenticated}
          />
        )}

        {status === 'success' && data && isEmpty && (
          <div className="tcs-container">
            <p className="tcs-empty tcs-empty--page">
              Chưa có dữ liệu gia sư hoặc môn học. Hãy chạy seed data ở backend.
            </p>
          </div>
        )}

        <CentersSection />
      </main>
      <SiteFooter />
    </div>
  );
}

export default HomePage;
