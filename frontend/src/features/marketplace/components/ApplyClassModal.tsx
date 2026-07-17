import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import { classToForm } from '../mappers/marketplaceMapper';
import {
  FEE_PER_HOUR_MIN,
  OTHER_SUBJECT,
  type CatalogOption,
  type ClassResponse,
  type Gender,
  type TutorProfileCard,
} from '../types/marketplaceTypes';
import './tutorFindClass.css';

const currency = new Intl.NumberFormat('vi-VN');

const GENDER_LABEL: Record<Gender, string> = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
};

interface Props {
  readonly target: ClassResponse;
  readonly subjects: CatalogOption[];
  /** Học phí đề xuất mặc định (từ tiêu chí gia sư đã nhập), dùng khi lớp không nêu giá môn. */
  readonly defaultRate?: number;
  readonly onClose: () => void;
  readonly onSubmitted: (classId: number) => void;
}

/**
 * Form ứng tuyển: hiển thị hồ sơ gia sư (lấy từ profile) rồi mới xác nhận gửi đơn.
 * Phần dữ liệu profile (GET /profile/me) là NON-BLOCKING — nếu chưa có/không tải được
 * thì form vẫn hiện đầy đủ bố cục, các mục hồ sơ để trống. (Phần profile do người khác ráp.)
 */
export function ApplyClassModal({ target, subjects, defaultRate, onClose, onSubmitted }: Props) {
  const [profile, setProfile] = useState<TutorProfileCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [coverLetter, setCoverLetter] = useState('Tôi quan tâm và mong muốn nhận lớp này.');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Môn của lớp + học phí Client đề xuất cho từng môn (bóc từ detailsJson).
  const form = useMemo(() => classToForm(target), [target]);
  const subjectIds = form.subjectIds;
  const subjectName = useMemo(() => {
    const m = new Map(subjects.map((s) => [String(s.id), s.name]));
    return (id: string) =>
      id === OTHER_SUBJECT ? form.subjectOther.trim() || 'Môn khác' : (m.get(id) ?? `#${id}`);
  }, [subjects, form.subjectOther]);
  const askingFee = (id: string) => Number(form.subjectFees[id]) || 0;

  // Giá gia sư đề xuất theo từng môn — mặc định lấy đúng mức Client đề xuất cho môn đó.
  const [rates, setRates] = useState<Record<string, string>>({});

  // Đặt lại ô nhập theo môn của lớp đang mở: modal có thể được dùng lại cho lớp khác
  // mà không unmount, khi đó state cũ còn key của lớp trước.
  useEffect(() => {
    setRates(
      Object.fromEntries(
        form.subjectIds.map((id) => {
          const fee = Number(form.subjectFees[id]) || 0;
          return [id, fee > 0 ? String(fee) : ''];
        }),
      ),
    );
  }, [form]);

  // Môn lớp không nêu giá: gợi ý theo tiêu chí gia sư đã nhập, rồi tới mức trong hồ sơ.
  // Chỉ điền vào ô còn trống nên không đè lên giá gia sư đã tự gõ.
  useEffect(() => {
    const fallback = defaultRate || (profile?.hourlyRate ? Math.round(Number(profile.hourlyRate)) : 0);
    if (!fallback) return;
    setRates((prev) =>
      Object.fromEntries(Object.entries(prev).map(([id, v]) => [id, v || String(fallback)])),
    );
  }, [form, profile, defaultRate]);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .getMyTutorProfile()
      .then((p) => alive && setProfile(p))
      .catch(() => {
        /* Bỏ qua: form vẫn hiển thị, mục hồ sơ để trống. */
      })
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, []);

  const birthYear = useMemo(
    () => (profile?.dateOfBirth ? profile.dateOfBirth.slice(0, 4) : null),
    [profile?.dateOfBirth],
  );

  // Phải báo giá đủ mọi môn của lớp, mỗi môn không dưới mức tối thiểu.
  const rateErrors = subjectIds
    .map((id) => {
      const fee = Number(rates[id]);
      if (!(fee > 0)) return `${subjectName(id)}: chưa nhập học phí/giờ`;
      if (fee < FEE_PER_HOUR_MIN)
        return `${subjectName(id)}: học phí/giờ tối thiểu ${currency.format(FEE_PER_HOUR_MIN)}đ`;
      return null;
    })
    .filter((e): e is string => e !== null);

  function setRate(subjectId: string, value: string) {
    setRates((prev) => ({ ...prev, [subjectId]: value.replace(/\D/g, '') }));
    // Lỗi của lần gửi trước không còn đúng khi đã sửa form — bỏ đi cho khỏi gây hiểu nhầm.
    setError(null);
  }

  async function handleSubmit() {
    if (rateErrors.length > 0) return;
    setSubmitting(true);
    setError(null);
    try {
      await marketplaceApi.applyToClass(target.classId, {
        proposedRates: Object.fromEntries(subjectIds.map((id) => [id, Number(rates[id])])),
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
            <h3>Học phí đề xuất theo môn</h3>
            <p className="apl-rates__intro">
              Lớp này gồm {subjectIds.length} môn — hãy báo giá cho <strong>từng môn</strong> bạn sẽ
              dạy.
            </p>
            <ul className="apl-rates">
              {subjectIds.map((id) => {
                const asking = askingFee(id);
                const mine = Number(rates[id]) || 0;
                return (
                  <li key={id} className="apl-rate">
                    <div className="apl-rate__head">
                      <span className="apl-rate__subject">📘 {subjectName(id)}</span>
                      <span className="apl-rate__asking">
                        {asking > 0
                          ? `Phụ huynh đề xuất: ${currency.format(asking)}đ/giờ`
                          : 'Phụ huynh chưa nêu mức học phí'}
                      </span>
                    </div>
                    <div className="apl-rate__input">
                      <input
                        type="text"
                        inputMode="numeric"
                        value={rates[id] ?? ''}
                        placeholder={`VD: ${asking || 200000}`}
                        aria-label={`Học phí đề xuất môn ${subjectName(id)} (đ/giờ)`}
                        onChange={(e) => setRate(id, e.target.value)}
                      />
                      <span className="apl-rate__unit">đ/giờ</span>
                    </div>
                    {mine > 0 && (
                      <small className="tfc-hint">
                        {currency.format(mine)}đ/giờ
                        {asking > 0 && mine > asking && (
                          <span className="apl-rate__over">
                            {' '}
                            · cao hơn phụ huynh đề xuất {currency.format(mine - asking)}đ
                          </span>
                        )}
                      </small>
                    )}
                  </li>
                );
              })}
            </ul>
            {rateErrors.length > 0 && (
              <ul className="apl-rate-errors">
                {rateErrors.map((e) => (
                  <li key={e}>{e}</li>
                ))}
              </ul>
            )}
          </section>

          <section className="cdm-section">
            <h3>Thông tin ứng tuyển</h3>
            <label className="apl-field">
              <span>Lời nhắn tới phụ huynh</span>
              <textarea
                rows={3}
                value={coverLetter}
                placeholder="Giới thiệu ngắn gọn vì sao bạn phù hợp với lớp này…"
                onChange={(e) => {
                  setCoverLetter(e.target.value);
                  setError(null);
                }}
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
            disabled={submitting || rateErrors.length > 0}
            title={rateErrors.length > 0 ? 'Hãy nhập học phí cho tất cả các môn' : undefined}
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
