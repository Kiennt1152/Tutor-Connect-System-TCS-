import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import type { ClassResponse, Gender, TutorProfileCard } from '../types/marketplaceTypes';
import './tutorFindClass.css';

const currency = new Intl.NumberFormat('vi-VN');

const GENDER_LABEL: Record<Gender, string> = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
};

interface Props {
  readonly target: ClassResponse;
  /** Học phí đề xuất mặc định (từ tiêu chí gia sư đã nhập). */
  readonly defaultRate?: number;
  readonly onClose: () => void;
  readonly onSubmitted: (classId: number) => void;
}

/**
 * Form ứng tuyển: hiển thị hồ sơ gia sư (lấy từ profile) rồi mới xác nhận gửi đơn.
 * Phần dữ liệu profile (GET /profile/me) là NON-BLOCKING — nếu chưa có/không tải được
 * thì form vẫn hiện đầy đủ bố cục, các mục hồ sơ để trống. (Phần profile do người khác ráp.)
 */
export function ApplyClassModal({ target, defaultRate, onClose, onSubmitted }: Props) {
  const [profile, setProfile] = useState<TutorProfileCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [coverLetter, setCoverLetter] = useState('Tôi quan tâm và mong muốn nhận lớp này.');
  const [rate, setRate] = useState<string>(defaultRate ? String(defaultRate) : '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .getMyTutorProfile()
      .then((p) => {
        if (!alive) return;
        setProfile(p);
        // Nếu chưa có học phí đề xuất, gợi ý theo mức trong hồ sơ.
        if (!defaultRate && p.hourlyRate) setRate(String(Math.round(Number(p.hourlyRate))));
      })
      .catch(() => {
        /* Bỏ qua: form vẫn hiển thị, mục hồ sơ để trống. */
      })
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [defaultRate]);

  const birthYear = useMemo(
    () => (profile?.dateOfBirth ? profile.dateOfBirth.slice(0, 4) : null),
    [profile?.dateOfBirth],
  );

  async function handleSubmit() {
    setSubmitting(true);
    setError(null);
    try {
      await marketplaceApi.applyToClass(target.classId, {
        proposedRate: Number(rate) || undefined,
        coverLetter: coverLetter.trim() || undefined,
      });
      onSubmitted(target.classId);
    } catch (err) {
      setError(extractError(err));
      setSubmitting(false);
    }
  }

  const educations = profile?.educations ?? [];
  const certificates = profile?.certificates ?? [];
  const fullName = profile?.fullName?.trim() || 'Họ và tên gia sư';
  const initials = (profile?.fullName?.trim() || 'GS')
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  return (
    <div
      className="cdm-overlay"
      role="dialog"
      aria-modal="true"
      onMouseDown={(e) => {
        // Chỉ đóng khi bấm trực tiếp trên nền — tránh đóng nhầm khi bôi đen text rồi thả ra ngoài.
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="cdm">
        <button type="button" className="cdm__close" aria-label="Đóng" onClick={onClose}>
          ✕
        </button>

        <header className="cdm__head">
          <span className="cdm__id">Ứng tuyển lớp</span>
          <h2 className="cdm__title">{target.title}</h2>
        </header>

        <div className="cdm__body">
          <p className="apl-intro">
            Phụ huynh sẽ xem hồ sơ dưới đây khi bạn ứng tuyển. Hãy đảm bảo thông tin chính xác — cập
            nhật ở trang <strong>Hồ sơ</strong> nếu cần.
            {loading && <span className="cdm-muted"> (đang tải hồ sơ…)</span>}
          </p>

          {/* Thẻ hồ sơ gia sư */}
          <div className="apl-card">
            <div className="apl-card__avatar">
              {profile?.avatarUrl ? (
                <img src={profile.avatarUrl} alt={fullName} />
              ) : (
                <span>{initials || '?'}</span>
              )}
            </div>
            <div className="apl-card__head">
              <h3 className="apl-card__name">{fullName}</h3>
              {profile?.verificationStatus === 'VERIFIED' && (
                <span className="apl-verified">✓ Đã xác minh</span>
              )}
              <div className="apl-card__sub">
                {birthYear && <span>🎂 Năm sinh {birthYear}</span>}
                {profile?.gender && <span>👤 {GENDER_LABEL[profile.gender]}</span>}
                {profile?.experienceYears != null && (
                  <span>🎓 {profile.experienceYears} năm kinh nghiệm</span>
                )}
              </div>
              <p className="apl-card__addr">📍 {profile?.address?.trim() || 'Chưa cập nhật địa chỉ'}</p>
            </div>
          </div>

          {/* Học vấn & bằng cấp */}
          <section className="cdm-section">
            <h3>Trình độ học vấn &amp; bằng cấp</h3>
            {educations.length > 0 ? (
              <ul className="apl-list">
                {educations.map((e) => (
                  <li key={e.educationId}>
                    <strong>{e.institution}</strong>
                    <div className="apl-list__meta">
                      {e.degree}
                      {e.fieldOfStudy ? ` · ${e.fieldOfStudy}` : ''}
                      {e.startYear || e.endYear
                        ? ` · ${e.startYear ?? '?'}–${e.endYear ?? 'nay'}`
                        : ''}
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="cdm-muted">Chưa cập nhật học vấn trong hồ sơ.</p>
            )}
          </section>

          {/* Điểm nổi bật (GPA / điểm thi / danh hiệu) — lưu ở phần giới thiệu */}
          <section className="cdm-section">
            <h3>Điểm nổi bật &amp; giới thiệu</h3>
            {profile?.bio?.trim() ? (
              <p className="cdm-note">{profile.bio.trim()}</p>
            ) : (
              <p className="cdm-muted">Chưa cập nhật giới thiệu / điểm nổi bật.</p>
            )}
          </section>

          {/* Chứng chỉ */}
          <section className="cdm-section">
            <h3>Chứng chỉ</h3>
            {certificates.length > 0 ? (
              <ul className="apl-chips">
                {certificates.map((c) => (
                  <li key={c.certificateId} className="apl-chip">
                    🏅 {c.name}
                    <span className="apl-chip__issuer">
                      {c.issuer}
                      {c.issueDate ? ` · ${c.issueDate.slice(0, 4)}` : ''}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="cdm-muted">Chưa cập nhật chứng chỉ trong hồ sơ.</p>
            )}
          </section>

          {/* Nội dung ứng tuyển */}
          <section className="cdm-section">
            <h3>Thông tin ứng tuyển</h3>
            <label className="apl-field">
              <span>Học phí đề xuất (đ/giờ)</span>
              <input
                type="text"
                inputMode="numeric"
                value={rate}
                placeholder="VD: 200000"
                onChange={(e) => setRate(e.target.value.replace(/\D/g, ''))}
              />
              {Number(rate) > 0 && (
                <small className="tfc-hint">{currency.format(Number(rate))}đ/giờ</small>
              )}
            </label>
            <label className="apl-field">
              <span>Lời nhắn tới phụ huynh</span>
              <textarea
                rows={3}
                value={coverLetter}
                placeholder="Giới thiệu ngắn gọn vì sao bạn phù hợp với lớp này…"
                onChange={(e) => setCoverLetter(e.target.value)}
              />
            </label>
          </section>

          {error && <p className="apl-error">{error}</p>}
        </div>

        <footer className="cdm__foot">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onClose}>
            Hủy
          </button>
          <button
            type="button"
            className="tfc-btn tfc-btn--primary"
            disabled={submitting}
            onClick={handleSubmit}
          >
            {submitting ? 'Đang gửi…' : 'Gửi đơn ứng tuyển'}
          </button>
        </footer>
      </div>
    </div>
  );
}

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
