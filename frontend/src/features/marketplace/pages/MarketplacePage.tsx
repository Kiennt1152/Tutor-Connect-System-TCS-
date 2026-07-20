import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { marketplaceApi } from '../api/marketplaceApi';
import type { LessonMode, MarketplaceClass } from '../types/marketplaceTypes';
import './MarketplacePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatCurrency(value: number): string {
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

export default function MarketplacePage() {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<MarketplaceClass[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    setStatus('loading');
    marketplaceApi
      .getOpenClasses()
      .then((res) => {
        setClasses(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được danh sách lớp.'));
        setStatus('error');
      });
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return classes;
    return classes.filter((c) =>
      [c.title, c.subjectName, c.gradeName]
        .filter(Boolean)
        .some((v) => (v as string).toLowerCase().includes(q)),
    );
  }, [classes, query]);

  const openDetail = (classId: number) => navigate(`/marketplace/classes/${classId}`);

  return (
    <>
      <VerificationHeader />
      <div className="mk-page">
        <section className="mk-hero">
          <div className="mk-hero__text">
            <span className="mk-hero__eyebrow">Khám phá lớp học</span>
            <h1 className="mk-hero__title">Tìm lớp phù hợp với bạn</h1>
            <p className="mk-hero__sub">
              Duyệt các lớp đang mở đăng ký. Bấm vào một lớp để xem chi tiết và đăng ký.
            </p>
          </div>
          <div className="mk-search">
            <span className="mk-search__icon" aria-hidden="true">🔍</span>
            <input
              className="mk-search__input"
              placeholder="Tìm theo tên lớp, môn học, khối/lớp…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
        </section>

        {status === 'loading' && <div className="mk-state">Đang tải danh sách lớp…</div>}
        {status === 'error' && <div className="mk-state mk-state--error">{error}</div>}
        {status === 'success' && classes.length === 0 && (
          <div className="mk-empty">
            <div className="mk-empty__emoji">📭</div>
            <p>Hiện chưa có lớp nào đang mở đăng ký.</p>
          </div>
        )}
        {status === 'success' && classes.length > 0 && filtered.length === 0 && (
          <div className="mk-empty">
            <div className="mk-empty__emoji">🔎</div>
            <p>Không tìm thấy lớp phù hợp với “{query}”.</p>
          </div>
        )}

        {status === 'success' && filtered.length > 0 && (
          <>
            <p className="mk-count">
              {filtered.length} lớp{query.trim() ? ` khớp “${query.trim()}”` : ' đang mở'}
            </p>
            <div className="mk-grid">
              {filtered.map((c) => (
                <button
                  type="button"
                  className="mk-card"
                  key={c.classId}
                  onClick={() => openDetail(c.classId)}
                >
                  <span className="mk-card__accent" aria-hidden="true" />
                  <div className="mk-card__body">
                    <h2 className="mk-card__title">{c.title}</h2>

                    <div className="mk-tags">
                      {c.subjectName && <span className="mk-tag mk-tag--subject">{c.subjectName}</span>}
                      {c.gradeName && <span className="mk-tag">{c.gradeName}</span>}
                      <span className="mk-tag">{LESSON_MODE_LABELS[c.lessonMode]}</span>
                    </div>

                    <ul className="mk-facts">
                      <li>
                        <span className="mk-facts__ic" aria-hidden="true">🗓️</span>
                        {c.startDate} → {c.endDate}
                      </li>
                      <li>
                        <span className="mk-facts__ic" aria-hidden="true">📚</span>
                        {c.numberOfSessions} buổi
                      </li>
                      <li>
                        <span className="mk-facts__ic" aria-hidden="true">👥</span>
                        Tối đa {c.maxStudents ?? '—'} học sinh
                      </li>
                    </ul>
                  </div>

                  <div className="mk-card__foot">
                    <span className="mk-fee">{formatCurrency(c.tuitionFee)}</span>
                    <span className="mk-card__cta">Xem chi tiết →</span>
                  </div>
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    </>
  );
}
