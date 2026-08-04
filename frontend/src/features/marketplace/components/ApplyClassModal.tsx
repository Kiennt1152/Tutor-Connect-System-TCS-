import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import { classToForm } from '../mappers/marketplaceMapper';
import {
  FEE_PER_HOUR_MIN,
  isOtherSubject,
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
  readonly defaultRate?: number;
  readonly onClose: () => void;
  readonly onSubmitted: (classId: number) => void;
}

export function ApplyClassModal({ target, subjects, defaultRate, onClose, onSubmitted }: Props) {
  const [profile, setProfile] = useState<TutorProfileCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [coverLetter, setCoverLetter] = useState('Tôi quan tâm và mong muốn nhận lớp này.');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const form = useMemo(() => classToForm(target), [target]);
  const subjectIds = form.subjectIds;
  const subjectName = useMemo(() => {
    const m = new Map(subjects.map((s) => [String(s.id), s.name]));
    return (id: string) =>
      isOtherSubject(id) ? form.subjectOthers[id]?.trim() || 'Môn khác' : (m.get(id) ?? `#${id}`);
  }, [subjects, form.subjectOthers]);
  const askingFee = (id: string) => Number(form.subjectFees[id]) || 0;

  const [rates, setRates] = useState<Record<string, string>>({});
  const [selected, setSelected] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setRates(
      Object.fromEntries(
        form.subjectIds.map((id) => {
          const fee = Number(form.subjectFees[id]) || 0;
          return [id, fee > 0 ? String(fee) : ''];
        }),
      ),
    );
    setSelected(Object.fromEntries(form.subjectIds.map((id) => [id, true])));
  }, [form]);

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

  const chosenIds = subjectIds.filter((id) => selected[id]);

  const rateErrors = chosenIds
    .map((id) => {
      const fee = Number(rates[id]);
      if (!(fee > 0)) return `${subjectName(id)}: chưa nhập học phí/giờ`;
      if (fee < FEE_PER_HOUR_MIN)
        return `${subjectName(id)}: học phí/giờ tối thiểu ${currency.format(FEE_PER_HOUR_MIN)}đ`;
      return null;
    })
    .filter((e): e is string => e !== null);
  const noSubjectChosen = chosenIds.length === 0;

  function setRate(subjectId: string, value: string) {
    setRates((prev) => ({ ...prev, [subjectId]: value.replace(/\D/g, '') }));
    setError(null);
  }

  function toggleSubject(subjectId: string) {
    setSelected((prev) => ({ ...prev, [subjectId]: !prev[subjectId] }));
    setError(null);
  }

  async function handleSubmit() {
    if (noSubjectChosen || rateErrors.length > 0) return;
    setSubmitting(true);
    setError(null);
    try {
      await marketplaceApi.applyToClass(target.classId, {
        proposedRates: Object.fromEntries(chosenIds.map((id) => [id, Number(rates[id])])),
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
              Lớp này gồm {subjectIds.length} môn — <strong>tích chọn môn bạn muốn dạy</strong> và báo
              giá cho môn đó.
            </p>
            <ul className="apl-rates">
              {subjectIds.map((id) => {
                const asking = askingFee(id);
                const mine = Number(rates[id]) || 0;
                const isOn = !!selected[id];
                return (
                  <li key={id} className={`apl-rate ${isOn ? '' : 'is-off'}`}>
                    <div className="apl-rate__head">
                      <label className="apl-rate__subject apl-rate__check">
                        <input
                          type="checkbox"
                          checked={isOn}
                          onChange={() => toggleSubject(id)}
                          aria-label={`Dạy môn ${subjectName(id)}`}
                        />
                        <span>📘 {subjectName(id)}</span>
                      </label>
                      <span className="apl-rate__asking">
                        {asking > 0
                          ? `Phụ huynh đề xuất: ${currency.format(asking)}đ/giờ`
                          : 'Phụ huynh chưa nêu mức học phí'}
                      </span>
                    </div>
                    {isOn && (
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
                    )}
                    {isOn && mine > 0 && (
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
            {noSubjectChosen && (
              <ul className="apl-rate-errors">
                <li>Hãy chọn ít nhất một môn bạn muốn dạy.</li>
              </ul>
            )}
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
            disabled={submitting || noSubjectChosen || rateErrors.length > 0}
            title={
              noSubjectChosen
                ? 'Hãy chọn ít nhất một môn'
                : rateErrors.length > 0
                  ? 'Hãy nhập học phí cho các môn đã chọn'
                  : undefined
            }
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
