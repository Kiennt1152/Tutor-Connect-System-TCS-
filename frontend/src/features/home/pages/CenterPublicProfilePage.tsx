import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useLocation } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { ClassListingCard } from '../components/ClassListingCard';
import { mapTutorSearchItem, tutorSearchToFeatured } from '../mappers/tutorSearchMapper';
import { mapOpenClassItem } from '../mappers/openClassMapper';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type { CenterProfile } from '../../marketplace/types/marketplaceTypes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './HomePage.css';
import './TutorPublicProfilePage.css';
import './CenterPublicProfilePage.css';

const SESSION_KEY = 'center-profile-id';

type ProfileTab = 'tutors' | 'classes';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) return 'Không tìm thấy trung tâm này.';
    if (typeof error.response?.data?.message === 'string') return error.response.data.message;
  }
  return fallback;
}

function initialsOf(name: string): string {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(-2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('') || 'TT'
  );
}

/** "08/2026" — tháng/năm trung tâm tham gia nền tảng. */
function formatJoined(iso: string | null): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return `${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
}

export default function CenterPublicProfilePage() {
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  // Giống trang hồ sơ gia sư: centerId đi qua router state, dự phòng sessionStorage để refresh vẫn đúng.
  const stateCenterId = (location.state as { centerId?: number | string } | null)?.centerId;
  const centerId =
    stateCenterId != null ? String(stateCenterId) : (sessionStorage.getItem(SESSION_KEY) ?? undefined);
  useEffect(() => {
    if (stateCenterId != null) sessionStorage.setItem(SESSION_KEY, String(stateCenterId));
  }, [stateCenterId]);

  const [data, setData] = useState<CenterProfile | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [tab, setTab] = useState<ProfileTab>('tutors');

  const load = useCallback(() => {
    if (!centerId) {
      setError('Không xác định được trung tâm. Vui lòng chọn lại từ danh sách.');
      setStatus('error');
      return;
    }
    setStatus('loading');
    marketplaceApi
      .getCenterProfile(centerId)
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được hồ sơ trung tâm.'));
        setStatus('error');
      });
  }, [centerId]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main className="tcs-container tp-page">
        <Link to={APP_ROUTES.centers} className="tp-back">
          ← Quay lại danh sách trung tâm
        </Link>

        {status === 'loading' ? <p className="tp-muted">Đang tải hồ sơ…</p> : null}
        {status === 'error' ? (
          <div className="tp-state">
            <p className="tp-error">{error}</p>
            {centerId ? (
              <button type="button" className="tcs-btn tcs-btn--market" onClick={load}>
                Thử lại
              </button>
            ) : (
              <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.centers}>
                Về danh sách trung tâm
              </Link>
            )}
          </div>
        ) : null}

        {status === 'success' && data ? (
          <>
            <ProfileHeader data={data} />

            <div className="tp-tabs cp-tabs" role="tablist">
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'tutors'}
                className={`tp-tab${tab === 'tutors' ? ' tp-tab--active' : ''}`}
                onClick={() => setTab('tutors')}
              >
                Đội ngũ gia sư
                <span className="cp-tab__count">{data.tutors.length}</span>
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'classes'}
                className={`tp-tab${tab === 'classes' ? ' tp-tab--active' : ''}`}
                onClick={() => setTab('classes')}
              >
                Lớp đang mở
                <span className="cp-tab__count">{data.openClasses.length}</span>
              </button>
            </div>

            {tab === 'tutors' ? (
              data.tutors.length === 0 ? (
                <EmptyPanel
                  title="Chưa có gia sư đang hoạt động"
                  text="Trung tâm chưa có gia sư nào trong đội ngũ. Hãy quay lại sau."
                />
              ) : (
                <div className="tcs-listing-grid tcs-listing-grid--fill">
                  {data.tutors.map((t) => (
                    <TutorListingCard
                      key={t.tutorId}
                      tutor={tutorSearchToFeatured(mapTutorSearchItem(t))}
                      isAuthenticated={isAuthenticated}
                    />
                  ))}
                </div>
              )
            ) : data.openClasses.length === 0 ? (
              <EmptyPanel
                title="Chưa có lớp đang mở"
                text="Trung tâm hiện chưa mở lớp nào để đăng ký."
              />
            ) : (
              // Thẻ lớp dùng cùng lưới 2 cột với bảng tin lớp (thẻ lớp cần khung rộng).
              <div className="cboard-grid">
                {data.openClasses.map((c) => (
                  <ClassListingCard
                    key={c.classId}
                    classItem={mapOpenClassItem(c)}
                    isAuthenticated={isAuthenticated}
                  />
                ))}
              </div>
            )}
          </>
        ) : null}
      </main>
      <SiteFooter />
    </div>
  );
}

function ProfileHeader({ data }: { readonly data: CenterProfile }) {
  const joined = formatJoined(data.joinedAt);
  const hasMeta = Boolean(data.address?.trim() || data.phone?.trim() || joined);
  return (
    <header className="tp-header cp-header">
      <div className="tp-header__avatar cp-header__avatar">
        {data.avatar ? (
          <img src={data.avatar} alt={data.companyName} />
        ) : (
          <span>{initialsOf(data.companyName)}</span>
        )}
      </div>

      <div className="tp-header__main">
        <div className="tp-header__name-row">
          <h1 className="tp-header__name">{data.companyName}</h1>
          {/* API chỉ trả về trung tâm đã xác minh, nên luôn hiện nhãn. */}
          <span className="tcs-listing-card__badge tcs-listing-card__badge--verified cp-badge">
            ✓ Đã xác minh
          </span>
        </div>
        <p className="cp-header__kind">Trung tâm gia sư</p>

        {data.description?.trim() ? (
          <p className="tp-header__bio cp-header__bio">{data.description.trim()}</p>
        ) : null}

        {hasMeta ? (
          <ul className="cp-meta">
            {data.address?.trim() ? (
              <li className="cp-meta__chip">
                <PinIcon />
                {data.address.trim()}
              </li>
            ) : null}
            {data.phone?.trim() ? (
              <li className="cp-meta__chip">
                <PhoneIcon />
                {data.phone.trim()}
              </li>
            ) : null}
            {joined ? (
              <li className="cp-meta__chip">
                <CalendarIcon />
                Tham gia từ {joined}
              </li>
            ) : null}
          </ul>
        ) : null}
      </div>

      <div className="cp-stats">
        <div className="tcs-stat">
          <span className="tcs-stat__value">{data.tutors.length}</span>
          <span className="tcs-stat__label">Gia sư</span>
        </div>
        <div className="tcs-stat">
          <span className="tcs-stat__value">{data.openClasses.length}</span>
          <span className="tcs-stat__label">Lớp đang mở</span>
        </div>
      </div>
    </header>
  );
}

function EmptyPanel({ title, text }: { readonly title: string; readonly text: string }) {
  return (
    <div className="cp-empty">
      <p className="cp-empty__title">{title}</p>
      <p className="cp-empty__text">{text}</p>
    </div>
  );
}

const iconProps = {
  width: 14,
  height: 14,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
};

function PinIcon() {
  return (
    <svg {...iconProps}>
      <path d="M12 21s-7-5.4-7-11a7 7 0 1 1 14 0c0 5.6-7 11-7 11z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

function PhoneIcon() {
  return (
    <svg {...iconProps}>
      <path d="M5 4h3.2l1.8 4.6-2.3 1.4a11 11 0 0 0 6.3 6.3l1.4-2.3L20 15.8V19a1.8 1.8 0 0 1-1.9 1.8A16 16 0 0 1 3.2 5.9 1.8 1.8 0 0 1 5 4z" />
    </svg>
  );
}

function CalendarIcon() {
  return (
    <svg {...iconProps}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M16 3v4M8 3v4M3 10h18" />
    </svg>
  );
}
