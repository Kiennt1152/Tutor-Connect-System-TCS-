import { Link } from 'react-router-dom';
import { tutorProfilePath } from '../../../shared/constants/routes';
import type { FeaturedTutor } from '../types/homeTypes';

type TutorListingCardProps = {
  tutor: FeaturedTutor;
  isAuthenticated: boolean;
  variant?: 'grid' | 'search';
  showPrice?: boolean;
};

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');

const bioSnippet = (bio: string | null) => {
  const text = bio?.trim();
  if (!text) return 'Gia sư tận tâm, sẵn sàng đồng hành cùng học viên trên nền tảng TCS.';
  return text.length > 120 ? `${text.slice(0, 120)}…` : text;
};

export function TutorListingCard({
  tutor,
  variant = 'grid',
  showPrice = true,
}: TutorListingCardProps) {
  return (
    <article className={`tcs-listing-card${variant === 'search' ? ' tcs-listing-card--compact' : ''}`}>
      <div className="tcs-listing-card__top">
        <div className="tcs-listing-card__profile">
          <div className="tcs-listing-card__avatar">{initials(tutor.fullName) || 'GS'}</div>
          <div className="tcs-listing-card__identity">
            <h3 className="tcs-listing-card__name">{tutor.fullName}</h3>
            <div className="tcs-listing-card__badges">
              <span className="tcs-listing-card__badge">Gia sư</span>
              {tutor.verificationStatus === 'VERIFIED' ? (
                <span className="tcs-listing-card__badge tcs-listing-card__badge--verified" title="Hồ sơ đã được xác minh">
                  ✓ Đã xác minh
                </span>
              ) : null}
            </div>
          </div>
        </div>
        {showPrice ? (
          <div className="tcs-listing-card__price">
            <span className="tcs-listing-card__price-value">{currency(tutor.hourlyRate)} đ</span>
            <span className="tcs-listing-card__price-unit">/giờ</span>
          </div>
        ) : null}
      </div>

      <div className="tcs-listing-card__meta">
        <div className="tcs-listing-card__row">
          <span className="tcs-listing-card__label">Đánh giá</span>
          <span className="tcs-listing-card__value">
            <span className="tcs-listing-card__star">★</span> {Number(tutor.ratingAvg).toFixed(1)}
          </span>
        </div>
        <div className="tcs-listing-card__row">
          <span className="tcs-listing-card__label">Kinh nghiệm</span>
          <span className="tcs-listing-card__value">{tutor.experienceYears} năm</span>
        </div>
        {tutor.gender ? (
          <div className="tcs-listing-card__row">
            <span className="tcs-listing-card__label">Giới tính</span>
            <span className="tcs-listing-card__value">{tutor.gender}</span>
          </div>
        ) : null}
      </div>

      <p className="tcs-listing-card__bio">{bioSnippet(tutor.bio)}</p>

      <div className="tcs-listing-card__foot">
        <span className="tcs-listing-card__status">Sẵn sàng nhận lớp</span>
        <div className="tcs-listing-card__actions">
          <Link
            className="tcs-btn tcs-btn--ghost tcs-listing-card__review"
            to={tutorProfilePath()}
            state={{ tab: 'reviews', tutorId: tutor.id }}
          >
            Xem đánh giá
          </Link>
          <Link
            className="tcs-btn tcs-btn--market"
            to={tutorProfilePath()}
            state={{ tab: 'profile', tutorId: tutor.id }}
          >
            Xem hồ sơ
          </Link>
        </div>
      </div>
    </article>
  );
}
