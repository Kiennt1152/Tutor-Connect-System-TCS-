import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { reviewApi } from '../api/reviewApi';
import type { ReviewableAssignment } from '../types/reviewTypes';
import { ReviewFormModal } from '../components/ReviewFormModal';
import { ReportReviewModal } from '../components/ReportReviewModal';
import { REPLY_REPORT_CATEGORY_OPTIONS } from '../api/reportApi';
import { StarRating } from '../components/StarRating';
import { CriteriaBreakdown } from '../components/CriteriaBreakdown';
import './MyReviewsPage.css';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatDate(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('vi-VN');
}

export default function MyReviewsPage() {
  const [items, setItems] = useState<ReviewableAssignment[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [loadError, setLoadError] = useState('');
  const [active, setActive] = useState<{ item: ReviewableAssignment; edit: boolean } | null>(null);
  const [toast, setToast] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    reviewApi
      .getReviewable()
      .then((res) => {
        setItems(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setLoadError(extractError(err, 'Không tải được danh sách lớp'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function handleSubmitted() {
    const wasEdit = active?.edit;
    setActive(null);
    setToast(wasEdit ? 'Đã cập nhật đánh giá của bạn.' : 'Đã gửi đánh giá. Cảm ơn phản hồi của bạn!');
    load();
    window.setTimeout(() => setToast(''), 4000);
  }

  const pending = items.filter((i) => i.reviewable);
  const done = items.filter((i) => i.reviewed);

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="tcs-container rv-page">
        <header className="rv-page__head">
          <h1 className="rv-page__title">Đánh giá của tôi</h1>
          <p className="rv-page__subtitle">
            Sau mỗi buổi học bạn có thể đánh giá gia sư. Hãy đánh giá ít nhất một lần mỗi tháng.
          </p>
        </header>

        {status === 'loading' ? <p className="rv-muted">Đang tải...</p> : null}
        {status === 'error' ? <p className="rv-error">{loadError}</p> : null}

        {status === 'success' && items.length === 0 ? (
          <div className="rv-empty">
            <p>Bạn chưa có lớp học nào để đánh giá.</p>
            <p className="rv-muted">
              Sau buổi học đầu tiên, lớp sẽ xuất hiện ở đây để bạn đánh giá gia sư.
            </p>
          </div>
        ) : null}

        {pending.length > 0 ? (
          <section className="rv-section">
            <h2 className="rv-section__title">Chờ đánh giá ({pending.length})</h2>
            <ul className="rv-list">
              {pending.map((item) => (
                <li key={item.assignmentId} className="rv-card">
                  <div className="rv-card__info">
                    <p className="rv-card__tutor">{item.tutorName}</p>
                    <p className="rv-card__class">
                      {item.classTitle}
                      {item.subjectName ? ` · ${item.subjectName}` : ''}
                    </p>
                    {item.reviewOverdue ? (
                      <span className="rv-badge rv-badge--overdue">
                        Cần đánh giá trong tháng này
                      </span>
                    ) : null}
                  </div>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--market"
                    onClick={() => setActive({ item, edit: false })}
                  >
                    Đánh giá
                  </button>
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {done.length > 0 ? (
          <section className="rv-section">
            <h2 className="rv-section__title">Đã đánh giá ({done.length})</h2>
            <ul className="rv-list">
              {done.map((item) => (
                <DoneReviewCard
                  key={item.assignmentId}
                  item={item}
                  onEdit={() => setActive({ item, edit: true })}
                />
              ))}
            </ul>
          </section>
        ) : null}
      </main>
      <SiteFooter />

      {active ? (
        <ReviewFormModal
          assignment={active.item}
          edit={active.edit}
          onClose={() => setActive(null)}
          onSubmitted={handleSubmitted}
        />
      ) : null}

      {toast ? <div className="rv-toast">{toast}</div> : null}
    </div>
  );
}

function DoneReviewCard({
  item,
  onEdit,
}: {
  readonly item: ReviewableAssignment;
  readonly onEdit: () => void;
}) {
  const [showDetails, setShowDetails] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reported, setReported] = useState(false);

  return (
    <li className="rv-card rv-card--done">
      <div className="rv-card__info">
        <p className="rv-card__tutor">{item.tutorName}</p>
        <p className="rv-card__class">
          {item.classTitle}
          {item.subjectName ? ` · ${item.subjectName}` : ''}
        </p>
        <div className="rv-card__review">
          <StarRating value={item.rating ?? 0} readOnly size={18} />
          <span className="rv-card__overall">Điểm tổng {(item.rating ?? 0).toFixed(1)}/5</span>
          <span className="rv-card__date">{formatDate(item.reviewedAt)}</span>
        </div>
        {item.criteriaJson ? (
          <>
            <button
              type="button"
              className="rv-card__toggle"
              aria-expanded={showDetails}
              onClick={() => setShowDetails((v) => !v)}
            >
              {showDetails ? 'Ẩn chi tiết đánh giá' : 'Xem chi tiết đánh giá'}
            </button>
            {showDetails ? <CriteriaBreakdown criteriaJson={item.criteriaJson} /> : null}
          </>
        ) : null}
        {item.comment ? <p className="rv-card__comment">“{item.comment}”</p> : null}
        <p className="rv-card__visibility">
          {item.anonymous
            ? `Ẩn danh · hiển thị là “${item.reviewerDisplayName}”`
            : `Công khai · hiển thị tên “${item.reviewerDisplayName}”`}
        </p>
        <p className="rv-card__periodic">
          Đã đánh giá {item.reviewsSubmitted} lần
          {item.reviewOverdue
            ? ' · cần đánh giá trong tháng này'
            : item.reviewable
              ? ' · có thể đánh giá thêm sau buổi học mới'
              : ''}
        </p>
        {item.tutorReply ? (
          <div className="rv-reply">
            <div className="rv-reply__head">
              <span className="rv-reply__label">↩ Phản hồi của gia sư</span>
              {item.tutorReplyAt ? (
                <span className="rv-reply__date">{formatDate(item.tutorReplyAt)}</span>
              ) : null}
            </div>
            <p className="rv-reply__text">{item.tutorReply}</p>
          </div>
        ) : null}
        <div className="rv-card__actions">
          {item.reviewId != null ? (
            <button type="button" className="tcs-btn tcs-btn--ghost rv-card__edit" onClick={onEdit}>
              Chỉnh sửa đánh giá
            </button>
          ) : null}
          {item.reviewId != null &&
            item.tutorReply &&
            (reported ? (
              <span className="rv-card__reported">✓ Đã gửi báo cáo — chờ kiểm duyệt</span>
            ) : (
              <button
                type="button"
                className="rv-card__report"
                onClick={() => setReporting(true)}
              >
                🚩 Báo cáo phản hồi của gia sư
              </button>
            ))}
        </div>
      </div>

      {reporting && item.reviewId != null ? (
        <ReportReviewModal
          reviewId={item.reviewId}
          title="Báo cáo phản hồi của gia sư"
          intro="Báo cáo sẽ được gửi tới quản trị viên để kiểm duyệt nội dung phản hồi của gia sư đối với đánh giá này."
          options={REPLY_REPORT_CATEGORY_OPTIONS}
          subtitle={`${item.tutorName}${item.classTitle ? ` · ${item.classTitle}` : ''}`}
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
