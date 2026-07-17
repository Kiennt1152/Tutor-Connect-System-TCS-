import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useOpenRecruitmentPosts } from '../hooks/useOpenRecruitmentPosts';
import { HOME_CENTERS, HOME_PROMO } from '../config/homeContent';
import './HomePage.css';

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

/**
 * Trang "Trung tâm" (tách khỏi trang chủ):
 * - Mọi người: các trung tâm đối tác.
 * - Gia sư: tin tuyển dụng đang mở, bấm "Ứng tuyển" -> trang nộp đơn.
 */
export default function CentersPage() {
  const { user } = useAuth();
  const isTutor = hasRole(user?.role, 'TUTOR');
  const { status: postsStatus, posts, reload: reloadPosts } = useOpenRecruitmentPosts(isTutor);

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main>
        <section className="tcs-section tcs-section--centers">
          <div className="tcs-container">
            <div className="tcs-section-bar">
              <div>
                <h1 className="tcs-section-bar__title">Trung tâm</h1>
                <p className="tcs-section-bar__subtitle">
                  Các trung tâm gia sư đối tác — quy trình tuyển chọn và hỗ trợ chuyên nghiệp.
                </p>
              </div>
            </div>

            {/* Gia sư: tin tuyển dụng đang mở từ các trung tâm. */}
            {isTutor && (
              <div className="tcs-recruit">
                <div className="tcs-section-bar">
                  <div>
                    <h2 className="tcs-recruit__title">Tin tuyển gia sư</h2>
                    <p className="tcs-section-bar__subtitle">
                      Các trung tâm đang tuyển — xem chi tiết và gửi đơn ứng tuyển.
                    </p>
                  </div>
                  {postsStatus === 'success' && posts.length > 0 ? (
                    <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to={APP_ROUTES.recruitment}>
                      Xem tất cả ({posts.length})
                    </Link>
                  ) : null}
                </div>

                {postsStatus === 'loading' && (
                  <div className="tcs-search-results__state">
                    <span className="tcs-spinner" aria-hidden="true" />
                    Đang tải tin tuyển dụng...
                  </div>
                )}

                {postsStatus === 'error' && (
                  <div className="tcs-search-results__state tcs-search-results__state--error">
                    Không thể tải tin tuyển dụng.
                    <button
                      type="button"
                      className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                      onClick={reloadPosts}
                    >
                      Thử lại
                    </button>
                  </div>
                )}

                {postsStatus === 'success' && posts.length === 0 && (
                  <p className="tcs-empty">Hiện chưa có tin tuyển gia sư nào đang mở.</p>
                )}

                {postsStatus === 'success' && posts.length > 0 && (
                  <div className="tcs-recruit__grid">
                    {posts.map((post) => (
                      <article key={post.recruitmentId} className="tcs-recruit-card">
                        <h3 className="tcs-recruit-card__title">{post.title}</h3>
                        <div className="tcs-recruit-card__chips">
                          {post.centerName && <span className="tcs-chip">🏫 {post.centerName}</span>}
                          {post.subjectName && (
                            <span className="tcs-chip">📘 {post.subjectName}</span>
                          )}
                          {post.locationLabel && (
                            <span className="tcs-chip">📍 {post.locationLabel}</span>
                          )}
                          <span className="tcs-chip">👤 {post.maxPositions} vị trí</span>
                        </div>
                        <p className="tcs-recruit-card__desc">{post.description}</p>
                        <Link
                          className="tcs-btn tcs-btn--market tcs-btn--sm"
                          to={APP_ROUTES.recruitment}
                        >
                          Ứng tuyển
                        </Link>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="tcs-promo tcs-promo--inline">
              <div className="tcs-promo__content">
                <span className="tcs-promo__eyebrow">Đối tác nền tảng</span>
                <h2 className="tcs-promo__title">{HOME_PROMO.title}</h2>
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
      </main>
    </div>
  );
}
