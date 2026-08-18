import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import { ApplyClassModal } from './ApplyClassModal';
import { ExpiryBadge } from './ExpiryBadge';
import { FormulaExplainer } from './FormulaExplainer';
import { FALLBACK_SUBJECTS, FALLBACK_GRADES } from '../constants/catalogFallback';
import {
  isOtherSubject,
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
import './tutorFindClass.css';

const WEIGHT_LABELS: { key: keyof MatchWeights; label: string; hint: string }[] = [
  { key: 'subject', label: 'Môn & lớp (S)', hint: 'Đúng môn, đúng khối lớp bạn dạy' },
  { key: 'location', label: 'Địa điểm (L)', hint: 'Gần bạn / học online' },
  { key: 'salary', label: 'Học phí (P)', hint: 'Đạt mức bạn mong muốn' },
  { key: 'schedule', label: 'Lịch học (T)', hint: 'Trùng khung giờ bạn rảnh' },
  { key: 'experience', label: 'Trình độ (E)', hint: 'Phù hợp yêu cầu bằng cấp' },
];

const WEIGHT_SCALE = ['Bỏ qua', 'Rất thấp', 'Thấp', 'Vừa', 'Cao', 'Rất cao'];
const PAGE_SIZE = 6;
const weightLabel = (v: number) => WEIGHT_SCALE[v] ?? '';

const normalize = (s: string) =>
  s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/đ/g, 'd').trim();

const SUBJECT_ALIASES: Record<string, string[]> = {
  toan: ['toan'],
  'vat ly': ['vat ly', 'vat', 'ly'],
  'hoa hoc': ['hoa hoc', 'hoa'],
  'sinh hoc': ['sinh hoc', 'sinh'],
  'ngu van': ['ngu van', 'van'],
  'tieng viet': ['tieng viet'],
  'tieng anh': ['tieng anh', 'anh', 'english'],
  'lich su': ['lich su', 'su'],
  'dia ly': ['dia ly', 'dia'],
  'tin hoc': ['tin hoc', 'tin'],
};

const GOAL_KEYWORDS: { kw: string; label: string }[] = [
  { kw: 'lay lai goc', label: 'Lấy lại gốc' },
  { kw: 'on thi hoc ky', label: 'Ôn thi học kỳ' },
  { kw: 'chuyen cap', label: 'Luyện thi chuyển cấp' },
  { kw: 'vao 10', label: 'Luyện thi chuyển cấp' },
  { kw: 'luyen thi dai hoc', label: 'Luyện thi Đại học' },
  { kw: 'luyen thi chung chi', label: 'Luyện thi chứng chỉ' },
];
const REQ_KEYWORDS: { kw: string; label: string }[] = [
  { kw: 'giao vien', label: 'Giáo viên' },
  { kw: 'sinh vien', label: 'Sinh viên' },
  { kw: 'bang cap', label: 'Có bằng cấp/chứng chỉ' },
];

interface QueryFilters {
  subjectIds: string[];
  gradeIds: string[];
  provinceId: string;
  expectedFee: string;
  lessonMode: '' | 'ONLINE' | 'OFFLINE';
  goalKeywords: string[];
  reqKeywords: string[];
  otherSubjectText: string;
}

function parseSmartQuery(
  text: string,
  subjects: readonly CatalogOption[],
  grades: readonly CatalogOption[],
  provinces: readonly CatalogOption[],
): QueryFilters {
  const q = ` ${normalize(text).replace(/[,;/|]+/g, ' ')} `;
  const isCert = /ielts|toeic|chung chi/.test(q);

  const subjectIds = new Set<string>();
  const aliasPairs: { id: string; kw: string }[] = [];
  for (const s of subjects) {
    const name = normalize(s.name);
    for (const kw of SUBJECT_ALIASES[name] ?? [name]) {
      if (kw) aliasPairs.push({ id: String(s.id), kw });
    }
  }
  aliasPairs.sort((a, b) => b.kw.length - a.kw.length);
  let scan = q;
  // Loại cụm "yêu cầu GS" / "mục tiêu" (vd "sinh viên", "giáo viên") trước khi dò môn,
  // tránh "sinh viên" bị nhận nhầm thành môn "Sinh học" (alias 'sinh').
  for (const { kw } of [...REQ_KEYWORDS, ...GOAL_KEYWORDS]) {
    scan = scan.replace(new RegExp(`\\b${kw}\\b`, 'g'), '  ');
  }
  for (const { id, kw } of aliasPairs) {
    if (scan.includes(` ${kw} `)) {
      subjectIds.add(id);
      scan = scan.split(` ${kw} `).join('  ');
    }
  }
  if (isCert) {
    const cert = subjects.find((s) => /chung chi/.test(normalize(s.name)));
    if (cert) subjectIds.add(String(cert.id));
  }

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

  let expectedFee = '';
  const kMatch = /(\d{2,4})\s*k(?![a-z])/.exec(q);
  if (kMatch) {
    expectedFee = String(Number(kMatch[1]) * 1000);
  } else {
    const rawMatch = /(\d[\d.,]{4,})\s*(?:d|dong|vnd)?/.exec(q);
    const n = rawMatch ? Number(rawMatch[1].replace(/[.,]/g, '')) : 0;
    if (n >= 10000) expectedFee = String(n);
  }

  const lessonMode: QueryFilters['lessonMode'] = /offline|truc tiep|tai nha|tai nguoi hoc/.test(q)
    ? 'OFFLINE'
    : /\bonline\b|truc tuyen/.test(q)
      ? 'ONLINE'
      : '';

  const goalKeywords = GOAL_KEYWORDS.filter((g) => q.includes(g.kw)).map((g) => g.kw);
  const reqKeywords = REQ_KEYWORDS.filter((g) => q.includes(g.kw)).map((g) => g.kw);

  let otherSubjectText = '';
  if (subjectIds.size === 0) {
    let s = q;
    const pc = cands.find((c) => c.id === provinceId)?.core;
    if (pc) s = s.split(pc).join(' ');
    for (const kw of [...goalKeywords, ...reqKeywords]) s = s.split(kw).join(' ');
    s = s
      .replace(/(?:lop|khoi)\s*\d{1,2}/g, ' ')
      .replace(/\d{2,4}\s*k(?![a-z])/g, ' ')
      .replace(/\d[\d.,]{4,}/g, ' ')
      .replace(/\b(online|offline|truc tiep|truc tuyen|tai nha|tai nguoi hoc|dai hoc|vao 10)\b/g, ' ')
      // Bỏ tiền tố hành chính/khu vực để tên tỉnh không bị hiểu nhầm thành "môn khác"
      .replace(/\b(tinh|thanh pho|tp|quan|huyen|phuong|xa|thi xa|khu vuc|khu vc|khu)\b/g, ' ')
      .replace(/\b(mon|tim|gia su|giasu|day|hoc phi|hoc|gio|vnd|dong|luyen thi|thi|o|tai|can|lop|khoi)\b/g, ' ')
      // Từ nối / nhãn tiêu chí (không phải tên môn): "yêu cầu là sinh viên" -> không tạo "môn khác"
      .replace(/\b(yeu cau|muc tieu|yeu|cau|la|vien|gia su)\b/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    if (s.length >= 2) otherSubjectText = s;
  }

  return {
    subjectIds: [...subjectIds],
    gradeIds: [...gradeIds],
    provinceId,
    expectedFee,
    lessonMode,
    goalKeywords,
    reqKeywords,
    otherSubjectText,
  };
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
  const [query, setQuery] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [gradeIds, setGradeIds] = useState<string[]>([]);
  const [provinceId, setProvinceId] = useState('');
  const [queryFee, setQueryFee] = useState('');
  const [queryMode, setQueryMode] = useState<QueryFilters['lessonMode']>('');
  const [goalKeys, setGoalKeys] = useState<string[]>([]);
  const [reqKeys, setReqKeys] = useState<string[]>([]);
  const [otherText, setOtherText] = useState('');
  const [searched, setSearched] = useState(false);
  const [showFormula, setShowFormula] = useState(false);
  const [page, setPage] = useState(1);

  const loadClasses = useCallback((silent = false) => {
    if (!silent) setStatus('loading');
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch(() => {
        if (!silent) setStatus('error');
      });
  }, []);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    const onFocus = () => loadClasses(true);
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [loadClasses]);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .listMyAppliedClassIds()
      .then((ids) => alive && setApplied(new Set(ids)))
      .catch(() => {
      });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .getMyTutorProfile()
      .then((p) => {
        if (!alive || !p.hourlyRate) return;
        setCriteria((c) => ({ ...c, expectedFee: String(Math.round(Number(p.hourlyRate))) }));
      })
      .catch(() => {
      });
    return () => {
      alive = false;
    };
  }, []);

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
    return (id: string) => (isOtherSubject(id) ? 'Môn khác' : (m.get(id) ?? `#${id}`));
  }, [effSubjects]);
  const gradeName = useMemo(() => {
    const m = new Map(effGrades.map((g) => [String(g.id), g.name]));
    return (id: string) => m.get(id) ?? '';
  }, [effGrades]);
  const provinceName = useMemo(() => {
    const m = new Map(provinces.map((p) => [String(p.id), p.name]));
    return (id: string) => m.get(id) ?? '';
  }, [provinces]);

  const activeCriteria = useMemo(
    () => ({
      ...criteria,
      subjectIds: selectedIds,
      gradeIds,
      provinceId,
      expectedFee: queryFee || criteria.expectedFee,
      onlineOnly: queryMode === 'ONLINE',
    }),
    [criteria, selectedIds, gradeIds, provinceId, queryFee, queryMode],
  );

  const results = useMemo(() => {
    let out = rankClasses(classes, activeCriteria);
    if (selectedIds.length > 0) {
      const wanted = new Set(selectedIds);
      out = out.filter((r) => r.parsed.subjectIds.some((id) => wanted.has(id)));
    } else if (otherText) {
      out = out.filter((r) => r.parsed.hasOtherSubject);
    }
    if (gradeIds.length > 0) {
      const wanted = new Set(gradeIds);
      out = out.filter((r) => wanted.has(r.parsed.gradeId));
    }
    if (queryMode === 'ONLINE') out = out.filter((r) => r.parsed.lessonMode === 'ONLINE');
    if (queryMode === 'OFFLINE') out = out.filter((r) => r.parsed.lessonMode !== 'ONLINE');
    if (provinceId) {
      // Lớp thường chỉ lưu TÊN tỉnh (provinceId rỗng) -> so khớp theo tên đã chuẩn hóa, id chỉ là dự phòng.
      const stripPrefix = (s: string) => normalize(s).replace(/^(tp|thanh pho|tinh)\s+/, '');
      const want = stripPrefix(provinceName(provinceId));
      out = out.filter((r) => {
        if (r.parsed.lessonMode === 'ONLINE') return true;
        if (r.parsed.provinceId && r.parsed.provinceId === provinceId) return true;
        const got = stripPrefix(r.parsed.provinceName ?? '');
        return !!got && got === want;
      });
    }
    if (goalKeys.length > 0) {
      out = out.filter((r) => {
        const g = normalize(r.parsed.learningGoal ?? '');
        return goalKeys.some((k) => g.includes(k));
      });
    }
    if (reqKeys.length > 0) {
      out = out.filter((r) => {
        const req = normalize(r.parsed.tutorRequirement ?? '');
        return reqKeys.some((k) => req.includes(k));
      });
    }
    return out;
  }, [classes, activeCriteria, selectedIds, gradeIds, provinceId, queryMode, goalKeys, reqKeys, otherText, provinceName]);

  // Phân trang: 6 lớp / trang (2 cột × 3 hàng); lớp thứ 7 nhảy sang trang 2.
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const pageResults = results.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
  // Kết quả đổi (tìm mới / lọc khác) -> quay về trang 1.
  useEffect(() => {
    setPage(1);
  }, [selectedIds, gradeIds, provinceId, queryMode, queryFee, goalKeys, reqKeys, otherText]);

  useEffect(() => {
    if (!query.trim()) {
      setSelectedIds([]);
      setGradeIds([]);
      setProvinceId('');
      setQueryFee('');
      setQueryMode('');
      setGoalKeys([]);
      setReqKeys([]);
      setOtherText('');
      return;
    }
    const f = parseSmartQuery(query, effSubjects, effGrades, provinces);
    setSelectedIds(f.subjectIds);
    setGradeIds(f.gradeIds);
    setProvinceId(f.provinceId);
    setQueryFee(f.expectedFee);
    setQueryMode(f.lessonMode);
    setGoalKeys(f.goalKeywords);
    setReqKeys(f.reqKeywords);
    setOtherText(f.otherSubjectText);
    setSearched(true);
  }, [query, effSubjects, effGrades, provinces]);

  const selectedNames = selectedIds.map((id) => subjectName(id)).join(', ');

  const understoodTags = [
    ...selectedIds.map((id) => ({ kind: 'Môn', label: subjectName(id) })),
    ...gradeIds.map((id) => ({ kind: 'Khối', label: gradeName(id) })),
    ...(provinceId ? [{ kind: 'Tỉnh', label: provinceName(provinceId) }] : []),
    ...(queryMode
      ? [{ kind: 'Hình thức', label: queryMode === 'ONLINE' ? 'Online' : 'Trực tiếp (offline)' }]
      : []),
    ...(queryFee
      ? [{ kind: 'Học phí', label: `${Number(queryFee).toLocaleString('vi-VN')}đ/giờ` }]
      : []),
    ...[...new Set(goalKeys.map((k) => GOAL_KEYWORDS.find((g) => g.kw === k)?.label))].map(
      (label) => ({ kind: 'Mục tiêu', label: label ?? '' }),
    ),
    ...[...new Set(reqKeys.map((k) => REQ_KEYWORDS.find((g) => g.kw === k)?.label))].map(
      (label) => ({ kind: 'Yêu cầu GS', label: label ?? '' }),
    ),
    ...(otherText ? [{ kind: 'Môn', label: 'Môn khác' }] : []),
  ].filter((t) => t.label);
  const hasFilter =
    selectedIds.length > 0 ||
    gradeIds.length > 0 ||
    provinceId !== '' ||
    queryFee !== '' ||
    queryMode !== '' ||
    goalKeys.length > 0 ||
    reqKeys.length > 0 ||
    otherText !== '';

  const setWeight = (key: keyof MatchWeights, value: number) =>
    setCriteria((c) => ({ ...c, weights: { ...c.weights, [key]: value } }));

  function handleSearch(e: FormEvent) {
    e.preventDefault();
    setSearched(true);
    loadClasses(true);
  }

  function clearSearch() {
    setQuery('');
    setSelectedIds([]);
    setGradeIds([]);
    setProvinceId('');
    setQueryFee('');
    setQueryMode('');
    setGoalKeys([]);
    setReqKeys([]);
    setOtherText('');
  }

  function openApply(target: ClassResponse) {
    setNotice(null);
    setApplyTarget(target);
    setDetailTarget(null);
  }

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
              placeholder="Hãy điền tên (môn học, lớp, địa điểm, mục tiêu học hoặc yêu cầu là gia sư nào)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="Tìm lớp bằng câu hỏi tự nhiên"
            />
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
            Tìm
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

        <aside className={`tfc-panel${searched ? '' : ' is-locked'}`}>
        <div className="tfc-panel__head">
          <h2 className="tfc-panel__title">Mức độ ưu tiên khi tìm</h2>
          <button
            type="button"
            className="tfc-formula-btn"
            onClick={() => setShowFormula(true)}
            title="Xem công thức chấm độ phù hợp"
          >
            Cách tính ?
          </button>
        </div>
        <p className="tfc-panel__desc">
          {searched ? (
            <>
              Kéo để chọn tiêu chí <strong>quan trọng hơn</strong> khi xếp hạng lớp ·{' '}
              <span className="tfc-weight-legend">0 = bỏ qua · 5 = ưu tiên cao nhất</span>
            </>
          ) : (
            <>
              Bấm <strong>Tìm</strong> trước, rồi mới chỉnh được mức ưu tiên.
            </>
          )}
        </p>

        <div className="tfc-weight-list">
          {WEIGHT_LABELS.map((w) => {
            const v = criteria.weights[w.key];
            return (
              <div key={w.key} className="tfc-weight" title={searched ? w.hint : undefined}>
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
                  disabled={!searched}
                  onChange={(e) => setWeight(w.key, Number(e.target.value))}
                />
              </div>
            );
          })}
        </div>
        </aside>
      </form>

      <section className="tfc-results" id="tfc-results">
        <header className="tfc-results__head">
          <h2>{searched ? 'Yêu cầu phù hợp với bạn' : 'Tất cả tin tìm gia sư đã đăng'}</h2>
          <span className="tfc-results__count">
            {status === 'success'
              ? searched && selectedIds.length > 0
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
            {searched && selectedIds.length > 0
              ? `Chưa có lớp nào đang cần: ${selectedNames}. Thử môn khác nhé.`
              : 'Chưa có lớp nào đang mở đơn ứng tuyển.'}
          </div>
        )}

        <div className="tfc-list">
          {pageResults.map((r) => (
            <ClassCard
              key={r.parsed.raw.classId}
              result={r}
              subjectName={subjectName}
              gradeName={gradeName}
              showScore={searched}
              applied={applied.has(r.parsed.raw.classId)}
              onApply={() => openApply(r.parsed.raw)}
              onDetail={() => setDetailTarget(r.parsed.raw)}
            />
          ))}
        </div>

        {pageCount > 1 && (
          <nav className="tfc-pager" aria-label="Phân trang danh sách lớp">
            <button
              type="button"
              className="tfc-pager__btn"
              disabled={safePage <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
            >
              ← Trước
            </button>
            {Array.from({ length: pageCount }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                className={`tfc-pager__num${n === safePage ? ' is-active' : ''}`}
                aria-current={n === safePage ? 'page' : undefined}
                onClick={() => setPage(n)}
              >
                {n}
              </button>
            ))}
            <button
              type="button"
              className="tfc-pager__btn"
              disabled={safePage >= pageCount}
              onClick={() => setPage((p) => Math.min(pageCount, p + 1))}
            >
              Sau →
            </button>
          </nav>
        )}
      </section>

      {showFormula && (
        <div
          className="cdm-overlay"
          role="presentation"
          onClick={(e) => e.target === e.currentTarget && setShowFormula(false)}
        >
          <div className="cdm tfc-formula-modal" role="dialog" aria-label="Cách tính độ phù hợp">
            <button
              type="button"
              className="cdm__close"
              aria-label="Đóng"
              onClick={() => setShowFormula(false)}
            >
              ✕
            </button>
            <h3 className="tfc-formula-modal__title">Cách tính độ phù hợp</h3>
            <FormulaExplainer weights={criteria.weights} bare />
          </div>
        </div>
      )}

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
          subjects={effSubjects}
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
  readonly showScore: boolean;
  readonly applied: boolean;
  readonly onApply: () => void;
  readonly onDetail: () => void;
}

function ClassCard({
  result,
  subjectName,
  gradeName,
  showScore,
  applied,
  onApply,
  onDetail,
}: CardProps) {
  const { parsed, breakdown } = result;
  const c = parsed.raw;
  const pct = Math.round(breakdown.score);
  const tone = pct >= 75 ? 'high' : pct >= 45 ? 'mid' : 'low';
  const otherNames = parsed.subjectOther
    ? parsed.subjectOther.split(',').map((s) => s.trim()).filter(Boolean)
    : parsed.hasOtherSubject
      ? ['Môn khác']
      : [];
  const subjectLabel =
    parsed.subjectIds.map(subjectName).concat(otherNames).join(', ') || (c.subjectName ?? '—');
  const gradeLabel = gradeName(parsed.gradeId) || c.gradeName || '—';
  const location =
    parsed.lessonMode === 'ONLINE'
      ? 'Online'
      : parsed.provinceName || c.address || c.locationName || 'Offline';
  const learningGoal = c.learningGoal?.trim() ?? '';
  const tutorRequirement = parsed.tutorRequirement?.trim() ?? '';
  const sessionCount = parsed.slots.length;
  const cycleWeeks = parsed.repeatEveryWeeks;
  const onWeeks = [...new Set(parsed.studyWeeks)]
    .filter((w) => Number.isInteger(w) && w >= 1 && w <= cycleWeeks)
    .sort((a, b) => a - b);
  const rhythm =
    cycleWeeks === 1 || onWeeks.length >= cycleWeeks || onWeeks.length === 0
      ? 'tuần'
      : onWeeks.length === 1 && onWeeks[0] === 1
        ? `${cycleWeeks} tuần`
        : `tuần học (tuần ${onWeeks.join(', ')}/${cycleWeeks})`;
  const scheduleSummary =
    sessionCount > 0
      ? parsed.scheduleMode === 'WEEKLY'
        ? `${sessionCount} buổi/${rhythm}`
        : `${sessionCount} buổi`
      : '';

  return (
    <article className={`tfc-card${showScore ? '' : ' tfc-card--noscore'}`}>
      {showScore && (
        <div className={`tfc-score tfc-score--${tone}`}>
          <span className="tfc-score__num">{pct}%</span>
          <span className="tfc-score__unit">phù hợp</span>
        </div>
      )}
      <div className="tfc-card__body">
        <div className="tfc-card__top">
          <h3 className="tfc-card__title">{c.title}</h3>
          {c.expiresAt && <ExpiryBadge expiresAt={c.expiresAt} />}
        </div>
        {/* Không nêu học phí ở đây: lớp nhiều môn mỗi môn một giá — xem chi tiết để biết từng môn. */}
        <div className="tfc-card__meta">
          <span>📚 {subjectLabel}</span>
          <span>🎓 {gradeLabel}</span>
          <span>📍 {location}</span>
        </div>

        <div className="tfc-card__info">
          {learningGoal && (
            <p className="tfc-card__info-row">
              <span className="tfc-card__ico" aria-hidden>🎯</span>
              <span>
                <strong>Mục tiêu:</strong> {learningGoal}
              </span>
            </p>
          )}
          {tutorRequirement && (
            <p className="tfc-card__info-row">
              <span className="tfc-card__ico" aria-hidden>🧑‍🏫</span>
              <span>
                <strong>Yêu cầu gia sư:</strong> {tutorRequirement}
              </span>
            </p>
          )}
          {scheduleSummary && (
            <p className="tfc-card__info-row">
              <span className="tfc-card__ico" aria-hidden>🗓️</span>
              <span>
                <strong>Lịch học:</strong> {scheduleSummary}
              </span>
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
            {applied ? '✓ Đã ứng tuyển' : 'Ứng tuyển'}
          </button>
        </div>
      </div>
    </article>
  );
}
