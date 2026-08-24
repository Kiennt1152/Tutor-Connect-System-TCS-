import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { FormulaExplainer } from '../components/FormulaExplainer';
import { FALLBACK_SUBJECTS, FALLBACK_GRADES } from '../constants/catalogFallback';
import { isOtherSubject, type CatalogOption, type ClassResponse } from '../types/marketplaceTypes';
import {
  FEE_FLOOR,
  emptyCriteria,
  parseClass,
  wardKey,
  type AvailabilitySlot,
  type MatchWeights,
  type TutorCriteria,
} from '../matching/tutorMatching';
import '../components/tutorFindClass.css';

const WEIGHT_LABELS: { key: keyof MatchWeights; label: string; hint: string }[] = [
  { key: 'subject', label: 'Môn học (S)', hint: 'Tỉ lệ môn của lớp mà bạn dạy được' },
  { key: 'location', label: 'Địa điểm (L)', hint: 'Đúng tỉnh, đúng phường/xã bạn chọn' },
  { key: 'salary', label: 'Học phí (P)', hint: 'Từ mốc sàn 50.000đ/giờ lên tới mức bạn mong muốn' },
  { key: 'schedule', label: 'Lịch học (T)', hint: 'Buổi học rơi vào khung giờ bạn rảnh' },
  { key: 'grade', label: 'Khối lớp (E)', hint: 'Đúng khối lớp ghi trong tin' },
];

const WEIGHT_SCALE = ['Bỏ qua', 'Rất thấp', 'Thấp', 'Vừa', 'Cao', 'Rất cao'];
const weightLabel = (v: number) => WEIGHT_SCALE[v] ?? '';

const normalize = (s: string) =>
  s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/đ/g, 'd').trim();

/**
 * Cách gõ nào cũng ra "học online". Gõ dở chừng vẫn nhận: onl · onli · onlin · online —
 * nếu bắt gõ đủ chữ thì phần dở dang rơi xuống ô "Môn học" thành môn ngoài danh mục.
 */
const ONLINE_WORDS = 'onl(?:i(?:n(?:e)?)?)?|truc tuyen|qua mang|tu xa';
const ONLINE_RE = new RegExp(`\\b(?:${ONLINE_WORDS})\\b`);

/** Từ chỉ hình thức/khối phải dọn khỏi phần dư trước khi coi nó là môn ngoài danh mục. */
const LESSON_MODE_RE = new RegExp(
  `\\b(?:${ONLINE_WORDS}|offline|truc tiep|tai nha|dai hoc|vao 10)\\b`,
  'g',
);

/** Tên rút gọn hay gặp khi gõ nhanh -> tên môn trong danh mục. */
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

/** Cụm phải loại TRƯỚC khi dò môn, không thì "sinh viên" thành môn Sinh học. */
const SUBJECT_STOP_PHRASES = ['sinh vien', 'giao vien', 'gia su', 'hoc vien'];

/**
 * Phần dư của câu tìm chỉ được coi là "môn ngoài danh mục" khi nó còn giống tên môn.
 * Gõ bừa ("vfdvdfvfdvdf...") mà vẫn gán vào tiêu chí S thì mọi lớp đều bị chấm 0 điểm
 * môn học — người dùng tưởng hệ thống hỏng. Tên môn thật luôn là vài tiếng ngắn, có
 * nguyên âm, không trộn chữ với số.
 */
function looksLikeSubjectName(text: string): boolean {
  const tokens = text.split(' ').filter(Boolean);
  if (tokens.length === 0 || tokens.length > 4) return false;
  return tokens.every((token) => {
    if (token.length < 2 || token.length > 12) return false;
    if (!/^[a-z]+$/.test(token)) return false;
    if (!/[aeiouy]/.test(token)) return false;
    return !/[bcdfghjklmnpqrstvwxz]{4,}/.test(token);
  });
}

/** Kết quả bóc từ câu gõ tự do — đúng 5 tiêu chí của thanh lọc. */
interface QueryFilters {
  subjectIds: string[];
  otherSubjectText: string;
  gradeIds: string[];
  provinceName: string;
  wardName: string;
  onlineOnly: boolean;
  expectedFee: string;
  availability: AvailabilitySlot[];
  /** Vế nào được gõ trước: "t3 chiều" -> 'day' · "chiều t3" -> 'session'. */
  scheduleLead: 'day' | 'session';
}

const dayCodeOf = (w: string): string =>
  /^t[2-7]$/.test(w) ? w.toUpperCase() : w === 'cn' ? 'CN' : '';

const sessionOf = (w: string): string =>
  w === 'sang' ? 'Sáng' : w === 'chieu' ? 'Chiều' : w === 'toi' ? 'Tối' : '';

/**
 * Bóc lịch rảnh: "sáng T3", "thứ 5 tối", "chủ nhật", "buổi chiều"…
 *
 * Thứ và buổi đứng cạnh nhau thì ghép thành một khung. Nêu lẻ thì GIỮ NGUYÊN vế trống
 * ({day:'T3', session:''}) chứ không bung ra đủ tổ hợp — có thế scoreSchedule mới phân
 * biệt được "thứ 3" với "sáng+chiều+tối thứ 3".
 *
 * Riêng "tối" đứng một mình cần thêm căn cứ, vì bỏ dấu xong nó trùng đại từ "tôi".
 * Nhận là buổi Tối khi có MỘT trong ba dấu hiệu:
 *   · gõ có dấu "tối" (dấu sắc phân biệt hẳn với "tôi") — nên cần cả câu gõ thô,
 *   · viết rõ "buổi tối",
 *   · đứng cạnh một thứ hoặc một buổi khác ("sáng tối chiều", "t3 toi").
 */
function parseAvailabilityQuery(
  q: string,
  rawText: string,
): { slots: AvailabilitySlot[]; lead: 'day' | 'session' } {
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

  const allowLooseEvening =
    /tối/.test(rawText.normalize('NFC').toLowerCase()) || /\bbuoi toi\b/.test(q);

  /** "toi" đứng cạnh một thứ hoặc một buổi khác thì chắc chắn là buổi Tối. */
  const eveningByNeighbour = (i: number): boolean =>
    [words[i - 1], words[i + 1]].some((n) => {
      if (!n) return false;
      const s = sessionOf(n);
      return dayCodeOf(n) !== '' || (s !== '' && s !== 'Tối');
    });

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
    if (!session) return;
    if (session === 'Tối' && !allowLooseEvening && !eveningByNeighbour(i)) return;
    looseSessions.add(session);
  });

  const out = [...pairs];
  for (const day of looseDays) out.push({ day, session: '' });
  for (const session of looseSessions) out.push({ day: '', session });
  const seen = new Set<string>();
  const slots = out.filter((x) => {
    const key = `${x.day}|${x.session}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });

  // Nêu cả hai vế thì vế nào gõ trước được ưu tiên khi xếp hạng (xem rankClasses).
  // Chỉ tính những buổi thực sự lọt vào kết quả — "toi" bị loại thì không được tính là dẫn.
  let lead: 'day' | 'session' = 'day';
  if (slots.some((x) => x.day) && slots.some((x) => x.session)) {
    for (const w of words) {
      if (dayCodeOf(w)) break;
      const session = sessionOf(w);
      if (session && slots.some((x) => x.session === session)) {
        lead = 'session';
        break;
      }
    }
  }

  return { slots, lead };
}

/**
 * Bóc câu gõ tự do thành đúng 5 tiêu chí của thanh lọc. Parser theo luật, không phải LLM.
 * Ví dụ: "toán lý lớp 9 hà nội cầu giấy tối thứ 2 200k".
 */
function parseSmartQuery(
  text: string,
  subjects: readonly CatalogOption[],
  grades: readonly CatalogOption[],
  provinces: readonly CatalogOption[],
  wards: readonly string[],
): QueryFilters {
  const q = ` ${normalize(text).replace(/[,;/|]+/g, ' ')} `;
  const isCert = /ielts|toeic|chung chi/.test(q);
  // "lớp thi chứng chỉ" / "luyện thi chứng chỉ" / "khối chứng chỉ" là tên KHỐI LỚP, không
  // phải môn. Có tiền tố lớp/khối/luyện thi thì chỉ điền ô Khối lớp, để yên ô Môn học.
  const certIsGrade = /\b(?:lop|khoi|luyen thi)\s+(?:thi\s+)?chung chi\b/.test(q);

  // --- Môn (S) ---
  const subjectIds = new Set<string>();
  const aliasPairs: { id: string; kw: string }[] = [];
  for (const sub of subjects) {
    const name = normalize(sub.name);
    for (const kw of SUBJECT_ALIASES[name] ?? [name]) {
      if (kw) aliasPairs.push({ id: String(sub.id), kw });
    }
  }
  aliasPairs.sort((a, b) => b.kw.length - a.kw.length);
  let scan = q;
  for (const phrase of SUBJECT_STOP_PHRASES) {
    scan = scan.split(` ${phrase} `).join('  ');
  }
  for (const { id, kw } of aliasPairs) {
    if (scan.includes(` ${kw} `)) {
      subjectIds.add(id);
      scan = scan.split(` ${kw} `).join('  ');
    }
  }
  if (isCert && !certIsGrade) {
    const cert = subjects.find((x) => /chung chi/.test(normalize(x.name)));
    if (cert) subjectIds.add(String(cert.id));
  }

  // --- Khối lớp (E) ---
  const gradeIds = new Set<string>();
  for (const m of q.matchAll(/(?:lop|khoi)\s*(\d{1,2})/g)) {
    const g = grades.find((x) => normalize(x.name) === `lop ${Number(m[1])}`);
    if (g) gradeIds.add(String(g.id));
  }
  // Số trần 1..12 cũng là khối lớp ("toán 9", "sáng tối chiều 12"). Phải gạt trước những
  // con số đã mang nghĩa khác: "thứ 2" là thứ, "200k" / "200000" là học phí. Số dính liền
  // chữ như "t5" không lọt vào đây vì \b không nhận ranh giới giữa hai ký tự chữ-số.
  const gradeScan = q
    .replace(/\bthu\s*[2-7]\b/g, ' ')
    .replace(/\d{2,4}\s*k(?![a-z])/g, ' ')
    .replace(/\d[\d.,]{4,}/g, ' ');
  for (const m of gradeScan.matchAll(/\b(\d{1,2})\b/g)) {
    const n = Number(m[1]);
    if (n < 1 || n > 12) continue;
    const g = grades.find((x) => normalize(x.name) === `lop ${n}`);
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

  // --- Địa điểm (L) ---
  // "học online" / "trực tuyến" là MỘT lựa chọn địa điểm, loại trừ tỉnh/xã.
  const onlineOnly = ONLINE_RE.test(q);

  let provinceName = '';
  let provinceCore = '';
  const provCands = provinces
    .map((p) => ({ name: p.name, core: normalize(p.name).replace(/^(tp|thanh pho|tinh)\s+/, '') }))
    .filter((p) => p.core.length >= 3)
    .sort((a, b) => b.core.length - a.core.length);
  for (const p of provCands) {
    if (q.includes(` ${p.core} `)) {
      provinceName = p.name;
      provinceCore = p.core;
      break;
    }
  }

  // Phường/xã không có catalog riêng -> dò theo tên đang có trong các tin đã tải.
  let wardName = '';
  let wardCore = '';
  const wardCands = wards
    .map((w) => ({ name: w, core: wardKey(w) }))
    .filter((w) => w.core.length >= 3 && w.core !== provinceCore)
    .sort((a, b) => b.core.length - a.core.length);
  for (const w of wardCands) {
    if (q.includes(` ${w.core} `)) {
      wardName = w.name;
      wardCore = w.core;
      break;
    }
  }

  // --- Học phí (P) ---
  let expectedFee = '';
  const kMatch = /(\d{2,4})\s*k(?![a-z])/.exec(q);
  if (kMatch) {
    expectedFee = clampFee(String(Number(kMatch[1]) * 1000));
  } else {
    const rawMatch = /(\d[\d.,]{4,})\s*(?:d|dong|vnd)?/.exec(q);
    const n = rawMatch ? Number(rawMatch[1].replace(/[.,]/g, '')) : 0;
    if (n > 0) expectedFee = clampFee(String(n));
  }

  // --- Lịch học (T) ---
  const { slots: availability, lead: scheduleLead } = parseAvailabilityQuery(q, text);

  // --- Phần còn lại coi là môn ngoài danh mục ---
  let otherSubjectText = '';
  if (subjectIds.size === 0) {
    let rest = q;
    if (provinceCore) rest = rest.split(provinceCore).join(' ');
    if (wardCore) rest = rest.split(wardCore).join(' ');
    rest = rest
      .replace(/(?:lop|khoi)\s*\d{1,2}/g, ' ')
      .replace(/\b\d{1,2}\b/g, ' ')
      .replace(/\d{2,4}\s*k(?![a-z])/g, ' ')
      .replace(/\d[\d.,]{4,}/g, ' ')
      .replace(/\b(thu\s*[2-7]|t[2-7]|chu nhat|cn|buoi|sang|chieu|toi)\b/g, ' ')
      .replace(LESSON_MODE_RE, ' ')
      .replace(/\b(chung chi|ielts|toeic|lop|khoi)\b/g, ' ')
      .replace(/\b(tinh|thanh pho|tp|quan|huyen|phuong|xa|thi xa|khu vuc|khu)\b/g, ' ')
      .replace(/\b(mon|tim|gia su|day|hoc phi|hoc|gio|vnd|dong|luyen thi|thi|o|tai|can)\b/g, ' ')
      .replace(/\b(yeu cau|muc tieu|yeu|cau|la|vien)\b/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    // Tối thiểu 3 ký tự: gõ dở chừng còn 2 chữ ("on" của "online") không phải tên môn.
    if (rest.length >= 3 && looksLikeSubjectName(rest)) otherSubjectText = rest;
  }

  return {
    subjectIds: [...subjectIds],
    otherSubjectText,
    gradeIds: [...gradeIds],
    provinceName: onlineOnly ? '' : provinceName,
    wardName: onlineOnly ? '' : wardName,
    onlineOnly,
    expectedFee,
    availability,
    scheduleLead,
  };
}

/**
 * Mức mong muốn không được thấp hơn mốc sàn của mọi tin tuyển. Nhận số dưới sàn thì
 * tiêu chí P vô nghĩa: tin nào cũng trả cao hơn nên lớp nào cũng trọn điểm.
 */
const clampFee = (raw: string): string => {
  const n = Number(raw) || 0;
  if (n <= 0) return '';
  return String(Math.max(FEE_FLOOR, n));
};

export const money = (n: number) => `${n.toLocaleString('vi-VN')}đ`;

/** "Tối T2" · lịch CUSTOM thì ghi ngày: "Tối 07/09". */
export function slotLabel(slot: { day: string; date: string; session: string }): string {
  const when = slot.day || (slot.date ? slot.date.slice(8, 10) + '/' + slot.date.slice(5, 7) : '');
  return [slot.session, when].filter(Boolean).join(' ');
}

export interface ClassSearchOptions {
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly provinces: CatalogOption[];
  /** Danh sách lớp đang có — chỉ dùng để lấy từ điển phường/xã cho câu gõ. */
  readonly classes: ClassResponse[];
}

export interface ClassSearch {
  /** Tiêu chí đã chốt, đem thẳng vào searchClasses/rankClasses. */
  readonly criteria: TutorCriteria;
  /** Có đang lọc gì không — chưa lọc thì đừng khoe % phù hợp. */
  readonly hasFilter: boolean;
  /** Học phí đang gõ (đã kẹp mức sàn). */
  readonly fee: string;
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly subjectName: (id: string) => string;
  readonly gradeName: (id: string) => string;
  /** Tên các môn đang lọc, để in ở tiêu đề kết quả. */
  readonly subjectNames: string;
  readonly selectedCount: number;
  /** Toàn bộ giao diện: ô gõ + 5 ô lọc + 5 thanh trượt + modal công thức. */
  readonly bar: ReactNode;
}

/**
 * Thanh tìm dùng chung cho MỌI màn duyệt lớp: gia sư tìm yêu cầu giảng dạy và phụ huynh
 * tìm lớp đều cần đúng một bộ — ô gõ nhanh, 5 ô lọc (S · L · P · T · E) và 5 thanh trượt
 * ưu tiên. Để mỗi màn một bản sao thì mỗi lần đổi luật chấm điểm phải sửa hai chỗ.
 *
 * Hook trả về cả giao diện (`bar`) lẫn tiêu chí đã chốt (`criteria`); màn gọi chỉ việc
 * dựng danh sách kết quả theo kiểu thẻ của riêng nó.
 */
export function useClassSearch({
  subjects,
  grades,
  provinces,
  classes,
}: ClassSearchOptions): ClassSearch {
  const [criteria, setCriteria] = useState<TutorCriteria>(emptyCriteria);
  const [query, setQuery] = useState('');
  /** Câu đã BẤM TÌM. Tách khỏi `query` để không bóc lọc theo từng phím gõ dở dang
   *  ("17k" vừa gõ tới "1" đã nhảy thành 50.000đ). */
  const [submittedQuery, setSubmittedQuery] = useState('');
  /** Tăng mỗi lần bấm Tìm. Bấm lại đúng câu cũ vẫn phải điền lại 5 ô (người dùng có thể
   *  đã sửa tay rồi muốn quay về đúng câu đang gõ). */
  const [submitSeq, setSubmitSeq] = useState(0);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [gradeIds, setGradeIds] = useState<string[]>([]);
  const [provinceName, setProvinceName] = useState('');
  const [wardName, setWardName] = useState('');
  const [onlineOnly, setOnlineOnly] = useState(false);
  const [availability, setAvailability] = useState<AvailabilitySlot[]>([]);
  const [scheduleLead, setScheduleLead] = useState<'day' | 'session'>('day');
  const [queryFee, setQueryFee] = useState('');
  const [otherText, setOtherText] = useState('');
  /** Câu đã bóc lần gần nhất. Danh sách lớp tự tải lại khi quay lại tab -> knownWards đổi
   *  tham chiếu -> effect chạy lại; không có mốc này nó sẽ xoá sạch ô người dùng vừa
   *  chọn tay dù câu tìm không hề đổi. */
  const lastParsedSeq = useRef(0);
  const [showFormula, setShowFormula] = useState(false);
  /** Panel trọng số đóng = chấm trung bình cộng 5 tiêu chí; mở mới dùng mức tự kéo. */
  const [showWeights, setShowWeights] = useState(false);

  const effSubjects = useMemo(
    () => (subjects.length > 0 ? subjects : [...FALLBACK_SUBJECTS]),
    [subjects],
  );
  const effGrades = useMemo(() => (grades.length > 0 ? grades : [...FALLBACK_GRADES]), [grades]);

  const subjectName = useMemo(() => {
    const m = new Map(effSubjects.map((s) => [String(s.id), s.name]));
    return (id: string) => (isOtherSubject(id) ? 'Môn khác' : (m.get(id) ?? `#${id}`));
  }, [effSubjects]);
  const gradeName = useMemo(() => {
    const m = new Map(effGrades.map((g) => [String(g.id), g.name]));
    return (id: string) => m.get(id) ?? '';
  }, [effGrades]);

  // Chưa mở panel -> mọi tiêu chí ưu tiên cao như nhau, mỗi tiêu chí ăn trọn 20% phần của nó.
  const effectiveWeights = useMemo<MatchWeights>(
    () =>
      showWeights
        ? criteria.weights
        : { subject: 5, location: 5, salary: 5, schedule: 5, grade: 5 },
    [showWeights, criteria.weights],
  );

  /** id khối -> số lớp. Chỉ "Lớp N" mới có số; chứng chỉ / đại học để ngoài. */
  const gradeLevels = useMemo<Record<string, number>>(() => {
    const out: Record<string, number> = {};
    for (const g of effGrades) {
      const m = /^lop (\d{1,2})$/.exec(normalize(g.name));
      if (m) out[String(g.id)] = Number(m[1]);
    }
    return out;
  }, [effGrades]);

  /** Tên phường/xã đang có trong các tin đã tải — làm từ điển cho câu gõ. */
  const knownWards = useMemo(() => {
    const seen = new Map<string, string>();
    for (const raw of classes) {
      const pc = parseClass(raw);
      const w = pc.wardName || pc.districtName;
      if (w) seen.set(wardKey(w), w);
    }
    return [...seen.values()];
  }, [classes]);

  const activeCriteria = useMemo<TutorCriteria>(
    () => ({
      ...criteria,
      weights: effectiveWeights,
      subjectIds: selectedIds,
      otherSubjectText: otherText,
      gradeIds,
      gradeLevels,
      provinceName,
      wardName,
      onlineOnly,
      availability,
      scheduleLead,
      expectedFee: clampFee(queryFee),
    }),
    [
      criteria,
      effectiveWeights,
      selectedIds,
      otherText,
      gradeIds,
      gradeLevels,
      provinceName,
      wardName,
      onlineOnly,
      availability,
      scheduleLead,
      queryFee,
    ],
  );

  // Ngừng tay khoảng nửa giây thì tự bóc câu — khỏi phải bấm Tìm cho mỗi lần thử.
  // Chờ một nhịp chứ không bóc theo từng phím: gõ "350k" mà bóc ngay thì nó lần lượt
  // nhảy 3.000đ -> 35.000đ -> 350.000đ, còn "17k" thì bị nâng lên mức sàn giữa chừng.
  const skipFirstAuto = useRef(true);
  useEffect(() => {
    if (skipFirstAuto.current) {
      skipFirstAuto.current = false;
      return;
    }
    const id = setTimeout(() => {
      setSubmittedQuery(query);
      setSubmitSeq((n) => n + 1);
    }, 500);
    return () => clearTimeout(id);
  }, [query]);

  // Câu gõ tự do chỉ là ĐƯỜNG TẮT: nó điền vào đúng 5 ô lọc bên dưới, còn giá trị dùng
  // để chấm điểm luôn lấy từ các ô đó. Sửa tay ô nào cũng được, chỉ khi câu gõ đổi thì
  // mới ghi đè lại.
  useEffect(() => {
    // Chặn effect chạy lại khi `knownWards` đổi tham chiếu (danh sách lớp tải lại mỗi
    // lần cửa sổ được focus) — chạy lại với câu rỗng sẽ xoá sạch ô người dùng chọn tay.
    if (lastParsedSeq.current === submitSeq) return;
    lastParsedSeq.current = submitSeq;
    if (!submittedQuery.trim()) {
      setSelectedIds([]);
      setOtherText('');
      setGradeIds([]);
      setProvinceName('');
      setWardName('');
      setOnlineOnly(false);
      setQueryFee('');
      setAvailability([]);
      return;
    }
    const f = parseSmartQuery(submittedQuery, effSubjects, effGrades, provinces, knownWards);
    setSelectedIds(f.subjectIds);
    setOtherText(f.otherSubjectText);
    setGradeIds(f.gradeIds);
    setProvinceName(f.provinceName);
    setWardName(f.wardName);
    setOnlineOnly(f.onlineOnly);
    setQueryFee(f.expectedFee);
    setAvailability(f.availability);
    setScheduleLead(f.scheduleLead);
  }, [submitSeq, submittedQuery, effSubjects, effGrades, provinces, knownWards]);

  const selectedNames = selectedIds.map((id) => subjectName(id)).join(', ');

  const hasFilter =
    selectedIds.length > 0 ||
    gradeIds.length > 0 ||
    provinceName !== '' ||
    wardName !== '' ||
    onlineOnly ||
    availability.length > 0 ||
    queryFee !== '' ||
    otherText !== '';

  // Câu gõ điền được gì thì hiện NGUYÊN thứ đó thành chip. Trước đây chỗ này là 5 ô lọc
  // xổ xuống cho chọn tay, nhưng đã có ô gõ lo trọn cả 5 tiêu chí thì 5 ô kia chỉ tổ
  // chiếm chỗ và bày ra "Mọi nơi / Mọi khối" — những thứ không hề được lọc.
  const chips: { label: string; value: string }[] = [];
  if (selectedIds.length > 0 || otherText.trim()) {
    chips.push({
      label: 'Môn',
      value: [...selectedIds.map(subjectName), ...(otherText.trim() ? [otherText.trim()] : [])].join(
        ', ',
      ),
    });
  }
  if (gradeIds.length > 0) {
    chips.push({ label: 'Khối lớp', value: gradeIds.map(gradeName).filter(Boolean).join(', ') });
  }
  if (onlineOnly || provinceName || wardName) {
    chips.push({
      label: 'Địa điểm',
      value: onlineOnly ? 'Học online' : [wardName, provinceName].filter(Boolean).join(', '),
    });
  }
  if (clampFee(queryFee)) {
    chips.push({ label: 'Học phí', value: `${money(Number(clampFee(queryFee)))}/giờ` });
  }
  if (availability.length > 0) {
    chips.push({
      label: 'Lịch học',
      value: availability.map((a) => `${a.session} ${a.day}`.trim()).join(' · '),
    });
  }

  const setWeight = (key: keyof MatchWeights, value: number) =>
    setCriteria((c) => ({ ...c, weights: { ...c.weights, [key]: value } }));

  /** Bấm Tìm / Enter: bóc câu đang gõ vào 5 ô lọc. */
  function runSearch() {
    setSubmittedQuery(query);
    setSubmitSeq((n) => n + 1);
  }

  function clearSearch() {
    setQuery('');
    setSubmittedQuery('');
    setSubmitSeq((n) => n + 1);
    setSelectedIds([]);
    setGradeIds([]);
    setProvinceName('');
    setWardName('');
    setOnlineOnly(false);
    setAvailability([]);
    setQueryFee('');
    setOtherText('');
  }

  const bar = (
    <>
        <div className="tfc-search">
          {/* Thanh lọc: 5 ô nằm ngang, xếp đúng thứ tự 5 thanh trượt bên dưới (S · L · P · T · E).
              Mỗi ô là một nút, bấm mới xổ bảng chọn — vì bảng chọn của Địa điểm và Lịch học
              rộng hơn hẳn một cột nên không thể nhét thẳng vào hàng. */}
          <div className="tfc-search__row">
            <div className="tfc-search__box">
              <input
                className="tfc-search__input"
                type="text"
                placeholder="Hãy nhập: môn, khối lớp, tỉnh/phường, thứ + buổi, học phí"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') runSearch();
                }}
                aria-label="Gõ nhanh để điền vào 5 ô lọc bên dưới"
              />
              {query && (
                <button
                  type="button"
                  className="tfc-search__clear"
                  onClick={clearSearch}
                  aria-label="Xóa câu tìm"
                >
                  ✕
                </button>
              )}
            </div>
            <button
              type="button"
              className="tfc-btn tfc-btn--primary tfc-search__btn"
              onClick={runSearch}
            >
              Tìm
            </button>
          </div>

          {/* Câu ví dụ tách khỏi placeholder: placeholder chỉ nêu các thành phần cần gõ,
              dòng này minh hoạ một câu hoàn chỉnh và vẫn đọc được sau khi đã gõ. */}
          <p className="tfc-search__hint">
            VD: <em>toán lớp 9 hà nội cầu giấy tối thứ 2 200k</em>
          </p>

          {chips.length > 0 && (
            <div className="tfc-chips">
              {chips.map((c) => (
                <span className="tfc-chip" key={c.label}>
                  <span className="tfc-chip__key">{c.label}:</span> {c.value}
                </span>
              ))}
              <button type="button" className="tfc-chips__clear" onClick={clearSearch}>
                Xóa lọc
              </button>
            </div>
          )}

          <aside className={`tfc-panel${hasFilter ? '' : ' is-locked'}`}>
          <div className="tfc-panel__head">
            {/* Nút này vừa mở/đóng bộ thanh trượt, vừa quyết định cách chấm: đóng thì 5 tiêu
                chí ngang nhau (trung bình cộng), mở mới ăn theo mức ưu tiên tự kéo. */}
            <button
              type="button"
              className={`tfc-panel__toggle${showWeights ? ' is-open' : ''}`}
              disabled={!hasFilter}
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
              {hasFilter ? (
                <>
                  Kéo để chọn tiêu chí <strong>quan trọng hơn</strong> khi xếp hạng lớp ·{' '}
                  <span className="tfc-weight-legend">0 = bỏ qua · 5 = ưu tiên cao nhất</span>
                </>
              ) : (
                <>
                  Chọn ít nhất một tiêu chí ở trên, rồi mới chỉnh được mức ưu tiên.
                </>
              )}
            </p>
          )}

          {showWeights && (
          <div className="tfc-weight-list">
            {WEIGHT_LABELS.map((w) => {
              const v = criteria.weights[w.key];
              return (
                <div key={w.key} className="tfc-weight" title={hasFilter ? w.hint : undefined}>
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
                    disabled={!hasFilter}
                    onChange={(e) => setWeight(w.key, Number(e.target.value))}
                  />
                </div>
              );
            })}
          </div>
          )}
          </aside>
        </div>
        {showFormula && createPortal(
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
              <FormulaExplainer criteria={activeCriteria} bare />
            </div>
          </div>,
          document.body,
        )}
    </>
  );

  return {
    criteria: activeCriteria,
    hasFilter,
    fee: clampFee(queryFee),
    subjects: effSubjects,
    grades: effGrades,
    subjectName,
    gradeName,
    subjectNames: selectedNames,
    selectedCount: selectedIds.length,
    bar,
  };
}
