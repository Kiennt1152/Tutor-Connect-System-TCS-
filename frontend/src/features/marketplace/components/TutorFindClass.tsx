import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import { ApplyClassModal } from './ApplyClassModal';
import { ExpiryBadge } from '../../../shared/components/ExpiryBadge';
import { FormulaExplainer } from './FormulaExplainer';
import { FALLBACK_SUBJECTS, FALLBACK_GRADES } from '../constants/catalogFallback';
import {
  isOtherSubject,
  type CatalogOption,
  type ClassResponse,
} from '../types/marketplaceTypes';
import {
  SESSIONS,
  WEEKDAYS,
  emptyCriteria,
  rankClasses,
  type AvailabilitySlot,
  type MatchResult,
  type MatchWeights,
  type TutorCriteria,
} from '../matching/tutorMatching';
import './tutorFindClass.css';

const WEIGHT_LABELS: { key: keyof MatchWeights; label: string; hint: string }[] = [
  { key: 'subject', label: 'Môn học (S)', hint: 'Tỉ lệ môn của lớp mà bạn dạy được' },
  { key: 'location', label: 'Địa điểm (L)', hint: 'Đúng tỉnh, đúng phường/xã bạn chọn' },
  { key: 'salary', label: 'Học phí (P)', hint: 'Lớp trả đủ mức bạn mong muốn' },
  { key: 'schedule', label: 'Lịch học (T)', hint: 'Buổi học rơi vào khung giờ bạn rảnh' },
  { key: 'grade', label: 'Khối lớp (E)', hint: 'Đúng khối lớp ghi trong tin' },
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
  provinceName: string;
  expectedFee: string;
  availability: AvailabilitySlot[];
  lessonMode: '' | 'ONLINE' | 'OFFLINE';
  goalKeywords: string[];
  reqKeywords: string[];
  otherSubjectText: string;
}

const dayCodeOf = (w: string): string =>
  /^t[2-7]$/.test(w) ? w.toUpperCase() : w === 'cn' ? 'CN' : '';

const sessionOf = (w: string): string =>
  w === 'sang' ? 'Sáng' : w === 'chieu' ? 'Chiều' : w === 'toi' ? 'Tối' : '';

/**
 * Bóc lịch rảnh từ câu tìm: "sáng T3", "thứ 5 tối", "chủ nhật", "buổi chiều"…
 *
 * Thứ và buổi đứng cạnh nhau thì ghép thành một khung giờ. Còn lẻ:
 * chỉ có thứ -> lấy cả 3 buổi của thứ đó; chỉ có buổi -> lấy buổi đó của cả 7 thứ.
 *
 * Riêng "tối" phải đi kèm thứ (hoặc viết "buổi tối") mới tính, vì bỏ dấu xong nó
 * trùng đại từ "tôi" — câu "tôi muốn dạy toán" mà nhận thành buổi Tối là sai.
 */
function parseAvailabilityQuery(q: string): AvailabilitySlot[] {
  const words = q
    .replace(/\bthu\s*([2-7])\b/g, ' t$1 ')
    .replace(/\bchu nhat\b/g, ' cn ')
    .replace(/\bbuoi\b/g, ' ')
    .split(/\s+/)
    .filter(Boolean);

  const pairs: AvailabilitySlot[] = [];
  const used = new Array<boolean>(words.length).fill(false);
  for (let i = 0; i < words.length - 1; i += 1) {
    if (used[i]) continue;
    const a = words[i];
    const b = words[i + 1];
    const session = sessionOf(a) || sessionOf(b);
    const day = dayCodeOf(a) || dayCodeOf(b);
    if (session && day && (sessionOf(a) ? dayCodeOf(b) : dayCodeOf(a))) {
      pairs.push({ day, session });
      used[i] = true;
      used[i + 1] = true;
      i += 1;
    }
  }

  const allowLooseEvening = /\bbuoi toi\b/.test(q);
  const looseDays = new Set<string>();
  const looseSessions = new Set<string>();
  words.forEach((w, i) => {
    if (used[i]) return;
    const day = dayCodeOf(w);
    if (day) {
      looseDays.add(day);
      return;
    }
    const session = sessionOf(w);
    if (session && (session !== 'Tối' || allowLooseEvening)) looseSessions.add(session);
  });

  const out = [...pairs];
  for (const day of looseDays) for (const session of SESSIONS) out.push({ day, session });
  for (const session of looseSessions) {
    for (const d of WEEKDAYS) out.push({ day: d.code, session });
  }
  const seen = new Set<string>();
  return out.filter((s) => {
    const key = `${s.day}|${s.session}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
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

  let provinceName = '';
  let provinceCore = '';
  const cands = provinces
    .map((p) => ({ name: p.name, core: normalize(p.name).replace(/^(tp|thanh pho|tinh)\s+/, '') }))
    .filter((p) => p.core.length >= 3)
    .sort((a, b) => b.core.length - a.core.length);
  for (const p of cands) {
    if (q.includes(` ${p.core} `)) {
      provinceName = p.name;
      provinceCore = p.core;
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
  const availability = parseAvailabilityQuery(q);

  let otherSubjectText = '';
  if (subjectIds.size === 0) {
    let s = q;
    if (provinceCore) s = s.split(provinceCore).join(' ');
    for (const kw of [...goalKeywords, ...reqKeywords]) s = s.split(kw).join(' ');
    s = s
      .replace(/(?:lop|khoi)\s*\d{1,2}/g, ' ')
      .replace(/\d{2,4}\s*k(?![a-z])/g, ' ')
      .replace(/\d[\d.,]{4,}/g, ' ')
      .replace(/\b(online|offline|truc tiep|truc tuyen|tai nha|tai nguoi hoc|dai hoc|vao 10)\b/g, ' ')
      // Thứ + buổi đã thành lịch rảnh rồi, để sót lại sẽ bị hiểu nhầm thành "môn khác"
      .replace(/\b(thu\s*[2-7]|t[2-7]|chu nhat|cn|buoi|sang|chieu|toi)\b/g, ' ')
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
    provinceName,
    expectedFee,
    availability,
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
  const [provinceName, setProvinceName] = useState('');
  const [wardName, setWardName] = useState('');
  const [availability, setAvailability] = useState<AvailabilitySlot[]>([]);
  const [queryFee, setQueryFee] = useState('');
  /** Mức trong hồ sơ — chỉ dùng làm gợi ý cho ô học phí, không phải giá trị mặc định. */
  const [profileFee, setProfileFee] = useState('');
  const [queryMode, setQueryMode] = useState<QueryFilters['lessonMode']>('');
  const [goalKeys, setGoalKeys] = useState<string[]>([]);
  const [reqKeys, setReqKeys] = useState<string[]>([]);
  const [otherText, setOtherText] = useState('');
  const [searched, setSearched] = useState(false);
  const [showFormula, setShowFormula] = useState(false);
  /** Panel trọng số đóng = chấm trung bình cộng 5 tiêu chí; mở mới dùng mức tự kéo. */
  const [showWeights, setShowWeights] = useState(false);
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
        // Chỉ GỢI Ý mức trong hồ sơ (đổ vào placeholder), KHÔNG tự điền thành giá trị:
        // tự điền thì tiêu chí P âm thầm trừ điểm dù gia sư chưa hề khai mức nào.
        setProfileFee(String(Math.round(Number(p.hourlyRate))));
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
  // Chưa mở panel -> mọi tiêu chí ngang nhau, tức là trung bình cộng đơn thuần.
  const effectiveWeights = useMemo<MatchWeights>(
    () =>
      showWeights
        ? criteria.weights
        : { subject: 1, location: 1, salary: 1, schedule: 1, grade: 1 },
    [showWeights, criteria.weights],
  );

  const activeCriteria = useMemo<TutorCriteria>(
    () => ({
      ...criteria,
      weights: effectiveWeights,
      subjectIds: selectedIds,
      otherSubjectText: otherText,
      gradeIds,
      provinceName,
      wardName,
      availability,
      expectedFee: queryFee,
      onlineOnly: queryMode === 'ONLINE',
    }),
    [
      criteria,
      effectiveWeights,
      selectedIds,
      otherText,
      gradeIds,
      provinceName,
      wardName,
      availability,
      queryFee,
      queryMode,
    ],
  );

  const results = useMemo(() => {
    let out = rankClasses(classes, activeCriteria);
    if (selectedIds.length > 0) {
      const wanted = new Set(selectedIds);
      out = out.filter((r) => r.parsed.subjectIds.some((id) => wanted.has(id)));
    } else if (otherText) {
      out = out.filter((r) => r.parsed.hasOtherSubject);
    }
    // Khối lớp (E) và địa điểm (L) KHÔNG lọc cứng nữa: lọc cứng thì mọi lớp còn lại đều
    // khớp 100%, hai tiêu chí đó coi như bị vô hiệu. Để chúng chấm điểm và tự tụt hạng.
    if (queryMode === 'ONLINE') out = out.filter((r) => r.parsed.lessonMode === 'ONLINE');
    if (queryMode === 'OFFLINE') out = out.filter((r) => r.parsed.lessonMode !== 'ONLINE');
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
  }, [classes, activeCriteria, selectedIds, queryMode, goalKeys, reqKeys, otherText]);

  // Phân trang: 6 lớp / trang (2 cột × 3 hàng); lớp thứ 7 nhảy sang trang 2.
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const pageResults = results.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
  // Kết quả đổi (tìm mới / lọc khác) -> quay về trang 1.
  useEffect(() => {
    setPage(1);
  }, [selectedIds, gradeIds, provinceName, wardName, availability, queryMode, queryFee, goalKeys, reqKeys, otherText]);

  useEffect(() => {
    if (!query.trim()) {
      // Chỉ xoá thứ do câu tìm sinh ra. Khối lớp / địa điểm / học phí / lịch rảnh có ô
      // chọn riêng bên dưới nên phải giữ nguyên, xoá hết là gia sư mất lựa chọn vừa đặt.
      setSelectedIds([]);
      setQueryMode('');
      setGoalKeys([]);
      setReqKeys([]);
      setOtherText('');
      return;
    }
    const f = parseSmartQuery(query, effSubjects, effGrades, provinces);
    setSelectedIds(f.subjectIds);
    setQueryMode(f.lessonMode);
    setGoalKeys(f.goalKeywords);
    setReqKeys(f.reqKeywords);
    setOtherText(f.otherSubjectText);
    // Câu tìm chỉ điền hộ khi thực sự nhận ra; không nhận ra thì giữ lựa chọn hiện tại.
    if (f.gradeIds.length > 0) setGradeIds(f.gradeIds);
    if (f.provinceName) {
      setProvinceName(f.provinceName);
      setWardName('');
    }
    if (f.expectedFee) setQueryFee(f.expectedFee);
    if (f.availability.length > 0) setAvailability(f.availability);
    setSearched(true);
  }, [query, effSubjects, effGrades, provinces]);

  const selectedNames = selectedIds.map((id) => subjectName(id)).join(', ');

  const understoodTags = [
    ...selectedIds.map((id) => ({ kind: 'Môn', label: subjectName(id) })),
    ...gradeIds.map((id) => ({ kind: 'Khối', label: gradeName(id) })),
    ...(provinceName ? [{ kind: 'Tỉnh', label: provinceName }] : []),
    ...(wardName ? [{ kind: 'Phường/Xã', label: wardName }] : []),
    // Ít khung thì kể tên ra ("Sáng T3") cho gia sư đối chiếu; nhiều quá thì gộp số lượng.
    ...(availability.length > 0
      ? availability.length <= 4
        ? availability.map((a) => ({ kind: 'Rảnh', label: `${a.session} ${a.day}` }))
        : [{ kind: 'Rảnh', label: `${availability.length} khung giờ` }]
      : []),
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
    provinceName !== '' ||
    wardName !== '' ||
    availability.length > 0 ||
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
    setProvinceName('');
    setWardName('');
    setAvailability([]);
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
          {/* Nút này vừa mở/đóng bộ thanh trượt, vừa quyết định cách chấm: đóng thì 5 tiêu
              chí ngang nhau (trung bình cộng), mở mới ăn theo mức ưu tiên tự kéo. */}
          <button
            type="button"
            className={`tfc-panel__toggle${showWeights ? ' is-open' : ''}`}
            disabled={!searched}
            aria-expanded={showWeights}
            onClick={() => setShowWeights((v) => !v)}
          >
            <span className="tfc-panel__title">Mức độ ưu tiên khi tìm</span>
            <span className="tfc-panel__toggle-hint">
              {showWeights ? 'đang theo mức bạn kéo' : 'đang tính trung bình 5 tiêu chí'}
            </span>
            <span className="tfc-panel__caret" aria-hidden>
              {showWeights ? '▲' : '▼'}
            </span>
          </button>
          <button
            type="button"
            className="tfc-formula-btn"
            onClick={() => setShowFormula(true)}
            title="Xem công thức chấm độ phù hợp"
          >
            Cách tính ?
          </button>
        </div>
        {showWeights && (
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
        )}

        {/* Bộ ô nhập tiêu chí (khối lớp / tỉnh + phường / học phí / lưới rảnh) đã ẩn.
            Bốn tiêu chí đó giờ chỉ đặt qua câu tìm — parseSmartQuery bóc ra rồi hiện lại
            ở hàng thẻ "Đã hiểu". Muốn bật lại thì trả khối JSX này về, state và CSS
            (.tfc-criteria / .tfc-avail) vẫn còn nguyên. */}

        {showWeights && (
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
        )}
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
            <FormulaExplainer weights={effectiveWeights} bare />
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
          defaultRate={Number(queryFee || profileFee) || undefined}
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
