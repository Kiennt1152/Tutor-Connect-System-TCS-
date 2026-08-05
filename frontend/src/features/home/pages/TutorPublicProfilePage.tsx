import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useParams } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { StarRating } from '../../reviews/components/StarRating';
import { CriteriaBreakdown } from '../../reviews/components/CriteriaBreakdown';
import { reviewApi } from '../../reviews/api/reviewApi';
import type { ReviewResponse, TutorReputation } from '../../reviews/types/reviewTypes';
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

export default function TutorPublicProfilePage() {
  const { tutorId } = useParams<{ tutorId: string }>();
  const [data, setData] = useState<TutorReputation | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    if (!tutorId) return;
    setStatus('loading');
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
            <div className="tp-grid">
              <ReputationSummary data={data} />
              <ReviewsList reviews={data.reviews} />
            </div>
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

function ReputationSummary({ data }: { readonly data: TutorReputation }) {
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
          return (
            <li key={star} className="tp-dist__row">
              <span className="tp-dist__star">{star}★</span>
              <span className="tp-dist__bar">
                <span className="tp-dist__fill" style={{ width: `${pct}%` }} />
              </span>
              <span className="tp-dist__count">{count}</span>
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

function ReviewsList({ reviews }: { readonly reviews: ReviewResponse[] }) {
  return (
    <section className="tp-card tp-reviews">
      <h2 className="tp-card__title">Đánh giá từ học viên ({reviews.length})</h2>
      {reviews.length === 0 ? (
        <p className="tp-muted">Gia sư chưa có đánh giá công khai nào.</p>
      ) : (
        <ul className="tp-reviews__list">
          {reviews.map((r) => (
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
