import { useEffect, useState } from 'react';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { marketplaceApi } from '../api/marketplaceApi';
import type { LessonMode, MarketplaceClass, RecurringType } from '../types/marketplaceTypes';
import './MarketplacePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

const RECURRING_LABELS: Record<RecurringType, string> = {
  DAILY: 'Hằng ngày',
  WEEKLY: 'Hằng tuần',
  ONCE: 'Một lần',
};

type RegStatus = 'idle' | 'loading' | 'done' | 'error';
interface RegState {
  status: RegStatus;
  message: string;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value) + ' đ';
}

export default function MarketplacePage() {
  const { user } = useAuth();
  const canRegister = user?.role === 'TUTOR' || user?.role === 'CLIENT';

  const [classes, setClasses] = useState<MarketplaceClass[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [reg, setReg] = useState<Record<number, RegState>>({});

  const reload = () => {
    setStatus('loading');
    setError('');
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
  };

  useEffect(() => {
    reload();
  }, []);

  const register = async (classId: number) => {
    setReg((prev) => ({ ...prev, [classId]: { status: 'loading', message: '' } }));
    try {
      const res = await marketplaceApi.register(classId);
      setReg((prev) => ({
        ...prev,
        [classId]: { status: 'done', message: res.data?.message ?? 'Đăng ký thành công' },
      }));
    } catch (err) {
      setReg((prev) => ({
        ...prev,
        [classId]: { status: 'error', message: extractError(err, 'Đăng ký thất bại.') },
      }));
    }
  };

  return (
    <>
      <VerificationHeader />
      <div className="mk-page">
        <header className="mk-header">
          <h1 className="mk-title">Tìm lớp</h1>
          <p className="mk-subtitle">Các lớp đang mở đăng ký.</p>
        </header>

        {status === 'loading' && <div className="mk-state">Đang tải danh sách lớp…</div>}

        {status === 'error' && (
          <div className="mk-state">
            <p>{error}</p>
            <button className="mk-btn mk-btn--primary" type="button" onClick={reload}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && classes.length === 0 && (
          <div className="mk-state">Hiện chưa có lớp nào đang mở đăng ký.</div>
        )}

        {status === 'success' && classes.length > 0 && (
          <div className="mk-grid">
            {classes.map((c) => {
              const r = reg[c.classId];
              return (
                <article className="mk-card" key={c.classId}>
                  <div className="mk-card__head">
                    <h2 className="mk-card__title">{c.title}</h2>
                    <span className="mk-badge">{RECURRING_LABELS[c.recurringType]}</span>
                  </div>

                  {c.description && <p className="mk-card__desc">{c.description}</p>}

                  <dl className="mk-meta">
                    <div className="mk-meta__row">
                      <dt>Môn</dt>
                      <dd>{c.subjectName ?? '—'}</dd>
                    </div>
                    <div className="mk-meta__row">
                      <dt>Khối/Lớp</dt>
                      <dd>{c.gradeName ?? '—'}</dd>
                    </div>
                    <div className="mk-meta__row">
                      <dt>Hình thức</dt>
                      <dd>{LESSON_MODE_LABELS[c.lessonMode]}</dd>
                    </div>
                    <div className="mk-meta__row">
                      <dt>Thời gian</dt>
                      <dd>
                        {c.startDate} → {c.endDate}
                      </dd>
                    </div>
                    <div className="mk-meta__row">
                      <dt>Số buổi</dt>
                      <dd>{c.numberOfSessions}</dd>
                    </div>
                    <div className="mk-meta__row">
                      <dt>Học phí</dt>
                      <dd className="mk-fee">{formatCurrency(c.tuitionFee)}</dd>
                    </div>
                  </dl>

                  {r?.status === 'done' ? (
                    <div className="mk-alert mk-alert--ok">{r.message}</div>
                  ) : (
                    <>
                      {r?.status === 'error' && (
                        <div className="mk-alert mk-alert--error">{r.message}</div>
                      )}
                      {canRegister ? (
                        <button
                          className="mk-btn mk-btn--primary mk-card__cta"
                          type="button"
                          disabled={r?.status === 'loading'}
                          onClick={() => register(c.classId)}
                        >
                          {r?.status === 'loading' ? 'Đang đăng ký…' : 'Đăng ký'}
                        </button>
                      ) : (
                        <p className="mk-note">Đăng nhập bằng tài khoản gia sư/phụ huynh để đăng ký.</p>
                      )}
                    </>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </div>
    </>
  );
}
