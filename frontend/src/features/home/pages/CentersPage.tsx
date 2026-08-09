import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type {
  CenterSummary,
  ClassRequest,
  ClassRequestPayload,
} from '../../marketplace/types/marketplaceTypes';
import { ClassRequestForm } from '../../marketplace/components/ClassRequestForm';
import { emptyForm } from '../../marketplace/mappers/marketplaceMapper';
import { profileApi } from '../../profile/api/profileApi';
import { useOpenRecruitmentPosts } from '../hooks/useOpenRecruitmentPosts';
import { useTutorRequestForm } from '../hooks/useTutorRequestForm';
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
  SEARCHING: 'Đang tìm gia sư',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
};
const STATUS_CLASS: Record<ClassRequest['status'], string> = {
  PENDING: 'cr-badge cr-badge--pending',
  SEARCHING: 'cr-badge cr-badge--pending',
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

  // ----- Modal gửi yêu cầu (dùng lại form "tìm gia sư" cho rõ ràng) -----
  const { subjects, grades } = useTutorRequestForm();
  const [target, setTarget] = useState<CenterSummary | null>(null);
  const [sending, setSending] = useState(false);
  const [modalError, setModalError] = useState('');
  // Chỉ phụ huynh đã nhập đủ CCCD mới được gửi yêu cầu.
  const [cccdComplete, setCccdComplete] = useState<boolean | null>(null);

  useEffect(() => {
    if (!isClient) return;
    profileApi
      .getMyCccd()
      .then((res) => setCccdComplete(Boolean(res.data.complete)))
      .catch(() => setCccdComplete(false));
  }, [isClient]);

  const openModal = (center: CenterSummary) => {
    setTarget(center);
    setModalError('');
  };
  const closeModal = () => setTarget(null);

  // Gửi yêu cầu tới trung tâm: đính nguyên payload form vào detailsJson để trung tâm xem đủ.
  const submitRequest = async (payload: ClassRequestPayload) => {
    if (!target) return;
    setSending(true);
    setModalError('');
    try {
      const note =
        payload.description?.trim() || 'Yêu cầu tìm gia sư (xem thông tin chi tiết đính kèm).';
      await marketplaceApi.createClassRequest(target.centerId, {
        note,
        desiredBudget: payload.budget ?? payload.tuitionFee ?? null,
        detailsJson: JSON.stringify(payload),
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
                          Nhờ trung tâm tìm gia sư
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

      {/* Modal gửi yêu cầu mở lớp — dùng lại form "tìm gia sư" cho đầy đủ thông tin */}
      {target && (
        <div className="cr-overlay" role="dialog" aria-modal="true" onClick={closeModal}>
          <div
            className="cr-modal"
            style={{ maxHeight: '88vh', overflowY: 'auto', maxWidth: 720 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="cr-modal__title">Nhờ trung tâm tìm gia sư</h3>
            <p className="cr-modal__subtitle">Gửi tới: {target.companyName}</p>

            {modalError && <p className="cr-modal__error">{modalError}</p>}

            {cccdComplete === false ? (
              <div style={{ padding: '8px 0' }}>
                <p style={{ color: '#9a3412', marginTop: 0 }}>
                  Bạn cần nhập đầy đủ <strong>thông tin CCCD</strong> trong hồ sơ trước khi gửi yêu
                  cầu tới trung tâm.
                </p>
                <div className="cr-modal__actions">
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                    onClick={closeModal}
                  >
                    Đóng
                  </button>
                  <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.profile}>
                    Đi tới hồ sơ nhập CCCD →
                  </Link>
                </div>
              </div>
            ) : (
              <ClassRequestForm
                initial={emptyForm()}
                subjects={subjects}
                grades={grades}
                isEdit={false}
                submitting={sending}
                error={modalError}
                onSubmit={submitRequest}
                onCancel={closeModal}
                freeTextSubjects
              />
            )}
          </div>
        </div>
      )}
      <SiteFooter />
    </div>
  );
}
