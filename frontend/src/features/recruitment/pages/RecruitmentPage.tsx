import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { centerApi } from '../../center/api/centerApi';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { ChatButton } from '../../messaging/components/ChatButton';
import { APP_ROUTES } from '../../../shared/constants/routes';
import type {
  RecruitmentApplication,
  RecruitmentApplicationStatus,
  RecruitmentPost,
} from '../../center/types/centerTypes';
import '../../center/pages/CenterPage.css';

const APP_STATUS_LABELS: Record<RecruitmentApplicationStatus, { label: string; cls: string }> = {
  APPLIED: { label: 'Chờ trung tâm duyệt', cls: 'pending' },
  SCREENING: { label: 'Đang lọc hồ sơ', cls: 'pending' },
  INTERVIEW: { label: 'Phỏng vấn', cls: 'pending' },
  PASSED: { label: 'Chờ ký hợp đồng', cls: 'pending' },
  HIRED: { label: 'Đã được nhận', cls: 'ok' },
  REJECTED: { label: 'Bị từ chối', cls: 'no' },
  WITHDRAWN: { label: 'Đã rút đơn', cls: 'no' },
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

/** Mã lỗi backend trả về (VD: "VERIFICATION_REQUIRED") để frontend xử lý riêng. */
function errorCode(error: unknown): string | undefined {
  if (axios.isAxiosError(error) && typeof error.response?.data?.code === 'string') {
    return error.response.data.code;
  }
  return undefined;
}

function fmtDate(value: string | null): string {
  if (!value) return '—';
  const d = new Date(value);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

export default function RecruitmentPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<'open' | 'mine'>('open');
  const [posts, setPosts] = useState<RecruitmentPost[]>([]);
  const [myApps, setMyApps] = useState<RecruitmentApplication[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    Promise.all([centerApi.getOpenPosts(), centerApi.getMyApplications()])
      .then(([open, mine]) => {
        setPosts(open.data);
        setMyApps(mine.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được tin tuyển dụng.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Gia sư rút đơn ứng tuyển (chỉ khi đơn còn ở trạng thái chờ duyệt).
  const [withdrawBusy, setWithdrawBusy] = useState<number | null>(null);
  const withdraw = async (a: RecruitmentApplication) => {
    setWithdrawBusy(a.recruitmentAppId);
    setError('');
    setOkMsg('');
    try {
      await centerApi.withdrawApplication(a.recruitmentAppId);
      setOkMsg('Đã rút đơn ứng tuyển.');
      load();
    } catch (err) {
      setError(extractError(err, 'Không rút được đơn.'));
    } finally {
      setWithdrawBusy(null);
    }
  };

  /** recruitmentId của các tin mình đã nộp đơn — để khoá nút ứng tuyển. */
  const appliedIds = useMemo(
    () => new Set(myApps.map((a) => a.recruitmentId)),
    [myApps],
  );

  // ----- Ứng tuyển -----
  const [applyFor, setApplyFor] = useState<RecruitmentPost | null>(null);
  const [coverLetter, setCoverLetter] = useState('');
  const [applyBusy, setApplyBusy] = useState(false);
  const [applyError, setApplyError] = useState('');

  const openApply = (post: RecruitmentPost) => {
    setApplyFor(post);
    setCoverLetter('');
    setApplyError('');
  };
  const closeApply = () => setApplyFor(null);

  const submitApply = async () => {
    if (!applyFor) return;
    setApplyBusy(true);
    setApplyError('');
    try {
      await centerApi.apply(applyFor.recruitmentId, coverLetter.trim());
      setOkMsg('Đã gửi đơn ứng tuyển. Chờ trung tâm duyệt.');
      setApplyFor(null);
      load();
    } catch (err) {
      // Chưa xác minh hồ sơ -> điều hướng sang trang Xác minh (không hiện lỗi đỏ).
      if (errorCode(err) === 'VERIFICATION_REQUIRED') {
        setApplyFor(null);
        navigate(APP_ROUTES.verification, {
          state: {
            notice: 'Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển tin tuyển dụng.',
          },
        });
        return;
      }
      setApplyError(extractError(err, 'Không gửi được đơn ứng tuyển.'));
    } finally {
      setApplyBusy(false);
    }
  };

  return (
    <>
      <HomeNavbar />
      <div className="rc-bg">
      <div className="rc-page">
        <div className="rc-topbar">
          <Link className="rc-back" to="/">
            ← Trang chủ
          </Link>
        </div>

        <header className="rc-header">
          <div>
            <h1 className="rc-title">Tin tuyển gia sư</h1>
            <p className="rc-subtitle">
              Xem các tin tuyển dụng đang mở từ trung tâm và gửi đơn ứng tuyển. Theo dõi kết quả ở
              tab “Đơn của tôi”.
            </p>
          </div>
        </header>

        <div className="rc-tabs">
          <button
            className={`rc-tab${tab === 'open' ? ' is-active' : ''}`}
            type="button"
            onClick={() => setTab('open')}
          >
            Tin đang tuyển ({posts.length})
          </button>
          <button
            className={`rc-tab${tab === 'mine' ? ' is-active' : ''}`}
            type="button"
            onClick={() => setTab('mine')}
          >
            Đơn của tôi ({myApps.length})
          </button>
        </div>

        {error && <div className="rc-alert rc-alert--error">{error}</div>}
        {okMsg && <div className="rc-alert rc-alert--ok">{okMsg}</div>}
        {status === 'loading' && <div className="rc-state">Đang tải…</div>}

        {status === 'success' && tab === 'open' && (
          <>
            {posts.length === 0 ? (
              <div className="rc-empty">
                <div className="rc-empty__emoji">📭</div>
                <p>Hiện chưa có tin tuyển dụng nào đang mở.</p>
              </div>
            ) : (
              <div className="rc-list">
                {posts.map((p) => {
                  const applied = appliedIds.has(p.recruitmentId);
                  return (
                    <article className="rc-card" key={p.recruitmentId}>
                      <div className="rc-card__head">
                        <div>
                          <h2 className="rc-card__title">{p.title}</h2>
                          <div className="rc-chips">
                            {p.centerName && <span className="rc-chip">🏫 {p.centerName}</span>}
                            {p.subjectName && <span className="rc-chip">📘 {p.subjectName}</span>}
                            {p.locationLabel && (
                              <span className="rc-chip">📍 {p.locationLabel}</span>
                            )}
                            <span className="rc-chip">👤 {p.maxPositions} vị trí</span>
                            {!!p.requiredExperience && (
                              <span className="rc-chip">🎓 ≥ {p.requiredExperience} năm KN</span>
                            )}
                          </div>
                        </div>
                        <div className="rc-card__meta">
                          <span>Đăng: {fmtDate(p.publishedAt)}</span>
                        </div>
                      </div>

                      <p className="rc-card__desc">{p.description}</p>

                      {p.requirements && (
                        <div className="rc-section">
                          <span className="rc-section__label">Yêu cầu</span>
                          <p className="rc-card__desc">{p.requirements}</p>
                        </div>
                      )}
                      {p.benefits && (
                        <div className="rc-section">
                          <span className="rc-section__label">Quyền lợi</span>
                          <p className="rc-card__desc">{p.benefits}</p>
                        </div>
                      )}

                      <div className="rc-card__foot">
                        <span className="rc-count">
                          {applied ? '✓ Bạn đã ứng tuyển tin này' : ''}
                        </span>
                        <div className="rc-actions">
                          <button
                            className="rc-btn rc-btn--primary"
                            type="button"
                            disabled={applied}
                            onClick={() => openApply(p)}
                          >
                            {applied ? 'Đã ứng tuyển' : 'Ứng tuyển'}
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </>
        )}

        {status === 'success' && tab === 'mine' && (
          <>
            {myApps.length === 0 ? (
              <div className="rc-empty">
                <div className="rc-empty__emoji">📄</div>
                <p>Bạn chưa nộp đơn ứng tuyển nào.</p>
              </div>
            ) : (
              <div className="rc-list">
                {myApps.map((a) => {
                  const st = APP_STATUS_LABELS[a.status];
                  return (
                    <article className="rc-card" key={a.recruitmentAppId}>
                      <div className="rc-card__head">
                        <div>
                          <h2 className="rc-card__title">{a.postTitle}</h2>
                          <div className="rc-chips">
                            {a.centerName && <span className="rc-chip">🏫 {a.centerName}</span>}
                            <span className={`rc-status rc-status--${st.cls}`}>{st.label}</span>
                          </div>
                        </div>
                        <div className="rc-card__meta">
                          <span>Nộp: {fmtDate(a.appliedAt)}</span>
                          {a.reviewedAt && <span>Duyệt: {fmtDate(a.reviewedAt)}</span>}
                          <ChatButton
                            contextType="RECRUITMENT"
                            contextId={a.recruitmentAppId}
                            label="Nhắn tin với trung tâm"
                            size="sm"
                          />
                        </div>
                      </div>
                      {a.coverLetter && <p className="rc-card__desc">{a.coverLetter}</p>}
                      {a.status === 'APPLIED' && (
                        <div className="rc-card__foot">
                          <button
                            type="button"
                            className="rc-btn rc-btn--ghost rc-btn--sm"
                            disabled={withdrawBusy === a.recruitmentAppId}
                            onClick={() => withdraw(a)}
                          >
                            {withdrawBusy === a.recruitmentAppId ? 'Đang rút…' : 'Rút đơn'}
                          </button>
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>

      {applyFor && (
        <div className="rc-modal" role="dialog" aria-modal="true">
          <div className="rc-modal__backdrop" onClick={closeApply} />
          <div className="rc-modal__card">
            <div className="rc-modal__head">
              <div>
                <h2 className="rc-modal__title">Ứng tuyển</h2>
                <p className="rc-modal__sub">
                  {applyFor.title}
                  {applyFor.centerName ? ` — ${applyFor.centerName}` : ''}
                </p>
              </div>
              <button
                className="rc-modal__close"
                type="button"
                onClick={closeApply}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <div className="rc-modal__body">
              {applyError && <div className="rc-alert rc-alert--error">{applyError}</div>}
              <label className="rc-field">
                <span>Thư giới thiệu (tuỳ chọn)</span>
                <textarea
                  rows={5}
                  value={coverLetter}
                  onChange={(e) => setCoverLetter(e.target.value)}
                  placeholder="Giới thiệu ngắn về kinh nghiệm, thời gian rảnh, lý do phù hợp với tin này…"
                />
              </label>
            </div>
            <div className="rc-modal__actions">
              <button className="rc-btn rc-btn--ghost" type="button" onClick={closeApply}>
                Huỷ
              </button>
              <button
                className="rc-btn rc-btn--primary"
                type="button"
                disabled={applyBusy}
                onClick={submitApply}
              >
                {applyBusy ? 'Đang gửi…' : 'Gửi đơn ứng tuyển'}
              </button>
            </div>
          </div>
        </div>
      )}
      </div>
    </>
  );
}
