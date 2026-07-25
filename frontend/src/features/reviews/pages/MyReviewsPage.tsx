import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { reviewApi } from '../api/reviewApi';
import type { ReviewableAssignment } from '../types/reviewTypes';
import { ReviewFormModal } from '../components/ReviewFormModal';
import { StarRating } from '../components/StarRating';
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
  const [active, setActive] = useState<ReviewableAssignment | null>(null);
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
    setActive(null);
    setToast('Đã gửi đánh giá. Cảm ơn phản hồi của bạn!');
    load();
    window.setTimeout(() => setToast(''), 4000);
  }

  const pending = items.filter((i) => !i.reviewed);
  const done = items.filter((i) => i.reviewed);

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="tcs-container rv-page">
        <header className="rv-page__head">
          <h1 className="rv-page__title">Đánh giá của tôi</h1>
          <p className="rv-page__subtitle">
            Gửi đánh giá và phản hồi cho gia sư của các lớp học đã hoàn thành.
          </p>
        </header>

        {status === 'loading' ? <p className="rv-muted">Đang tải...</p> : null}
        {status === 'error' ? <p className="rv-error">{loadError}</p> : null}

        {status === 'success' && items.length === 0 ? (
          <div className="rv-empty">
            <p>Bạn chưa có lớp học hoàn thành nào để đánh giá.</p>
            <p className="rv-muted">
              Sau khi một lớp học kết thúc, lớp đó sẽ xuất hiện ở đây để bạn đánh giá gia sư.
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
                  </div>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--market"
                    onClick={() => setActive(item)}
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
                <li key={item.assignmentId} className="rv-card rv-card--done">
                  <div className="rv-card__info">
                    <p className="rv-card__tutor">{item.tutorName}</p>
                    <p className="rv-card__class">
                      {item.classTitle}
                      {item.subjectName ? ` · ${item.subjectName}` : ''}
                    </p>
                    <div className="rv-card__review">
                      <StarRating value={item.rating ?? 0} readOnly size={18} />
                      <span className="rv-card__date">{formatDate(item.reviewedAt)}</span>
                    </div>
                    {item.comment ? <p className="rv-card__comment">“{item.comment}”</p> : null}
                  </div>
                </li>
              ))}
            </ul>
          </section>
        ) : null}
      </main>

      {active ? (
        <ReviewFormModal
          assignment={active}
          onClose={() => setActive(null)}
          onSubmitted={handleSubmitted}
        />
      ) : null}

      {toast ? <div className="rv-toast">{toast}</div> : null}
    </div>
  );
}
