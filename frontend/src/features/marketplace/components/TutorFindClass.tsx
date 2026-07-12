import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import {
  DAY_OF_WEEK_OPTIONS,
  SESSION_OPTIONS,
  OTHER_SUBJECT,
  type CatalogOption,
  type ClassResponse,
} from '../types/marketplaceTypes';
import {
  CRITERIA_LABELS,
  TUTOR_LEVEL_OPTIONS,
  emptyCriteria,
  rankClasses,
  type MatchResult,
  type MatchWeights,
  type TutorCriteria,
} from '../matching/tutorMatching';
import './tutorFindClass.css';

const currency = new Intl.NumberFormat('vi-VN');

const WEIGHT_LABELS: { key: keyof MatchWeights; label: string; hint: string }[] = [
  { key: 'subject', label: 'Môn & lớp (S)', hint: 'Đúng môn, đúng khối lớp bạn dạy' },
  { key: 'location', label: 'Địa điểm (L)', hint: 'Gần bạn / học online' },
  { key: 'salary', label: 'Học phí (P)', hint: 'Đạt mức bạn mong muốn' },
  { key: 'schedule', label: 'Lịch học (T)', hint: 'Trùng khung giờ bạn rảnh' },
  { key: 'experience', label: 'Trình độ (E)', hint: 'Phù hợp yêu cầu bằng cấp' },
];

interface Props {
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly provinces: CatalogOption[];
}

export function TutorFindClass({ subjects, grades, provinces }: Props) {
  const [criteria, setCriteria] = useState<TutorCriteria>(emptyCriteria);
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [applied, setApplied] = useState<Set<number>>(new Set());
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [detailTarget, setDetailTarget] = useState<ClassResponse | null>(null);
  const [districts, setDistricts] = useState<CatalogOption[]>([]);

  useEffect(() => {
    setStatus('loading');
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch(() => setStatus('error'));
  }, []);

  // Tải danh sách quận/huyện theo tỉnh đã chọn.
  useEffect(() => {
    if (!criteria.provinceId) {
      setDistricts([]);
      return;
    }
    marketplaceApi
      .listDistricts(Number(criteria.provinceId))
      .then(setDistricts)
      .catch(() => setDistricts([]));
  }, [criteria.provinceId]);

  const subjectName = useMemo(() => {
    const m = new Map(subjects.map((s) => [String(s.id), s.name]));
    return (id: string) => (id === OTHER_SUBJECT ? 'Môn khác' : (m.get(id) ?? `#${id}`));
  }, [subjects]);
  const gradeName = useMemo(() => {
    const m = new Map(grades.map((g) => [String(g.id), g.name]));
    return (id: string) => m.get(id) ?? '';
  }, [grades]);

  const results = useMemo(() => rankClasses(classes, criteria), [classes, criteria]);

  // --- Cập nhật tiêu chí ---
  const patch = (p: Partial<TutorCriteria>) => setCriteria((c) => ({ ...c, ...p }));
  const toggleId = (key: 'subjectIds' | 'gradeIds', id: string) =>
    setCriteria((c) => {
      const set = new Set(c[key]);
      if (set.has(id)) set.delete(id);
      else set.add(id);
      return { ...c, [key]: [...set] };
    });
  const toggleAvail = (day: string, session: string) =>
    setCriteria((c) => {
      const exists = c.availability.some((a) => a.day === day && a.session === session);
      return {
        ...c,
        availability: exists
          ? c.availability.filter((a) => !(a.day === day && a.session === session))
          : [...c.availability, { day, session }],
      };
    });
  const setWeight = (key: keyof MatchWeights, value: number) =>
    setCriteria((c) => ({ ...c, weights: { ...c.weights, [key]: value } }));

  async function handleApply(classId: number) {
    setApplyingId(classId);
    setNotice(null);
    try {
      const rate = Number(criteria.expectedFee) || undefined;
      await marketplaceApi.applyToClass(classId, {
        proposedRate: rate,
        coverLetter: 'Tôi quan tâm và mong muốn nhận lớp này.',
      });
      setApplied((s) => new Set(s).add(classId));
      setNotice(`Đã gửi đơn ứng tuyển lớp #${classId}.`);
    } catch (err) {
      setNotice(extractError(err));
    } finally {
      setApplyingId(null);
    }
  }

  return (
    <div className="tfc">
      <aside className="tfc-panel">
        <h2 className="tfc-panel__title">Tiêu chí của bạn</h2>
        <p className="tfc-panel__desc">
          Khai báo môn dạy, khu vực, học phí mong muốn và lịch rảnh. Hệ thống chấm điểm độ phù hợp
          của từng lớp theo trọng số bạn đặt.
        </p>

        <div className="tfc-field">
          <span className="tfc-label">Môn bạn dạy</span>
          <div className="tfc-chips">
            {subjects.map((s) => {
              const on = criteria.subjectIds.includes(String(s.id));
              return (
                <button
                  key={s.id}
                  type="button"
                  className={`tfc-chip ${on ? 'is-on' : ''}`}
                  onClick={() => toggleId('subjectIds', String(s.id))}
                >
                  {s.name}
                </button>
              );
            })}
          </div>
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Khối lớp bạn nhận</span>
          <div className="tfc-chips">
            {grades.map((g) => {
              const on = criteria.gradeIds.includes(String(g.id));
              return (
                <button
                  key={g.id}
                  type="button"
                  className={`tfc-chip ${on ? 'is-on' : ''}`}
                  onClick={() => toggleId('gradeIds', String(g.id))}
                >
                  {g.name}
                </button>
              );
            })}
          </div>
          <small className="tfc-hint">Bỏ trống = nhận mọi khối lớp.</small>
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Khu vực</span>
          <label className="tfc-check tfc-check--top">
            <input
              type="checkbox"
              checked={criteria.onlineOnly}
              onChange={(e) => patch({ onlineOnly: e.target.checked })}
            />
            Chỉ dạy Online
          </label>
          {/* Dạy online thì không cần vị trí → khóa cả 2 dropdown. */}
          {/* Tỉnh/Thành → reset huyện khi đổi tỉnh. */}
          <select
            className="tfc-input"
            value={criteria.provinceId}
            disabled={criteria.onlineOnly}
            onChange={(e) => patch({ provinceId: e.target.value, districtId: '' })}
          >
            <option value="">— Chọn tỉnh/thành —</option>
            {provinces.map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
          </select>
          {/* Quận/Huyện — chỉ bật khi đã chọn tỉnh và không phải online. */}
          <select
            className="tfc-input tfc-input--stack"
            value={criteria.districtId}
            disabled={criteria.onlineOnly || !criteria.provinceId}
            onChange={(e) => patch({ districtId: e.target.value })}
          >
            <option value="">
              {criteria.provinceId ? '— Chọn quận/huyện (tùy chọn) —' : '— Chọn tỉnh trước —'}
            </option>
            {districts.map((d) => (
              <option key={d.id} value={String(d.id)}>
                {d.name}
              </option>
            ))}
          </select>
          {criteria.onlineOnly && (
            <small className="tfc-hint">Đang lọc lớp Online — không cần chọn khu vực.</small>
          )}
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Học phí/giờ mong muốn (đ)</span>
          <input
            type="number"
            min={0}
            step={10000}
            className="tfc-input"
            placeholder="VD: 150000"
            value={criteria.expectedFee}
            onChange={(e) => patch({ expectedFee: e.target.value })}
          />
          <small className="tfc-hint">Bỏ trống = không đặt mức tối thiểu.</small>
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Trình độ của bạn</span>
          <select
            className="tfc-input"
            value={criteria.level}
            onChange={(e) => patch({ level: e.target.value as TutorCriteria['level'] })}
          >
            {TUTOR_LEVEL_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Lịch rảnh</span>
          <div className="tfc-avail">
            <table>
              <thead>
                <tr>
                  <th />
                  {SESSION_OPTIONS.map((s) => (
                    <th key={s.value}>{s.value}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {DAY_OF_WEEK_OPTIONS.map((d) => (
                  <tr key={d.value}>
                    <th>{d.label}</th>
                    {SESSION_OPTIONS.map((s) => {
                      const on = criteria.availability.some(
                        (a) => a.day === d.value && a.session === s.value,
                      );
                      return (
                        <td key={s.value}>
                          <button
                            type="button"
                            className={`tfc-avail__cell ${on ? 'is-on' : ''}`}
                            aria-label={`${d.label} ${s.value}`}
                            onClick={() => toggleAvail(d.value, s.value)}
                          >
                            {on ? '✓' : ''}
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <small className="tfc-hint">Bỏ trống = lịch linh hoạt.</small>
        </div>

        <div className="tfc-field">
          <span className="tfc-label">Mức độ ưu tiên (trọng số 0–5)</span>
          {WEIGHT_LABELS.map((w) => (
            <div key={w.key} className="tfc-weight">
              <div className="tfc-weight__head">
                <span>{w.label}</span>
                <span className="tfc-weight__val">{criteria.weights[w.key]}</span>
              </div>
              <input
                type="range"
                min={0}
                max={5}
                step={1}
                value={criteria.weights[w.key]}
                onChange={(e) => setWeight(w.key, Number(e.target.value))}
              />
              <small className="tfc-hint">{w.hint}</small>
            </div>
          ))}
        </div>
      </aside>

      <section className="tfc-results">
        <header className="tfc-results__head">
          <h2>Lớp phù hợp với bạn</h2>
          <span className="tfc-results__count">
            {status === 'success' ? `${results.length} lớp đang mở` : ''}
          </span>
        </header>

        {notice && <div className="tfc-notice">{notice}</div>}
        {status === 'loading' && <div className="tfc-state">Đang tải danh sách lớp…</div>}
        {status === 'error' && (
          <div className="tfc-state tfc-state--error">Không tải được danh sách lớp.</div>
        )}
        {status === 'success' && results.length === 0 && (
          <div className="tfc-state">Chưa có lớp nào đang mở đơn ứng tuyển.</div>
        )}

        <div className="tfc-list">
          {results.map((r) => (
            <ClassCard
              key={r.parsed.raw.classId}
              result={r}
              subjectName={subjectName}
              gradeName={gradeName}
              applied={applied.has(r.parsed.raw.classId)}
              applying={applyingId === r.parsed.raw.classId}
              onApply={() => handleApply(r.parsed.raw.classId)}
              onDetail={() => setDetailTarget(r.parsed.raw)}
            />
          ))}
        </div>
      </section>

      {detailTarget && (
        <ClassDetailModal
          raw={detailTarget}
          subjects={subjects}
          grades={grades}
          applied={applied.has(detailTarget.classId)}
          applying={applyingId === detailTarget.classId}
          onApply={() => handleApply(detailTarget.classId)}
          onClose={() => setDetailTarget(null)}
        />
      )}
    </div>
  );
}

interface CardProps {
  readonly result: MatchResult;
  readonly subjectName: (id: string) => string;
  readonly gradeName: (id: string) => string;
  readonly applied: boolean;
  readonly applying: boolean;
  readonly onApply: () => void;
  readonly onDetail: () => void;
}

function ClassCard({
  result,
  subjectName,
  gradeName,
  applied,
  applying,
  onApply,
  onDetail,
}: CardProps) {
  const { parsed, breakdown } = result;
  const c = parsed.raw;
  const pct = Math.round(breakdown.score);
  const tone = pct >= 75 ? 'high' : pct >= 45 ? 'mid' : 'low';
  const subjectLabel =
    parsed.subjectIds.map(subjectName).concat(parsed.hasOtherSubject ? ['Môn khác'] : []).join(', ') ||
    (c.subjectName ?? '—');
  const gradeLabel = gradeName(parsed.gradeId) || c.gradeName || '—';
  const location =
    parsed.lessonMode === 'ONLINE'
      ? 'Online'
      : parsed.provinceName || c.address || c.locationName || 'Offline';

  return (
    <article className="tfc-card">
      <div className={`tfc-score tfc-score--${tone}`}>
        <span className="tfc-score__num">{pct}</span>
        <span className="tfc-score__unit">điểm</span>
      </div>
      <div className="tfc-card__body">
        <div className="tfc-card__top">
          <h3 className="tfc-card__title">{c.title}</h3>
          <span className="tfc-card__id">#{c.classId}</span>
        </div>
        <div className="tfc-card__meta">
          <span>📚 {subjectLabel}</span>
          <span>🎓 {gradeLabel}</span>
          <span>📍 {location}</span>
          <span>💰 {parsed.feePerHour > 0 ? `${currency.format(parsed.feePerHour)}đ/giờ` : '—'}</span>
        </div>

        <div className="tfc-break">
          {CRITERIA_LABELS.map((cl) => {
            const v = breakdown[cl.key] as number;
            return (
              <div key={cl.key} className="tfc-break__item" title={`${cl.label}: ${Math.round(v * 100)}%`}>
                <span className="tfc-break__label">{cl.label}</span>
                <span className="tfc-break__bar">
                  <span className="tfc-break__fill" style={{ width: `${Math.round(v * 100)}%` }} />
                </span>
                <span className="tfc-break__pct">{Math.round(v * 100)}%</span>
              </div>
            );
          })}
        </div>

        <div className="tfc-card__actions">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onDetail}>
            Xem chi tiết
          </button>
          <button
            type="button"
            className="tfc-btn tfc-btn--primary"
            disabled={applied || applying}
            onClick={onApply}
          >
            {applied ? '✓ Đã ứng tuyển' : applying ? 'Đang gửi…' : 'Ứng tuyển nhận lớp'}
          </button>
        </div>
      </div>
    </article>
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
