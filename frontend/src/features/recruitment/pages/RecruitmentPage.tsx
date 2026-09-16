import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { centerApi } from '../../center/api/centerApi';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { ExpiryBadge } from '../../../shared/components/ExpiryBadge';
import { ChatButton } from '../../messaging/components/ChatButton';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { normalizeName } from '../../marketplace/matching/tutorMatching';
import type {
  RecruitmentApplication,
  RecruitmentApplicationStatus,
  RecruitmentPost,
} from '../../center/types/centerTypes';
import '../../center/pages/CenterPage.css';
// Ô tìm kiếm + phân trang dùng chung kiểu với trang "Tìm gia sư".
import '../../home/pages/FindTutorPage.css';

const PAGE_SIZE = 5;

const time = (value: string | null | undefined) => (value ? Date.parse(value) : NaN);

/**
 * Phân trang cùng kiểu trang "Tìm gia sư" (tcs-pagination). Luôn hiện khi danh sách có tin
 * (kể cả chỉ 1 trang) để người dùng thấy đang ở trang nào; nút Trước/Sau tự khoá ở hai đầu.
 */
function Pager({
  page,
  totalPages,
  onChange,
  label,
}: {
  readonly page: number;
  readonly totalPages: number;
  readonly onChange: (page: number) => void;
  readonly label: string;
}) {
  return (
    <nav className="tcs-pagination" aria-label={label}>
      <button
        type="button"
        className="tcs-pagination__nav"
        onClick={() => onChange(page - 1)}
        disabled={page === 1}
      >
        ← Trước
      </button>
      {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
        <button
          key={p}
          type="button"
          className={`tcs-pagination__page${p === page ? ' tcs-pagination__page--active' : ''}`}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
        >
          {p}
        </button>
      ))}
      <button
        type="button"
        className="tcs-pagination__nav"
        onClick={() => onChange(page + 1)}
        disabled={page === totalPages}
      >
        Sau →
      </button>
    </nav>
  );
}

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

  // ----- Tìm kiếm + phân trang (lọc ngay trên danh sách đã tải, không gọi lại API) -----
  const [draft, setDraft] = useState(''); // chữ đang gõ
  const [query, setQuery] = useState(''); // từ khoá đã bấm "Tìm"
  const [page, setPage] = useState(1);
  const [minePage, setMinePage] = useState(1);
  const listTopRef = useRef<HTMLDivElement>(null);

  // Tìm không dấu theo tên tin, trung tâm, lớp, môn, khu vực, mô tả, yêu cầu; tin mới đăng lên đầu.
  const filteredPosts = useMemo(() => {
    const q = normalizeName(query);
    return posts
      .filter(
        (p) =>
          !q ||
          [
            p.title,
            p.centerName,
            p.classTitle,
            p.subjectName,
            p.locationLabel,
            p.description,
            p.requirements,
          ].some((field) => normalizeName(field).includes(q)),
      )
      .sort((a, b) => (time(b.publishedAt) || 0) - (time(a.publishedAt) || 0));
  }, [posts, query]);

  // Đổi từ khoá -> quay về trang 1.
  useEffect(() => {
    setPage(1);
  }, [query]);

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedPosts = filteredPosts.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const mineTotalPages = Math.max(1, Math.ceil(myApps.length / PAGE_SIZE));
  const mineCurrentPage = Math.min(minePage, mineTotalPages);
  const pagedApps = myApps.slice((mineCurrentPage - 1) * PAGE_SIZE, mineCurrentPage * PAGE_SIZE);

  /** Sang trang khác thì cuộn về đầu danh sách để đọc từ tin đầu tiên. */
  const goToPage = (setter: (p: number) => void) => (p: number) => {
    setter(p);
    listTopRef.current?.scrollIntoView({ block: 'start' });
  };

  const clearSearch = () => {
    setDraft('');
    setQuery('');
  };

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

        {tab === 'open' && (
          <div className="rc-search">
            <form
              className="tcs-find-search"
              role="search"
              onSubmit={(event) => {
                event.preventDefault();
                setQuery(draft.trim());
              }}
            >
              <div className="tcs-find-search__field">
                <input
                  type="search"
                  className="tcs-find-search__input"
                  placeholder="Tìm theo tên tin, trung tâm, môn học, khu vực..."
                  value={draft}
                  onChange={(event) => {
                    const value = event.target.value;
                    setDraft(value);
                    // Bấm ✕ của trình duyệt (làm rỗng ô) -> bỏ luôn từ khoá đang lọc.
                    if (value === '') setQuery('');
                  }}
                  aria-label="Tìm kiếm tin tuyển dụng"
                />
              </div>
              <button type="submit" className="tcs-find-search__btn">
                Tìm
              </button>
            </form>
          </div>
        )}

        {/* Điểm neo: chuyển trang thì cuộn về đây — đầu danh sách, ngay dưới khung tìm kiếm. */}
        <div ref={listTopRef} className="rc-list-anchor" />

        {status === 'success' && tab === 'open' && (
          <>
            {posts.length === 0 ? (
              <div className="rc-empty">
                <div className="rc-empty__emoji">📭</div>
                <p>Hiện chưa có tin tuyển dụng nào đang mở.</p>
              </div>
            ) : filteredPosts.length === 0 ? (
              <div className="rc-empty">
                <div className="rc-empty__emoji">🔍</div>
                <p>Không có tin nào khớp với “{query}”.</p>
                <button type="button" className="rc-btn rc-btn--ghost rc-btn--sm" onClick={clearSearch}>
                  Xem tất cả tin
                </button>
              </div>
            ) : (
              <>
              <div className="rc-list">
                {pagedPosts.map((p) => {
                  const applied = appliedIds.has(p.recruitmentId);
                  const locked = applied || Boolean(p.alreadyCenterTutor);
                  // Mô tả · Yêu cầu · Quyền lợi xếp thành các ô ngang trải hết bề rộng thẻ.
                  const infoCells = [
                    { label: 'Mô tả', text: p.description },
                    { label: 'Yêu cầu', text: p.requirements },
                    { label: 'Quyền lợi', text: p.benefits },
                  ].filter((cell) => cell.text?.trim());
                  return (
                    <article className="rc-card rc-card--post" key={p.recruitmentId}>
                      <div className="rc-post__head">
                        <h2 className="rc-card__title rc-post__title">{p.title}</h2>
                        <span className="rc-post__date">Đăng {fmtDate(p.publishedAt)}</span>
                      </div>
                      <div className="rc-chips">
                        {p.centerName && <span className="rc-chip">🏫 {p.centerName}</span>}
                        {p.subjectName && <span className="rc-chip">📘 {p.subjectName}</span>}
                        {p.locationLabel && <span className="rc-chip">📍 {p.locationLabel}</span>}
                        <span className="rc-chip">👤 {p.maxPositions} vị trí</span>
                        {!!p.requiredExperience && (
                          <span className="rc-chip">🎓 ≥ {p.requiredExperience} năm KN</span>
                        )}
                        {p.expiresAt && (
                          <ExpiryBadge
                            expiresAt={p.expiresAt}
                            expiredLabel="Đã hết hạn nhận đơn"
                            title={`Tin nhận đơn đến ${new Date(p.expiresAt).toLocaleString('vi-VN')}. Quá hạn trung tâm sẽ phải đăng lại.`}
                          />
                        )}
                      </div>

                      {infoCells.length > 0 && (
                        <dl className="rc-post__info">
                          {infoCells.map((cell) => (
                            <div className="rc-post__cell" key={cell.label}>
                              <dt>{cell.label}</dt>
                              <dd title={cell.text ?? undefined}>{cell.text}</dd>
                            </div>
                          ))}
                        </dl>
                      )}

                      <div className="rc-card__foot rc-post__foot">
                        <span className="rc-count">
                          {p.alreadyCenterTutor
                            ? '🏫 Bạn đã là gia sư của trung tâm này'
                            : applied
                              ? '✓ Bạn đã ứng tuyển tin này'
                              : ''}
                        </span>
                        <div className="rc-actions">
                          <button
                            className={`rc-btn rc-btn--primary rc-btn--sm${locked ? ' rc-btn--locked' : ''}`}
                            type="button"
                            disabled={locked}
                            title={
                              p.alreadyCenterTutor
                                ? 'Bạn đã thuộc đội ngũ của trung tâm này nên không cần ứng tuyển'
                                : undefined
                            }
                            onClick={() => openApply(p)}
                          >
                            {p.alreadyCenterTutor
                              ? 'Đã trong đội ngũ'
                              : applied
                                ? 'Đã ứng tuyển'
                                : 'Ứng tuyển'}
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
              <Pager
                page={currentPage}
                totalPages={totalPages}
                onChange={goToPage(setPage)}
                label="Phân trang tin tuyển dụng"
              />
              </>
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
              <>
              <div className="rc-list">
                {pagedApps.map((a) => {
                  const st = APP_STATUS_LABELS[a.status];
                  return (
                    <article className="rc-card rc-card--post" key={a.recruitmentAppId}>
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
              <Pager
                page={mineCurrentPage}
                totalPages={mineTotalPages}
                onChange={goToPage(setMinePage)}
                label="Phân trang đơn của tôi"
              />
              </>
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
