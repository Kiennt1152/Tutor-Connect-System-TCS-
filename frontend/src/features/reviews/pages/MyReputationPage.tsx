import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { StarRating } from '../components/StarRating';
import { CriteriaBreakdown } from '../components/CriteriaBreakdown';
import { ReportReviewModal } from '../components/ReportReviewModal';
import { reviewApi } from '../api/reviewApi';
import type { ReviewResponse, TutorReputation } from '../types/reviewTypes';
import '../../home/pages/TutorPublicProfilePage.css';

const STAR_ROWS = [5, 4, 3, 2, 1] as const;

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('vi-VN');
}

export default function MyReputationPage() {
  const [data, setData] = useState<TutorReputation | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    reviewApi
      .getMyReputation()
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được nhận xét về bạn.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="tcs-container tp-page">
        <header className="tp-mine-head">
          <h1 className="tp-mine-head__title">Nhận xét về tôi</h1>
          <p className="tp-mine-head__subtitle">
            Tổng hợp đánh giá và phản hồi mà khách hàng đã gửi cho bạn.
          </p>
        </header>

        {status === 'loading' ? <p className="tp-muted">Đang tải nhận xét…</p> : null}
        {status === 'error' ? (
          <div className="tp-state">
            <p className="tp-error">{error}</p>
            <button type="button" className="tcs-btn tcs-btn--market" onClick={load}>
              Thử lại
            </button>
          </div>
        ) : null}

        {status === 'success' && data ? (
          <div className="tp-grid">
            <ReputationSummary data={data} />
            <ReviewsList reviews={data.reviews} onReplied={load} />
          </div>
        ) : null}
      </main>
    </div>
  );
}

function ReputationSummary({ data }: { readonly data: TutorReputation }) {
  const total = data.totalReviews;
  return (
    <section className="tp-card tp-summary">
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

function ReviewsList({
  reviews,
  onReplied,
}: {
  readonly reviews: ReviewResponse[];
  readonly onReplied: () => void;
}) {
  return (
    <section className="tp-card tp-reviews">
      <h2 className="tp-card__title">Đánh giá từ học viên ({reviews.length})</h2>
      {reviews.length === 0 ? (
        <p className="tp-muted">Bạn chưa nhận được đánh giá công khai nào.</p>
      ) : (
        <ul className="tp-reviews__list">
          {reviews.map((r) => (
            <ReviewCard key={r.reviewId} review={r} onReplied={onReplied} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ReviewCard({
  review: r,
  onReplied,
}: {
  readonly review: ReviewResponse;
  readonly onReplied: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [text, setText] = useState(r.tutorReply ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [showDetails, setShowDetails] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reported, setReported] = useState(false);

  function startEdit() {
    setText(r.tutorReply ?? '');
    setError('');
    setEditing(true);
  }

  function submit() {
    const reply = text.trim();
    if (!reply) {
      setError('Vui lòng nhập nội dung phản hồi.');
      return;
    }
    setSubmitting(true);
    setError('');
    reviewApi
      .replyToReview(r.reviewId, reply)
      .then(() => {
        setEditing(false);
        onReplied();
      })
      .catch((err) => {
        setError(extractError(err, 'Không gửi được phản hồi.'));
        setSubmitting(false);
      });
  }

  return (
    <li className="tp-review">
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
      {r.criteriaJson ? (
        <>
          <button
            type="button"
            className="tp-review__toggle"
            aria-expanded={showDetails}
            onClick={() => setShowDetails((v) => !v)}
          >
            {showDetails ? 'Ẩn chi tiết đánh giá' : 'Xem chi tiết đánh giá'}
          </button>
          {showDetails ? <CriteriaBreakdown criteriaJson={r.criteriaJson} /> : null}
        </>
      ) : null}
      {r.comment ? <p className="tp-review__comment">“{r.comment}”</p> : null}

      {r.tutorReply && !editing ? (
        <div className="tp-reply">
          <div className="tp-reply__head">
            <span className="tp-reply__label">↩ Phản hồi của bạn</span>
            {r.tutorReplyAt ? (
              <span className="tp-reply__date">{formatDate(r.tutorReplyAt)}</span>
            ) : null}
          </div>
          <p className="tp-reply__text">{r.tutorReply}</p>
          <button type="button" className="tp-reply__edit" onClick={startEdit}>
            Chỉnh sửa phản hồi
          </button>
        </div>
      ) : null}

      {editing ? (
        <div className="tp-reply tp-reply--form">
          <textarea
            className="tp-reply__input"
            rows={3}
            placeholder="Viết phản hồi cho học viên…"
            value={text}
            onChange={(e) => setText(e.target.value)}
            disabled={submitting}
          />
          {error ? <p className="tp-error">{error}</p> : null}
          <div className="tp-reply__actions">
            <button
              type="button"
              className="tcs-btn tcs-btn--market"
              onClick={submit}
              disabled={submitting}
            >
              {submitting ? 'Đang gửi…' : 'Gửi phản hồi'}
            </button>
            <button
              type="button"
              className="tp-reply__cancel"
              onClick={() => setEditing(false)}
              disabled={submitting}
            >
              Hủy
            </button>
          </div>
        </div>
      ) : null}

      {!r.tutorReply && !editing ? (
        <button type="button" className="tp-reply__add" onClick={startEdit}>
          ↩ Phản hồi đánh giá này
        </button>
      ) : null}

      <div className="tp-review__report">
        {reported ? (
          <span className="tp-review__reported">✓ Đã gửi báo cáo — chờ quản trị viên kiểm duyệt</span>
        ) : (
          <button type="button" className="tp-review__report-btn" onClick={() => setReporting(true)}>
            🚩 Báo cáo đánh giá này
          </button>
        )}
      </div>

      {reporting ? (
        <ReportReviewModal
          reviewId={r.reviewId}
          subtitle={`${r.reviewerDisplayName}${r.classTitle ? ` · ${r.classTitle}` : ''}`}
          onClose={() => setReporting(false)}
          onReported={() => {
            setReporting(false);
            setReported(true);
          }}
        />
      ) : null}
    </li>
  );
}
