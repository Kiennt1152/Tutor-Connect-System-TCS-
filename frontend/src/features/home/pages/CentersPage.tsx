import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type {
  CenterSummary,
  ClassRequestPayload,
} from '../../marketplace/types/marketplaceTypes';
import { ClassRequestForm } from '../../marketplace/components/ClassRequestForm';
import { emptyForm } from '../../marketplace/mappers/marketplaceMapper';
import { profileApi } from '../../profile/api/profileApi';
import { useTutorRequestForm } from '../hooks/useTutorRequestForm';
import './HomePage.css';
import './CentersRequest.css';

function extractError(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

/**
 * Trang "Trung tâm":
 * - Mọi người: danh sách trung tâm đã xác minh.
 * - Phụ huynh (CLIENT): gửi yêu cầu mở lớp tới một trung tâm + theo dõi yêu cầu đã gửi.
 *
 * Gia sư xem/ứng tuyển tin tuyển dụng ở trang "Tin tuyển dụng" riêng (không lặp ở đây).
 */
export default function CentersPage() {
  const { user } = useAuth();
  const isClient = hasRole(user?.role, 'CLIENT');

  const [centers, setCenters] = useState<CenterSummary[]>([]);
  const [centersLoading, setCentersLoading] = useState(true);

  useEffect(() => {
    marketplaceApi
      .listCenters()
      .then((res) => setCenters(res.data))
      .catch(() => setCenters([]))
      .finally(() => setCentersLoading(false));
  }, []);

  // ----- Modal gửi yêu cầu (dùng lại form "tìm gia sư" cho rõ ràng) -----
  const { subjects, grades } = useTutorRequestForm();
  const [target, setTarget] = useState<CenterSummary | null>(null);
  const [sending, setSending] = useState(false);
  const [modalError, setModalError] = useState('');
  // Chỉ phụ huynh đã nhập đủ CCCD mới được gửi yêu cầu.
  const [cccdComplete, setCccdComplete] = useState<boolean | null>(null);
  // Thông báo thành công (toast trong app, không dùng alert trình duyệt).
  const [notice, setNotice] = useState('');

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
      setNotice('Đã gửi yêu cầu nhờ trung tâm tìm gia sư. Theo dõi ở trang “Yêu cầu của tôi”.');
      window.setTimeout(() => setNotice(''), 6000);
    } catch (err) {
      setModalError(extractError(err, 'Không gửi được yêu cầu.'));
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="tcs-page">
      <HomeNavbar />
      {notice && (
        <div className="cr-toast" role="status">
          <span className="cr-toast__icon" aria-hidden="true">✓</span>
          <span className="cr-toast__msg">{notice}</span>
          <Link className="cr-toast__link" to={APP_ROUTES.marketplace}>
            Xem
          </Link>
          <button
            type="button"
            className="cr-toast__x"
            aria-label="Đóng thông báo"
            onClick={() => setNotice('')}
          >
            ×
          </button>
        </div>
      )}
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
