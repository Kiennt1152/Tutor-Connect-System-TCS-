import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useLocation } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { StarRating } from '../../reviews/components/StarRating';
import { CriteriaBreakdown } from '../../reviews/components/CriteriaBreakdown';
import { reviewApi } from '../../reviews/api/reviewApi';
import type { ReviewResponse, TutorReputation } from '../../reviews/types/reviewTypes';
import { tutorProfileApi } from '../api/tutorProfileApi';
import type { PublicTutorProfile } from '../types/homeTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './TutorPublicProfilePage.css';

const currency = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) return 'Không tìm thấy gia sư này.';
    if (typeof error.response?.data?.message === 'string') return error.response.data.message;
  }
  return fallback;
}

function initialsOf(name: string): string {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(-2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('') || 'GS'
  );
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('vi-VN');
}

const STAR_ROWS = [5, 4, 3, 2, 1] as const;

type StarFilter = number | 'all';

type ProfileTab = 'profile' | 'reviews';

export default function TutorPublicProfilePage() {
  const location = useLocation();
  // tutorId ẩn khỏi URL — truyền qua router state; dự phòng sessionStorage để refresh vẫn giữ đúng gia sư.
  const stateTutorId = (location.state as { tutorId?: number | string } | null)?.tutorId;
  const tutorId =
    stateTutorId != null
      ? String(stateTutorId)
      : (sessionStorage.getItem('tutor-profile-id') ?? undefined);
  useEffect(() => {
    if (stateTutorId != null) sessionStorage.setItem('tutor-profile-id', String(stateTutorId));
  }, [stateTutorId]);
  const [data, setData] = useState<TutorReputation | null>(null);
  const [profile, setProfile] = useState<PublicTutorProfile | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [starFilter, setStarFilter] = useState<StarFilter>('all');
  // Tab mặc định do nút bấm quyết định (truyền qua router state) — URL giữ nguyên cho cả 2 nút.
  const initialTab: ProfileTab =
    (location.state as { tab?: ProfileTab } | null)?.tab === 'reviews' ? 'reviews' : 'profile';
  const [tab, setTab] = useState<ProfileTab>(initialTab);

  // Đổi tab khi điều hướng lại trang bằng nút khác (router state thay đổi).
  useEffect(() => {
    setTab(initialTab);
  }, [initialTab]);

  const load = useCallback(() => {
    if (!tutorId) return;
    setStatus('loading');
    setStarFilter('all');
    // Hồ sơ chi tiết (học vấn, chứng chỉ, kinh nghiệm) tải song song, không chặn phần đánh giá.
    tutorProfileApi
      .getPublicProfile(tutorId)
      .then((res) => setProfile(res.data))
      .catch(() => setProfile(null));
    reviewApi
      .getTutorReputation(tutorId)
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được hồ sơ gia sư.'));
        setStatus('error');
      });
  }, [tutorId]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main className="tcs-container tp-page">
        <Link to={APP_ROUTES.findTutor} className="tp-back">
          ← Quay lại tìm gia sư
        </Link>

        {status === 'loading' ? <p className="tp-muted">Đang tải hồ sơ…</p> : null}
        {status === 'error' ? (
          <div className="tp-state">
            <p className="tp-error">{error}</p>
            <button type="button" className="tcs-btn tcs-btn--market" onClick={load}>
              Thử lại
            </button>
          </div>
        ) : null}

        {status === 'success' && data ? (
          <>
            <ProfileHeader data={data} />

            <div className="tp-tabs" role="tablist">
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'profile'}
                className={`tp-tab${tab === 'profile' ? ' tp-tab--active' : ''}`}
                onClick={() => setTab('profile')}
              >
                Thông tin hồ sơ
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'reviews'}
                className={`tp-tab${tab === 'reviews' ? ' tp-tab--active' : ''}`}
                onClick={() => setTab('reviews')}
              >
                Đánh giá &amp; danh tiếng ({data.totalReviews})
              </button>
            </div>

            {tab === 'profile' ? (
              profile ? (
                <ProfileInfo profile={profile} />
              ) : (
                <p className="tp-muted">Đang tải thông tin hồ sơ…</p>
              )
            ) : (
              <div className="tp-grid">
                <ReputationSummary
                  data={data}
                  activeStar={starFilter}
                  onSelectStar={setStarFilter}
                />
                <ReviewsList
                  reviews={data.reviews}
                  distribution={data.ratingDistribution}
                  activeStar={starFilter}
                  onSelectStar={setStarFilter}
                />
              </div>
            )}
          </>
        ) : null}
      </main>
      <SiteFooter />
    </div>
  );
}

function ProfileHeader({ data }: { readonly data: TutorReputation }) {
  const verified = data.verificationStatus === 'VERIFIED';
  return (
    <header className="tp-header">
      <div className="tp-header__avatar">
        {data.avatar ? <img src={data.avatar} alt={data.fullName} /> : <span>{initialsOf(data.fullName)}</span>}
      </div>
      <div className="tp-header__main">
        <div className="tp-header__name-row">
          <h1 className="tp-header__name">{data.fullName}</h1>
          {verified ? <span className="tp-verified">✓ Đã xác minh</span> : null}
        </div>
        <div className="tp-header__stats">
          <span className="tp-header__rating">
            <span className="tp-star">★</span> {Number(data.ratingAvg).toFixed(1)}
            <span className="tp-header__count"> ({data.totalReviews} đánh giá)</span>
          </span>
          <span>🎓 {data.experienceYears} năm kinh nghiệm</span>
          <span>💰 {Number(data.hourlyRate) > 0 ? `${currency.format(Number(data.hourlyRate))}đ/giờ` : '—'}</span>
        </div>
        {data.bio?.trim() ? <p className="tp-header__bio">{data.bio.trim()}</p> : null}
      </div>
    </header>
  );
}

function yearRange(start: number | null, end: number | null): string {
  if (!start && !end) return '';
  return `${start ?? '?'} – ${end ?? 'nay'}`;
}

function dateRange(start: string | null, end: string | null): string {
  if (!start && !end) return '';
  return `${start ? formatDate(start) : '?'} – ${end ? formatDate(end) : 'nay'}`;
}

function ProfileInfo({ profile }: { readonly profile: PublicTutorProfile }) {
  const { experiences, educations, certificates } = profile;
  const isEmpty =
    experiences.length === 0 && educations.length === 0 && certificates.length === 0;

  return (
    <section id="chi-tiet-gia-su" className="tp-card tp-profile">
      <h2 className="tp-card__title">Thông tin hồ sơ</h2>

      {isEmpty ? (
        <p className="tp-muted">Gia sư chưa cập nhật học vấn, chứng chỉ hay kinh nghiệm.</p>
      ) : null}

      {experiences.length > 0 ? (
        <div className="tp-profile__block">
          <h3 className="tp-profile__subtitle">💼 Kinh nghiệm</h3>
          <ul className="tp-profile__list">
            {experiences.map((exp) => (
              <li key={exp.experienceId} className="tp-profile__item">
                <div className="tp-profile__item-head">
                  <span className="tp-profile__item-title">{exp.role || 'Vị trí'}</span>
                  {dateRange(exp.startDate, exp.endDate) ? (
                    <span className="tp-profile__item-meta">{dateRange(exp.startDate, exp.endDate)}</span>
                  ) : null}
                </div>
                {exp.organization ? (
                  <div className="tp-profile__item-sub">{exp.organization}</div>
                ) : null}
                {exp.description ? (
                  <p className="tp-profile__item-desc">{exp.description}</p>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {educations.length > 0 ? (
        <div className="tp-profile__block">
          <h3 className="tp-profile__subtitle">🎓 Học vấn</h3>
          <ul className="tp-profile__list">
            {educations.map((edu) => (
              <li key={edu.educationId} className="tp-profile__item">
                <div className="tp-profile__item-head">
                  <span className="tp-profile__item-title">{edu.institution || 'Cơ sở đào tạo'}</span>
                  {yearRange(edu.startYear, edu.endYear) ? (
                    <span className="tp-profile__item-meta">{yearRange(edu.startYear, edu.endYear)}</span>
                  ) : null}
                </div>
                {(edu.degree || edu.fieldOfStudy) ? (
                  <div className="tp-profile__item-sub">
                    {[edu.degree, edu.fieldOfStudy].filter(Boolean).join(' · ')}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {certificates.length > 0 ? (
        <div className="tp-profile__block">
          <h3 className="tp-profile__subtitle">📜 Chứng chỉ</h3>
          <ul className="tp-profile__list">
            {certificates.map((cert) => (
              <li key={cert.certificateId} className="tp-profile__item">
                <div className="tp-profile__item-head">
                  <span className="tp-profile__item-title">{cert.name || 'Chứng chỉ'}</span>
                  {cert.issueDate ? (
                    <span className="tp-profile__item-meta">{formatDate(cert.issueDate)}</span>
                  ) : null}
                </div>
                {cert.issuer ? <div className="tp-profile__item-sub">{cert.issuer}</div> : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}

function ReputationSummary({
  data,
  activeStar,
  onSelectStar,
}: {
  readonly data: TutorReputation;
  readonly activeStar: StarFilter;
  readonly onSelectStar: (star: StarFilter) => void;
}) {
  const total = data.totalReviews;
  return (
    <section className="tp-card tp-summary">
      <h2 className="tp-card__title">Danh tiếng</h2>

      <div className="tp-summary__overall">
        <div className="tp-summary__score">{Number(data.ratingAvg).toFixed(1)}</div>
        <div>
          <StarRating value={Math.round(Number(data.ratingAvg))} readOnly size={20} />
          <p className="tp-muted tp-summary__count">{total} lượt đánh giá</p>
        </div>
      </div>

      <ul className="tp-dist">
        {STAR_ROWS.map((star) => {
          const count = data.ratingDistribution?.[String(star)] ?? 0;
          const pct = total > 0 ? Math.round((count / total) * 100) : 0;
          const active = activeStar === star;
          return (
            <li key={star}>
              <button
                type="button"
                className={`tp-dist__row tp-dist__row--btn${active ? ' tp-dist__row--active' : ''}`}
                onClick={() => onSelectStar(active ? 'all' : star)}
                aria-pressed={active}
                title={`Lọc đánh giá ${star} sao`}
              >
                <span className="tp-dist__star">{star}★</span>
                <span className="tp-dist__bar">
                  <span className="tp-dist__fill" style={{ width: `${pct}%` }} />
                </span>
                <span className="tp-dist__count">{count}</span>
              </button>
            </li>
          );
        })}
      </ul>

      {data.criteriaAverages.length > 0 ? (
        <div className="tp-criteria">
          <h3 className="tp-criteria__title">Điểm theo tiêu chí</h3>
          <ul className="tp-criteria__list">
            {data.criteriaAverages.map((c) => (
              <li key={c.code} className="tp-criteria__row">
                <span className="tp-criteria__q">{c.question ?? c.code}</span>
                <span className="tp-criteria__val">
                  <span className="tp-star">★</span> {Number(c.average).toFixed(1)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}

function ReviewsList({
  reviews,
  distribution,
  activeStar,
  onSelectStar,
}: {
  readonly reviews: ReviewResponse[];
  readonly distribution: Record<string, number>;
  readonly activeStar: StarFilter;
  readonly onSelectStar: (star: StarFilter) => void;
}) {
  const filtered =
    activeStar === 'all'
      ? reviews
      : reviews.filter((r) => Math.round(r.rating) === activeStar);

  return (
    <section className="tp-card tp-reviews">
      <h2 className="tp-card__title">Đánh giá từ học viên ({filtered.length})</h2>

      {reviews.length > 0 ? (
        <div className="tp-filter" role="group" aria-label="Lọc theo số sao">
          <button
            type="button"
            className={`tp-filter__chip${activeStar === 'all' ? ' tp-filter__chip--active' : ''}`}
            onClick={() => onSelectStar('all')}
          >
            Tất cả ({reviews.length})
          </button>
          {STAR_ROWS.map((star) => (
            <button
              key={star}
              type="button"
              className={`tp-filter__chip${activeStar === star ? ' tp-filter__chip--active' : ''}`}
              onClick={() => onSelectStar(star)}
            >
              {star}★ ({distribution?.[String(star)] ?? 0})
            </button>
          ))}
        </div>
      ) : null}

      {reviews.length === 0 ? (
        <p className="tp-muted">Gia sư chưa có đánh giá công khai nào.</p>
      ) : filtered.length === 0 ? (
        <p className="tp-muted">Chưa có đánh giá {activeStar} sao.</p>
      ) : (
        <ul className="tp-reviews__list">
          {filtered.map((r) => (
            <li key={r.reviewId} className="tp-review">
              <div className="tp-review__head">
                <span className="tp-review__author">{r.reviewerDisplayName}</span>
                <span className="tp-review__date">{formatDate(r.createdAt)}</span>
              </div>
              <div className="tp-review__meta">
                <StarRating value={Math.round(r.rating)} readOnly size={16} />
                <span className="tp-review__overall">{r.rating.toFixed(1)}/5</span>
                {r.classTitle ? (
                  <span className="tp-review__class">
                    {r.classTitle}
                    {r.subjectName ? ` · ${r.subjectName}` : ''}
                  </span>
                ) : null}
              </div>
              <CriteriaBreakdown criteriaJson={r.criteriaJson} />
              {r.comment ? <p className="tp-review__comment">“{r.comment}”</p> : null}
              {r.tutorReply ? (
                <div className="tp-reply">
                  <div className="tp-reply__head">
                    <span className="tp-reply__label">↩ Phản hồi của gia sư</span>
                    {r.tutorReplyAt ? (
                      <span className="tp-reply__date">{formatDate(r.tutorReplyAt)}</span>
                    ) : null}
                  </div>
                  <p className="tp-reply__text">{r.tutorReply}</p>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
