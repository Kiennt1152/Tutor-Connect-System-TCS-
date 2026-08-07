import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type { CenterSummary, ClassRequest } from '../../marketplace/types/marketplaceTypes';
import { useOpenRecruitmentPosts } from '../hooks/useOpenRecruitmentPosts';
import './HomePage.css';
import './CentersRequest.css';

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

function extractError(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

const STATUS_LABEL: Record<ClassRequest['status'], string> = {
  PENDING: 'Đang chờ',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
};
const STATUS_CLASS: Record<ClassRequest['status'], string> = {
  PENDING: 'cr-badge cr-badge--pending',
  ACCEPTED: 'cr-badge cr-badge--accepted',
  REJECTED: 'cr-badge cr-badge--rejected',
};

/**
 * Trang "Trung tâm":
 * - Mọi người: danh sách trung tâm đã xác minh.
 * - Gia sư: tin tuyển dụng đang mở.
 * - Phụ huynh (CLIENT): gửi yêu cầu mở lớp tới một trung tâm + theo dõi yêu cầu đã gửi.
 */
export default function CentersPage() {
  const { user } = useAuth();
  const isTutor = hasRole(user?.role, 'TUTOR');
  const isClient = hasRole(user?.role, 'CLIENT');
  const { status: postsStatus, posts, reload: reloadPosts } = useOpenRecruitmentPosts(isTutor);

  const [centers, setCenters] = useState<CenterSummary[]>([]);
  const [centersLoading, setCentersLoading] = useState(true);

  const [myRequests, setMyRequests] = useState<ClassRequest[]>([]);
  const reloadRequests = useCallback(() => {
    if (!isClient) return;
    marketplaceApi
      .getMyClassRequests()
      .then((res) => setMyRequests(res.data))
      .catch(() => setMyRequests([]));
  }, [isClient]);

  useEffect(() => {
    marketplaceApi
      .listCenters()
      .then((res) => setCenters(res.data))
      .catch(() => setCenters([]))
      .finally(() => setCentersLoading(false));
  }, []);

  useEffect(() => {
    reloadRequests();
  }, [reloadRequests]);

  // ----- Modal gửi yêu cầu -----
  const [target, setTarget] = useState<CenterSummary | null>(null);
  const [note, setNote] = useState('');
  const [budget, setBudget] = useState('');
  const [sending, setSending] = useState(false);
  const [modalError, setModalError] = useState('');

  const openModal = (center: CenterSummary) => {
    setTarget(center);
    setNote('');
    setBudget('');
    setModalError('');
  };
  const closeModal = () => setTarget(null);

  const submitRequest = async () => {
    if (!target) return;
    if (!note.trim()) {
      setModalError('Vui lòng nhập nội dung nguyện vọng.');
      return;
    }
    setSending(true);
    setModalError('');
    try {
      await marketplaceApi.createClassRequest(target.centerId, {
        note: note.trim(),
        desiredBudget: budget.trim() ? Number(budget) : null,
      });
      setTarget(null);
      reloadRequests();
    } catch (err) {
      setModalError(extractError(err, 'Không gửi được yêu cầu.'));
    } finally {
      setSending(false);
    }
  };

  const cancelRequest = async (requestId: string) => {
    try {
      await marketplaceApi.cancelClassRequest(requestId);
      reloadRequests();
    } catch (err) {
      alert(extractError(err, 'Không hủy được yêu cầu.'));
    }
  };

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main>
        <section className="tcs-section tcs-section--centers">
          <div className="tcs-container">
            <div className="tcs-section-bar">
              <div>
                <h1 className="tcs-section-bar__title">Trung tâm</h1>
                <p className="tcs-section-bar__subtitle">
                  Các trung tâm gia sư đối tác — quy trình tuyển chọn và hỗ trợ chuyên nghiệp.
                </p>
              </div>
            </div>

            {/* Gia sư: tin tuyển dụng đang mở từ các trung tâm. */}
            {isTutor && (
              <div className="tcs-recruit">
                <div className="tcs-section-bar">
                  <div>
                    <h2 className="tcs-recruit__title">Tin tuyển gia sư</h2>
                    <p className="tcs-section-bar__subtitle">
                      Các trung tâm đang tuyển — xem chi tiết và gửi đơn ứng tuyển.
                    </p>
                  </div>
                  {postsStatus === 'success' && posts.length > 0 ? (
                    <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to={APP_ROUTES.recruitment}>
                      Xem tất cả ({posts.length})
                    </Link>
                  ) : null}
                </div>

                {postsStatus === 'loading' && (
                  <div className="tcs-search-results__state">
                    <span className="tcs-spinner" aria-hidden="true" />
                    Đang tải tin tuyển dụng...
                  </div>
                )}
                {postsStatus === 'error' && (
                  <div className="tcs-search-results__state tcs-search-results__state--error">
                    Không thể tải tin tuyển dụng.
                    <button
                      type="button"
                      className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                      onClick={reloadPosts}
                    >
                      Thử lại
                    </button>
                  </div>
                )}
                {postsStatus === 'success' && posts.length === 0 && (
                  <p className="tcs-empty">Hiện chưa có tin tuyển gia sư nào đang mở.</p>
                )}
                {postsStatus === 'success' && posts.length > 0 && (
                  <div className="tcs-recruit__grid">
                    {posts.map((post) => (
                      <article key={post.recruitmentId} className="tcs-recruit-card">
                        <h3 className="tcs-recruit-card__title">{post.title}</h3>
                        <div className="tcs-recruit-card__chips">
                          {post.centerName && <span className="tcs-chip">🏫 {post.centerName}</span>}
                          {post.subjectName && (
                            <span className="tcs-chip">📘 {post.subjectName}</span>
                          )}
                          {post.locationLabel && (
                            <span className="tcs-chip">📍 {post.locationLabel}</span>
                          )}
                          <span className="tcs-chip">👤 {post.maxPositions} vị trí</span>
                        </div>
                        <p className="tcs-recruit-card__desc">{post.description}</p>
                        <Link
                          className="tcs-btn tcs-btn--market tcs-btn--sm"
                          to={APP_ROUTES.recruitment}
                        >
                          Ứng tuyển
                        </Link>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Phụ huynh: yêu cầu mở lớp đã gửi. */}
            {isClient && myRequests.length > 0 && (
              <div className="tcs-recruit">
                <div className="tcs-section-bar">
                  <div>
                    <h2 className="tcs-recruit__title">Yêu cầu mở lớp của tôi</h2>
                    <p className="tcs-section-bar__subtitle">
                      Theo dõi trạng thái các yêu cầu bạn đã gửi tới trung tâm.
                    </p>
                  </div>
                </div>
                <div className="cr-req-list">
                  {myRequests.map((r) => (
                    <div key={r.requestId} className="cr-req">
                      <div className="cr-req__main">
                        <p className="cr-req__note">{r.note}</p>
                        <span className="cr-req__meta">
                          Gửi tới: {r.centerName ?? '—'}
                          {r.desiredBudget != null && ` · Ngân sách: ${currency(r.desiredBudget)}đ`}
                          {r.status === 'REJECTED' && r.reason && ` · Lý do: ${r.reason}`}
                        </span>
                      </div>
                      <span className={STATUS_CLASS[r.status]}>{STATUS_LABEL[r.status]}</span>
                      {r.status === 'PENDING' && (
                        <button
                          type="button"
                          className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                          onClick={() => cancelRequest(r.requestId)}
                        >
                          Hủy
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Danh sách trung tâm đã xác minh (thật). */}
            <div className="tcs-section-bar">
              <div>
                <h2 className="tcs-recruit__title">Danh sách trung tâm</h2>
                <p className="tcs-section-bar__subtitle">
                  {isClient
                    ? 'Chọn một trung tâm và gửi yêu cầu mở lớp theo nguyện vọng của bạn.'
                    : 'Các trung tâm đã được xác minh trên nền tảng.'}
                </p>
              </div>
            </div>

            {centersLoading && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách trung tâm...
              </div>
            )}
            {!centersLoading && centers.length === 0 && (
              <p className="tcs-empty">Hiện chưa có trung tâm nào được xác minh.</p>
            )}
            {!centersLoading && centers.length > 0 && (
              <div className="cr-grid">
                {centers.map((center) => (
                  <article key={center.centerId} className="cr-card">
                    <h3 className="cr-card__name">{center.companyName}</h3>
                    {center.description && <p className="cr-card__desc">{center.description}</p>}
                    {center.address && <span className="cr-card__meta">📍 {center.address}</span>}
                    {isClient && (
                      <div className="cr-card__actions">
                        <button
                          type="button"
                          className="tcs-btn tcs-btn--market tcs-btn--sm"
                          onClick={() => openModal(center)}
                        >
                          Gửi yêu cầu mở lớp
                        </button>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      </main>

      {/* Modal gửi yêu cầu mở lớp */}
      {target && (
        <div className="cr-overlay" role="dialog" aria-modal="true" onClick={closeModal}>
          <div className="cr-modal" onClick={(e) => e.stopPropagation()}>
            <h3 className="cr-modal__title">Gửi yêu cầu mở lớp</h3>
            <p className="cr-modal__subtitle">Gửi tới: {target.companyName}</p>

            {modalError && <p className="cr-modal__error">{modalError}</p>}

            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-note">
                Nguyện vọng của bạn *
              </label>
              <textarea
                id="cr-note"
                className="cr-textarea"
                placeholder="Ví dụ: Cần gia sư Toán lớp 9, học 2 buổi/tuần tại nhà (Quận 7), trình độ khá..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
              />
            </div>

            <div className="cr-field">
              <label className="cr-field__label" htmlFor="cr-budget">
                Ngân sách mong muốn (đ/buổi) — tuỳ chọn
              </label>
              <input
                id="cr-budget"
                className="cr-input"
                type="number"
                min={0}
                placeholder="Ví dụ: 200000"
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
              />
            </div>

            <div className="cr-modal__actions">
              <button type="button" className="tcs-btn tcs-btn--ghost tcs-btn--sm" onClick={closeModal}>
                Hủy
              </button>
              <button
                type="button"
                className="tcs-btn tcs-btn--market tcs-btn--sm"
                onClick={submitRequest}
                disabled={sending}
              >
                {sending ? 'Đang gửi...' : 'Gửi yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}
      <SiteFooter />
    </div>
  );
}
