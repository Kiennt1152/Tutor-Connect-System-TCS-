import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import { ApplyClassModal } from './ApplyClassModal';
import { FALLBACK_SUBJECTS, FALLBACK_GRADES } from '../constants/catalogFallback';
import {
  OTHER_SUBJECT,
  type CatalogOption,
  type ClassResponse,
} from '../types/marketplaceTypes';
import {
  emptyCriteria,
  rankClasses,
  type MatchResult,
  type MatchWeights,
  type TutorCriteria,
} from '../matching/tutorMatching';
import { FormulaExplainer } from './FormulaExplainer';
import './tutorFindClass.css';

const currency = new Intl.NumberFormat('vi-VN');

const WEIGHT_LABELS: { key: keyof MatchWeights; label: string; hint: string }[] = [
  { key: 'subject', label: 'Môn & lớp (S)', hint: 'Đúng môn, đúng khối lớp bạn dạy' },
  { key: 'location', label: 'Địa điểm (L)', hint: 'Gần bạn / học online' },
  { key: 'salary', label: 'Học phí (P)', hint: 'Đạt mức bạn mong muốn' },
  { key: 'schedule', label: 'Lịch học (T)', hint: 'Trùng khung giờ bạn rảnh' },
  { key: 'experience', label: 'Trình độ (E)', hint: 'Phù hợp yêu cầu bằng cấp' },
];

// Nhãn chữ cho từng mức trọng số 0–5 để người dùng dễ hiểu.
const WEIGHT_SCALE = ['Bỏ qua', 'Rất thấp', 'Thấp', 'Vừa', 'Cao', 'Rất cao'];
const weightLabel = (v: number) => WEIGHT_SCALE[v] ?? '';

// Bỏ dấu tiếng Việt + lowercase để so khớp tên môn không phân biệt dấu/hoa-thường.
const normalize = (s: string) =>
  s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/đ/g, 'd').trim();

interface QueryFilters {
  subjectIds: string[];
  gradeIds: string[];
  provinceId: string;
}

// "Đọc hiểu" câu tìm kiếm tiếng Việt tự do → tách MÔN HỌC + KHỐI LỚP + TỈNH/THÀNH.
// Parser theo luật (không phải LLM), đủ cho các câu như:
//   "tìm gia sư tiếng anh lớp 12 ở hà nội" · "cần lớp toán lý hóa khối 9 tại đà nẵng"
function parseSmartQuery(
  text: string,
  subjects: readonly CatalogOption[],
  grades: readonly CatalogOption[],
  provinces: readonly CatalogOption[],
): QueryFilters {
  const q = ` ${normalize(text)} `;
  const isCert = /ielts|toeic|chung chi/.test(q);

  // --- Môn học: tên môn xuất hiện trong câu (khớp theo ranh giới từ) ---
  const subjectIds = new Set<string>();
  for (const s of subjects) {
    const name = normalize(s.name);
    if (name && q.includes(` ${name} `)) subjectIds.add(String(s.id));
  }
  if (isCert) {
    const cert = subjects.find((s) => /chung chi/.test(normalize(s.name)));
    if (cert) subjectIds.add(String(cert.id));
  }

  // --- Khối lớp: "lớp N" / "khối N" / "đại học" / chứng chỉ ---
  const gradeIds = new Set<string>();
  for (const m of q.matchAll(/(?:lop|khoi)\s*(\d{1,2})/g)) {
    const g = grades.find((x) => normalize(x.name) === `lop ${Number(m[1])}`);
    if (g) gradeIds.add(String(g.id));
  }
  if (/dai hoc/.test(q)) {
    const g = grades.find((x) => /dai hoc/.test(normalize(x.name)));
    if (g) gradeIds.add(String(g.id));
  }
  if (isCert) {
    const g = grades.find((x) => /chung chi/.test(normalize(x.name)));
    if (g) gradeIds.add(String(g.id));
  }

  // --- Tỉnh/thành: bỏ tiền tố "tp/thành phố/tỉnh", ưu tiên tên dài nhất khớp ---
  let provinceId = '';
  const cands = provinces
    .map((p) => ({ id: String(p.id), core: normalize(p.name).replace(/^(tp|thanh pho|tinh)\s+/, '') }))
    .filter((p) => p.core.length >= 3)
    .sort((a, b) => b.core.length - a.core.length);
  for (const p of cands) {
    if (q.includes(` ${p.core} `)) {
      provinceId = p.id;
      break;
    }
  }

  return { subjectIds: [...subjectIds], gradeIds: [...gradeIds], provinceId };
}

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
  const [notice, setNotice] = useState<string | null>(null);
  const [detailTarget, setDetailTarget] = useState<ClassResponse | null>(null);
  const [applyTarget, setApplyTarget] = useState<ClassResponse | null>(null);
  // Thanh tìm: query = câu đang gõ. Bộ lọc tách từ câu (hoặc từ chip):
  // selectedIds = môn (lọc lớp) · gradeIds/provinceId = khối lớp + tỉnh (ảnh hưởng % xếp hạng).
  const [query, setQuery] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [gradeIds, setGradeIds] = useState<string[]>([]);
  const [provinceId, setProvinceId] = useState('');

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

  // Tiêu chí lấy TỰ ĐỘNG từ hồ sơ gia sư (không còn form khai báo). Hiện hồ sơ chỉ
  // expose học phí/giờ → dùng làm mức mong muốn (P). Các tiêu chí khác (môn, khu vực,
  // lịch) chưa có trong hồ sơ nên để linh hoạt; gia sư điều chỉnh bằng trọng số ưu tiên.
  useEffect(() => {
    let alive = true;
    marketplaceApi
      .getMyTutorProfile()
      .then((p) => {
        if (!alive || !p.hourlyRate) return;
        setCriteria((c) => ({ ...c, expectedFee: String(Math.round(Number(p.hourlyRate))) }));
      })
      .catch(() => {
        /* Không có hồ sơ / lỗi tải → bỏ qua, vẫn xếp hạng theo trọng số. */
      });
    return () => {
      alive = false;
    };
  }, []);

  // Nếu backend không trả được catalog (lỗi/404) thì dùng danh sách dự phòng
  // (ID khớp seed DB nên hiển thị tên môn/lớp vẫn đúng trên thẻ kết quả).
  const effSubjects = useMemo(
    () => (subjects.length > 0 ? subjects : [...FALLBACK_SUBJECTS]),
    [subjects],
  );
  const effGrades = useMemo(
    () => (grades.length > 0 ? grades : [...FALLBACK_GRADES]),
    [grades],
  );

  const subjectName = useMemo(() => {
    const m = new Map(effSubjects.map((s) => [String(s.id), s.name]));
    return (id: string) => (id === OTHER_SUBJECT ? 'Môn khác' : (m.get(id) ?? `#${id}`));
  }, [effSubjects]);
  const gradeName = useMemo(() => {
    const m = new Map(effGrades.map((g) => [String(g.id), g.name]));
    return (id: string) => m.get(id) ?? '';
  }, [effGrades]);
  const provinceName = useMemo(() => {
    const m = new Map(provinces.map((p) => [String(p.id), p.name]));
    return (id: string) => m.get(id) ?? '';
  }, [provinces]);

  // Bộ lọc từ câu tìm kiếm → đưa vào tiêu chí: môn (S), khối lớp (S), tỉnh (L).
  const activeCriteria = useMemo(
    () => ({ ...criteria, subjectIds: selectedIds, gradeIds, provinceId }),
    [criteria, selectedIds, gradeIds, provinceId],
  );

  // Xếp hạng lớp theo % của công thức trọng số (rankClasses đã sắp % giảm dần) — cập
  // nhật TRỰC TIẾP khi kéo trọng số. Đã chọn môn → CHỈ giữ lớp có ÍT NHẤT một môn đã
  // chọn (loại lớp chỉ "Khác"); chưa chọn → hiện mọi lớp mở.
  const results = useMemo(() => {
    const ranked = rankClasses(classes, activeCriteria);
    if (selectedIds.length === 0) return ranked;
    const wanted = new Set(selectedIds);
    return ranked.filter((r) => r.parsed.subjectIds.some((id) => wanted.has(id)));
  }, [classes, activeCriteria, selectedIds]);

  const selectedNames = selectedIds.map((id) => subjectName(id)).join(', ');

  // Các thẻ "hệ thống đã hiểu" từ câu tìm kiếm (môn · khối lớp · tỉnh) để phản hồi cho gia sư.
  const understoodTags = [
    ...selectedIds.map((id) => ({ kind: 'Môn', label: subjectName(id) })),
    ...gradeIds.map((id) => ({ kind: 'Khối', label: gradeName(id) })),
    ...(provinceId ? [{ kind: 'Tỉnh', label: provinceName(provinceId) }] : []),
  ].filter((t) => t.label);
  const hasFilter = selectedIds.length > 0 || gradeIds.length > 0 || provinceId !== '';

  const setWeight = (key: keyof MatchWeights, value: number) =>
    setCriteria((c) => ({ ...c, weights: { ...c.weights, [key]: value } }));

  // Bấm "Tìm lớp"/Enter → "đọc hiểu" cả câu, tách môn + khối lớp + tỉnh rồi áp bộ lọc.
  function handleSearch(e: FormEvent) {
    e.preventDefault();
    if (!query.trim()) {
      setSelectedIds([]);
      setGradeIds([]);
      setProvinceId('');
      return;
    }
    const f = parseSmartQuery(query, effSubjects, effGrades, provinces);
    setSelectedIds(f.subjectIds);
    setGradeIds(f.gradeIds);
    setProvinceId(f.provinceId);
  }
  // Bấm chip = đường tắt chỉ chọn môn: bật/tắt môn, ghi tên vào ô search, bỏ lọc khối/tỉnh.
  function toggleSubject(id: string) {
    const next = selectedIds.includes(id)
      ? selectedIds.filter((x) => x !== id)
      : [...selectedIds, id];
    setSelectedIds(next);
    setGradeIds([]);
    setProvinceId('');
    setQuery(next.map((sid) => subjectName(sid)).join(', '));
  }
  function clearSearch() {
    setQuery('');
    setSelectedIds([]);
    setGradeIds([]);
    setProvinceId('');
  }

  // Mở form ứng tuyển (hiển thị hồ sơ gia sư trước khi gửi).
  function openApply(target: ClassResponse) {
    setNotice(null);
    setApplyTarget(target);
    setDetailTarget(null);
  }

  // Sau khi form gửi đơn thành công.
  function handleApplied(classId: number) {
    setApplied((s) => new Set(s).add(classId));
    setNotice('Đã gửi đơn ứng tuyển thành công.');
    setApplyTarget(null);
  }

  return (
    <div className="tfc">
      <form className="tfc-search" onSubmit={handleSearch}>
        <div className="tfc-search__row">
          <div className="tfc-search__box">
            <span className="tfc-search__icon" aria-hidden>🔍</span>
            <input
              className="tfc-search__input"
              type="text"
              placeholder="Hỏi tự nhiên, VD: tìm lớp Tiếng Anh lớp 12 ở Hà Nội"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              list="tfc-subject-list"
              aria-label="Tìm lớp bằng câu hỏi tự nhiên"
            />
            <datalist id="tfc-subject-list">
              {effSubjects.map((s) => (
                <option key={s.id} value={s.name} />
              ))}
            </datalist>
            {hasFilter && (
              <button
                type="button"
                className="tfc-search__clear"
                onClick={clearSearch}
                aria-label="Xóa bộ lọc"
              >
                ✕
              </button>
            )}
          </div>
          <button type="submit" className="tfc-btn tfc-btn--primary tfc-search__btn">
            Tìm lớp
          </button>
        </div>

        {understoodTags.length > 0 && (
          <div className="tfc-search__understood">
            <span className="tfc-search__chips-label">Đã hiểu:</span>
            {understoodTags.map((t, i) => (
              <span key={`${t.kind}-${i}`} className="tfc-search__tag">
                {t.kind}: {t.label}
              </span>
            ))}
          </div>
        )}
        <div className="tfc-search__chips">
          <span className="tfc-search__chips-label">Chọn môn bạn dạy:</span>
          {effSubjects.map((s) => {
            const on = selectedIds.includes(String(s.id));
            return (
              <button
                key={s.id}
                type="button"
                className={`tfc-chip ${on ? 'is-on' : ''}`}
                aria-pressed={on}
                onClick={() => toggleSubject(String(s.id))}
              >
                {on ? '✓ ' : ''}
                {s.name}
              </button>
            );
          })}
        </div>
      </form>

      <div className="tfc-panel">
        <h2 className="tfc-panel__title">Mức độ ưu tiên khi tìm lớp</h2>
        <p className="tfc-panel__desc">
          Kéo để chọn tiêu chí <strong>quan trọng hơn</strong> khi xếp hạng lớp ·{' '}
          <span className="tfc-weight-legend">0 = bỏ qua · 5 = ưu tiên cao nhất</span>
        </p>

        <div className="tfc-form-grid">
        <div className="tfc-field tfc-field--wide tfc-priority">
          <div className="tfc-priority__left">
          <div className="tfc-weight-list">
          {WEIGHT_LABELS.map((w) => {
            const v = criteria.weights[w.key];
            return (
              <div key={w.key} className="tfc-weight" title={w.hint}>
                <div className="tfc-weight__head">
                  <span>{w.label}</span>
                  <span className={`tfc-weight__val ${v === 0 ? 'is-zero' : ''}`}>
                    {v} · {weightLabel(v)}
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={5}
                  step={1}
                  value={v}
                  onChange={(e) => setWeight(w.key, Number(e.target.value))}
                />
              </div>
            );
          })}
          </div>
          </div>
          <div className="tfc-priority__right">
            <FormulaExplainer weights={criteria.weights} defaultOpen={false} />
          </div>
        </div>
        </div>
      </div>

      <section className="tfc-results" id="tfc-results">
        <header className="tfc-results__head">
          <h2>Lớp phù hợp với bạn</h2>
          <span className="tfc-results__count">
            {status === 'success'
              ? selectedIds.length > 0
                ? `${results.length} lớp cần: ${selectedNames}`
                : `${results.length} lớp đang mở`
              : ''}
          </span>
        </header>

        {notice && <div className="tfc-notice">{notice}</div>}

        {status === 'loading' && <div className="tfc-state">Đang tải danh sách lớp…</div>}
        {status === 'error' && (
          <div className="tfc-state tfc-state--error">Không tải được danh sách lớp.</div>
        )}
        {status === 'success' && results.length === 0 && (
          <div className="tfc-state">
            {selectedIds.length > 0
              ? `Chưa có lớp nào đang cần: ${selectedNames}. Thử môn khác nhé.`
              : 'Chưa có lớp nào đang mở đơn ứng tuyển.'}
          </div>
        )}

        <div className="tfc-list">
          {results.map((r) => (
            <ClassCard
              key={r.parsed.raw.classId}
              result={r}
              subjectName={subjectName}
              gradeName={gradeName}
              applied={applied.has(r.parsed.raw.classId)}
              onApply={() => openApply(r.parsed.raw)}
              onDetail={() => setDetailTarget(r.parsed.raw)}
            />
          ))}
        </div>
      </section>

      {detailTarget && (
        <ClassDetailModal
          raw={detailTarget}
          subjects={effSubjects}
          grades={effGrades}
          applied={applied.has(detailTarget.classId)}
          onApply={() => openApply(detailTarget)}
          onClose={() => setDetailTarget(null)}
        />
      )}

      {applyTarget && (
        <ApplyClassModal
          target={applyTarget}
          defaultRate={Number(criteria.expectedFee) || undefined}
          onClose={() => setApplyTarget(null)}
          onSubmitted={handleApplied}
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
  readonly onApply: () => void;
  readonly onDetail: () => void;
}

function ClassCard({
  result,
  subjectName,
  gradeName,
  applied,
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
  // Thông tin chung của lớp (thay cho bảng chấm điểm từng tiêu chí).
  const learningGoal = c.learningGoal?.trim() ?? '';
  const tutorRequirement = parsed.tutorRequirement?.trim() ?? '';
  const sessionCount = parsed.slots.length;
  const scheduleSummary =
    sessionCount > 0
      ? parsed.scheduleMode === 'WEEKLY'
        ? `${sessionCount} buổi/tuần`
        : `${sessionCount} buổi`
      : '';

  return (
    <article className="tfc-card">
      <div className={`tfc-score tfc-score--${tone}`}>
        <span className="tfc-score__num">{pct}%</span>
        <span className="tfc-score__unit">phù hợp</span>
      </div>
      <div className="tfc-card__body">
        <div className="tfc-card__top">
          <h3 className="tfc-card__title">{c.title}</h3>
        </div>
        <div className="tfc-card__meta">
          <span>📚 {subjectLabel}</span>
          <span>🎓 {gradeLabel}</span>
          <span>📍 {location}</span>
          <span>💰 {parsed.feePerHour > 0 ? `${currency.format(parsed.feePerHour)}đ/giờ` : '—'}</span>
        </div>

        <div className="tfc-card__info">
          {learningGoal && (
            <p className="tfc-card__info-row">
              🎯 <strong>Mục tiêu:</strong> {learningGoal}
            </p>
          )}
          {tutorRequirement && (
            <p className="tfc-card__info-row">
              🧑‍🏫 <strong>Yêu cầu gia sư:</strong> {tutorRequirement}
            </p>
          )}
          {scheduleSummary && (
            <p className="tfc-card__info-row">
              🗓️ <strong>Lịch học:</strong> {scheduleSummary}
            </p>
          )}
        </div>

        <div className="tfc-card__actions">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onDetail}>
            Xem chi tiết
          </button>
          <button
            type="button"
            className="tfc-btn tfc-btn--primary"
            disabled={applied}
            onClick={onApply}
          >
            {applied ? '✓ Đã ứng tuyển' : 'Ứng tuyển nhận lớp'}
          </button>
        </div>
      </div>
    </article>
  );
}
