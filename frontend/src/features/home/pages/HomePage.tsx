import { useMemo } from 'react';
import { AppLogo } from '../../../shared/components/AppLogo';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasAnyRole, hasRole } from '../../../shared/auth/rbac';
import type { UserRole } from '../../../shared/types/userRole';
import { TutorSearchBlock } from '../components/TutorSearchBlock';
import { ClassSearchBlock } from '../components/ClassSearchBlock';
import { TutorListingCard } from '../components/TutorListingCard';
import { ClassListingCard } from '../components/ClassListingCard';
import { getAuthenticatedHeroCopy } from '../config/homeQuickActions';
import { useOpenClasses } from '../hooks/useOpenClasses';
import { usePublicAnnouncements } from '../../platform/hooks/useAnnouncements';
import {
  FOOTER_LINKS,
  HOME_CENTERS,
  HOME_NEWS,
  HOME_PROMO,
  HOME_TESTIMONIALS,
} from '../config/homeContent';
import type { FeaturedTutor, HomeData, SubjectItem } from '../types/homeTypes';
import type { OpenClassItem } from '../types/openClassTypes';
import type { OpenClassesStatus } from '../hooks/useOpenClasses';
import AdminHomePage from './AdminHomePage';
import './HomePage.css';

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

const MARKETPLACE_HOME_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'UNKNOWN'];

function HomeHeroSection({
  data,
  subjects,
  openClasses,
  classesStatus,
  isAuthenticated,
  displayName,
  role,
}: {
  data: HomeData | null;
  subjects: SubjectItem[];
  openClasses: OpenClassItem[];
  classesStatus: OpenClassesStatus;
  isAuthenticated: boolean;
  displayName?: string;
  role?: string;
}) {
  const copy = role ? getAuthenticatedHeroCopy(role) : null;
  const firstName = displayName?.trim().split(/\s+/)[0] || displayName;
  const showSearch = !isAuthenticated || hasAnyRole(role, MARKETPLACE_HOME_ROLES);
  const isTutor = hasRole(role, 'TUTOR');
  const subjectLinkTarget = isTutor ? '#classes' : '#find-tutor';

  return (
    <section className="tcs-home-hero">
      <div className="tcs-container">
        <div className="tcs-hero__panel">
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
                  Tìm gia sư theo môn học và khu vực — quy trình minh bạch, thanh toán an toàn qua ký
                  quỹ.
                </p>
                <HeroStats data={data} />
              </>
            )}
          </div>

          {showSearch ? (
            isTutor ? (
              <ClassSearchBlock
                subjects={subjects}
                classes={openClasses}
                classesStatus={classesStatus}
                isAuthenticated={isAuthenticated}
              />
            ) : (
              <TutorSearchBlock subjects={subjects} isAuthenticated={isAuthenticated} />
            )
          ) : null}
        </div>

        {subjects.length > 0 ? (
          <div className="tcs-hero__subjects">
            <p className="tcs-hero__subjects-label">
              {isTutor ? 'Môn học đang có lớp mở' : 'Môn học phổ biến'}
            </p>
            <div className="tcs-chips">
              {subjects.map((subject) => (
                <a key={subject.id} href={subjectLinkTarget} className="tcs-chip">
                  {subject.name}
                </a>
              ))}
            </div>
          </div>
        ) : null}
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
    <section id="find-tutor" className="tcs-section tcs-section--tutors">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Tìm gia sư</h2>
            <p className="tcs-section-bar__subtitle">
              Lọc theo môn học, khu vực và xem gia sư phù hợp ngay trên nền tảng.
            </p>
          </div>
          {tutors.length > 0 ? (
            <span className="tcs-section-bar__count">{tutors.length} gia sư</span>
          ) : null}
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
        <span className="tcs-stat__value">{data ? currency(data.totalTutors) : '—'}</span>
        <span className="tcs-stat__label">Gia sư</span>
      </div>
      <div className="tcs-stat">
        <span className="tcs-stat__value">{data ? currency(data.totalSubjects) : '—'}</span>
        <span className="tcs-stat__label">Môn học</span>
      </div>
      <div className="tcs-stat">
        <span className="tcs-stat__value">{data ? currency(data.totalClasses) : '—'}</span>
        <span className="tcs-stat__label">Lớp học</span>
      </div>
    </div>
  );
}

function ClassesSection({
  classes,
  status,
  isAuthenticated,
  onRetry,
}: {
  classes: OpenClassItem[];
  status: 'loading' | 'success' | 'error';
  isAuthenticated: boolean;
  onRetry: () => void;
}) {
  return (
    <section id="classes" className="tcs-section tcs-section--listing">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Tìm lớp</h2>
            <p className="tcs-section-bar__subtitle">
              Các lớp học đang mở — học viên có thể đăng ký hoặc gia sư có thể ứng tuyển.
            </p>
          </div>
          {status === 'success' && classes.length > 0 ? (
            <span className="tcs-section-bar__count">{classes.length} lớp</span>
          ) : null}
        </div>

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
  return (
    <section id="centers" className="tcs-section tcs-section--centers">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Trung tâm</h2>
            <p className="tcs-section-bar__subtitle">
              Các trung tâm gia sư đối tác — quy trình tuyển chọn và hỗ trợ chuyên nghiệp.
            </p>
          </div>
        </div>

        <div className="tcs-promo tcs-promo--inline">
          <div className="tcs-promo__content">
            <span className="tcs-promo__eyebrow">Đối tác nền tảng</span>
            <h3 className="tcs-promo__title">{HOME_PROMO.title}</h3>
            <p className="tcs-promo__desc">{HOME_PROMO.description}</p>
          </div>
          <a className="tcs-btn tcs-btn--market tcs-promo__cta" href={HOME_PROMO.ctaHref}>
            {HOME_PROMO.cta}
          </a>
        </div>

        <div className="tcs-center-grid">
          {HOME_CENTERS.map((center) => (
            <article key={center.id} className="tcs-center-card">
              <h3 className="tcs-center-card__name">{center.name}</h3>
              <p className="tcs-center-card__desc">{center.description}</p>
              <span className="tcs-center-card__meta">{currency(center.tutors)} gia sư</span>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function NewsSection() {
  return (
    <section id="news" className="tcs-section tcs-section--news">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Tin tức</h2>
            <p className="tcs-section-bar__subtitle">
              Cập nhật mới về giáo dục, gia sư và hoạt động trên nền tảng.
            </p>
          </div>
        </div>
        <div className="tcs-news-grid">
          {HOME_NEWS.map((item) => (
            <article key={item.id} className="tcs-news-card">
              <time className="tcs-news-card__date">{item.date}</time>
              <h3 className="tcs-news-card__title">{item.title}</h3>
              <p className="tcs-news-card__excerpt">{item.excerpt}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function ReviewsSection() {
  return (
    <section id="reviews" className="tcs-section tcs-section--testimonials">
      <div className="tcs-container">
        <div className="tcs-section-bar">
          <div>
            <h2 className="tcs-section-bar__title">Đánh giá</h2>
            <p className="tcs-section-bar__subtitle">
              Trải nghiệm thực tế từ phụ huynh, gia sư và trung tâm trên Tutor Connect System.
            </p>
          </div>
        </div>
        <div className="tcs-testimonial-grid">
          {HOME_TESTIMONIALS.map((item) => (
            <blockquote key={item.author} className="tcs-testimonial">
              <p className="tcs-testimonial__quote">“{item.quote}”</p>
              <footer className="tcs-testimonial__author">
                <strong>{item.author}</strong>
                <span>{item.role}</span>
              </footer>
            </blockquote>
          ))}
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="tcs-footer">
      <div className="tcs-container">
        <div className="tcs-footer__grid">
          <div className="tcs-footer__brand">
            <AppLogo href="/" variant="compact" />
            <p className="tcs-footer__tagline">
              Nền tảng kết nối gia sư — học viên — trung tâm với quy trình minh bạch và thanh toán an
              toàn.
            </p>
          </div>
          {FOOTER_LINKS.map((group) => (
            <div key={group.title} className="tcs-footer__col">
              <h3 className="tcs-footer__heading">{group.title}</h3>
              <ul className="tcs-footer__links">
                {group.links.map((link) => (
                  <li key={link.label}>
                    <a href={link.href}>{link.label}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="tcs-footer__bottom">
          <span>© {new Date().getFullYear()} Tutor Connect System</span>
          <span className="tcs-footer__muted">SEP490 · TCS</span>
        </div>
      </div>
    </footer>
  );
}

function LoadingState() {
  return (
    <div className="tcs-state">
      <div className="tcs-spinner" aria-hidden />
      <p>Đang tải dữ liệu trang chủ…</p>
    </div>
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
  const {
    status: classesStatus,
    classes: openClasses,
    reload: reloadClasses,
  } = useOpenClasses();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const isEmpty = useMemo(
    () =>
      status === 'success' &&
      data !== null &&
      data.featuredTutors.length === 0 &&
      data.subjects.length === 0,
    [status, data],
  );

  if (hasRole(user?.role, 'PLATFORM_ADMIN')) {
    return <AdminHomePage />;
  }

  const displayName = user?.displayName?.trim() || user?.email || 'bạn';
  const role = user?.role ?? 'UNKNOWN';

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main>
        <HomeHeroSection
          data={data}
          subjects={data?.subjects ?? []}
          openClasses={openClasses}
          classesStatus={classesStatus}
          isAuthenticated={isAuthenticated}
          displayName={isAuthenticated ? displayName : undefined}
          role={isAuthenticated ? role : undefined}
        />

        {status === 'loading' && <LoadingState />}
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

        <ClassesSection
          classes={openClasses}
          status={classesStatus}
          isAuthenticated={isAuthenticated}
          onRetry={reloadClasses}
        />
        <CentersSection />
        <NewsSection />
        <ReviewsSection />
      </main>
      <Footer />
    </div>
  );
}

export default HomePage;
