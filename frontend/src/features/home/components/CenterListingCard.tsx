import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { centerProfilePath } from '../../../shared/constants/routes';
import type { CenterSummary } from '../../marketplace/types/marketplaceTypes';

type CenterListingCardProps = {
  center: CenterSummary;
  /** Nút phụ đặt trước "Xem hồ sơ" (vd. phụ huynh: nhờ trung tâm tìm gia sư). */
  action?: ReactNode;
};

const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');

const descSnippet = (desc: string | null) => {
  const text = desc?.trim();
  if (!text) return 'Trung tâm gia sư đã được xác minh trên nền tảng TCS.';
  return text.length > 140 ? `${text.slice(0, 140)}…` : text;
};

/** Thẻ trung tâm cùng form với thẻ gia sư (tcs-listing-card) — bấm vào thẻ mở hồ sơ trung tâm. */
export function CenterListingCard({ center, action }: CenterListingCardProps) {
  return (
    <article className="tcs-listing-card tcs-listing-card--link">
      <div className="tcs-listing-card__top">
        <div className="tcs-listing-card__profile">
          <div className="tcs-listing-card__avatar">
            {center.avatar ? (
              <img src={center.avatar} alt="" />
            ) : (
              initials(center.companyName) || 'TT'
            )}
          </div>
          <div className="tcs-listing-card__identity">
            <h3 className="tcs-listing-card__name">
              <Link
                className="tcs-listing-card__cover-link"
                to={centerProfilePath()}
                state={{ centerId: center.centerId }}
              >
                {center.companyName}
              </Link>
            </h3>
            <div className="tcs-listing-card__badges">
              <span className="tcs-listing-card__badge">Trung tâm</span>
              <span
                className="tcs-listing-card__badge tcs-listing-card__badge--verified"
                title="Trung tâm đã được xác minh"
              >
                ✓ Đã xác minh
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="tcs-listing-card__meta">
        <div className="tcs-listing-card__row">
          <span className="tcs-listing-card__label">Địa chỉ</span>
          <span className="tcs-listing-card__value">{center.address?.trim() || '—'}</span>
        </div>
        <div className="tcs-listing-card__row">
          <span className="tcs-listing-card__label">Điện thoại</span>
          <span className="tcs-listing-card__value">{center.phone?.trim() || '—'}</span>
        </div>
      </div>

      <p className="tcs-listing-card__bio">{descSnippet(center.description)}</p>

      <div className="tcs-listing-card__foot">
        <div className="tcs-listing-card__actions tcs-listing-card__actions--end">
          {action}
          <Link
            className="tcs-btn tcs-btn--market"
            to={centerProfilePath()}
            state={{ centerId: center.centerId }}
          >
            Xem hồ sơ
          </Link>
        </div>
      </div>
    </article>
  );
}
