/**
 * ====================================================================================================
 * [UC-14] MÀN HÌNH TÌM KIẾM GIA SƯ (FIND TUTOR PAGE)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Danh bạ gia sư toàn sàn với bộ lọc theo môn học, khu vực, đánh giá sao và học phí.
 * 2. Xem hồ sơ năng lực, lịch rảnh và gửi yêu cầu mời dạy kèm trực tiếp.
 * * @author Vũ Quốc Khánh (khanhvqhe176783)
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './HomePage.css';
import './FindTutorPage.css';

/** Bỏ dấu tiếng Việt + hạ chữ thường để tìm kiếm không phân biệt dấu. */
const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .toLowerCase()
    .trim();

const PAGE_SIZE = 6;

type Gender = '' | 'MALE' | 'FEMALE' | 'OTHER';

type Filters = {
  keyword: string;
  /**
   * Giới tính bóc ra từ ô tìm kiếm. Khớp LỎNG (giới tính HOẶC tên), vì "nam" vừa là giới tính
   * vừa là tên người rất phổ biến — chặn cứng sẽ làm mất kết quả người dùng đang tìm.
   */
  queryGender: Gender;
  maxPrice: string;
  minExperience: string;
  minRating: string;
  verifiedOnly: boolean;
};

const EMPTY_FILTERS: Filters = {
  keyword: '',
  queryGender: '',
  maxPrice: '',
  minExperience: '',
  minRating: '',
  verifiedOnly: false,
};

/** Các tiêu chí ô tìm kiếm hiểu được (chỉ để đọc, không bấm được). */
const SEARCH_EXAMPLES = ['Tên', 'Giới tính', 'Giá tiền', 'Kinh nghiệm', 'Số sao'];


const toNumber = (value: string) => {
  const parsed = Number(value.replace(/[^\d.]/g, ''));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
};

/** Từ khóa giới tính (đã bỏ dấu) -> mã giới tính. */
const GENDER_WORDS: Record<string, Gender> = { nam: 'MALE', nu: 'FEMALE', khac: 'OTHER' };

/** Chiều ngược lại, để đối chiếu với tên gia sư khi khớp lỏng. */
const GENDER_LABELS: Record<Exclude<Gender, ''>, string> = {
  MALE: 'nam',
  FEMALE: 'nu',
  OTHER: 'khac',
};

type ParsedQuery = {
  /** Phần chữ còn lại sau khi đã bóc tiêu chí — dùng để khớp tên/mô tả. */
  text: string;
  gender?: Gender;
  maxPrice?: string;
  minExperience?: string;
  minRating?: string;
  verifiedOnly?: boolean;
};

/**
 * Đọc câu tìm kiếm tự do thành tiêu chí lọc, để người dùng gõ thẳng
 * "nữ dưới 200k trên 3 năm từ 4 sao" thay vì mở bộ lọc bấm từng ô.
 *
 * Thứ tự bóc rất quan trọng: "sao" trước, rồi "năm kinh nghiệm", rồi học phí, cuối cùng mới tới
 * giới tính — nếu không, "3 năm" sẽ bị hiểu nhầm thành giới tính "nam".
 */
const parseQuery = (raw: string): ParsedQuery => {
  let rest = ` ${normalize(raw)} `;
  const out: ParsedQuery = { text: '' };

  const eat = (re: RegExp, take: (m: RegExpMatchArray) => void) => {
    const m = rest.match(re);
    if (!m) return;
    take(m);
    rest = rest.replace(re, ' ');
  };

  // "4 sao", "tu 4.5 sao", "5*"
  eat(/(?:tu\s+)?(\d(?:[.,]\d)?)\s*(?:sao|\*)/, (m) => {
    out.minRating = m[1].replace(',', '.');
  });

  // "3 nam", "tren 5 nam kinh nghiem"
  eat(/(?:tren|tu)?\s*(\d{1,2})\s*nam(?:\s*kinh\s*nghiem)?\b/, (m) => {
    out.minExperience = m[1];
  });

  // "duoi 200k", "toi da 150 nghin", "200000d" — phải có mốc "dưới/tối đa" hoặc đơn vị tiền,
  // nếu không một con số trần trụi sẽ bị nuốt oan.
  const money = (digits: string, unit?: string) => {
    const n = Number(digits.replace(/[.,]/g, ''));
    if (!Number.isFinite(n) || n <= 0) return '';
    const u = unit ?? '';
    if (u === 'k' || u.startsWith('ngh') || u.startsWith('ng')) return String(n * 1000);
    if (u === 'tr' || u.startsWith('tri')) return String(n * 1_000_000);
    return String(n);
  };
  eat(
    /(?:duoi|toi\s*da|khong\s*qua|<=?)\s*(\d[\d.,]*)\s*(k|nghin|ngan|tr|trieu|d|dong|vnd)?\b/,
    (m) => {
      const value = money(m[1], m[2]);
      if (value) out.maxPrice = value;
    },
  );
  if (out.maxPrice === undefined) {
    eat(/(\d[\d.,]*)\s*(k|nghin|ngan|tr|trieu|dong|vnd)\b/, (m) => {
      const value = money(m[1], m[2]);
      if (value) out.maxPrice = value;
    });
  }

  eat(/\b(?:da\s*)?xac\s*minh\b/, () => {
    out.verifiedOnly = true;
  });

  eat(/\b(?:gioi\s*tinh\s*)?(nam|nu|khac)(?:\s*gioi)?\b/, (m) => {
    out.gender = GENDER_WORDS[m[1]];
  });

  out.text = rest.replace(/\s+/g, ' ').trim();
  return out;
};

export default function FindTutorPage() {
  const { status, data, reload } = useHome();
  const { isAuthenticated } = useAuth();
  const searchInputRef = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState<Filters>(EMPTY_FILTERS); // đang chỉnh trên form
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS); // đã bấm "Tìm"
  const [page, setPage] = useState(1);

  const tutors = data?.featuredTutors ?? [];

  const rankedTutors = useMemo(() => {
    const q = normalize(applied.keyword);
    const maxPrice = toNumber(applied.maxPrice);
    const minExp = toNumber(applied.minExperience);
    const minRating = toNumber(applied.minRating);

    const matched = tutors.filter((tutor) => {
      if (q) {
        // Tìm theo họ tên, mở rộng sang mô tả để không bỏ sót gia sư phù hợp.
        const hit =
          normalize(tutor.fullName).includes(q) || normalize(tutor.bio ?? '').includes(q);
        if (!hit) return false;
      }
      if (applied.queryGender) {
        // Khớp lỏng: "nam" vừa có thể là giới tính, vừa là tên người (Hoàng Nam...).
        const word = GENDER_LABELS[applied.queryGender];
        const genderHit = (tutor.gender ?? '').toUpperCase() === applied.queryGender;
        const nameHit = normalize(tutor.fullName).includes(word);
        if (!genderHit && !nameHit) return false;
      }
      if (maxPrice && (tutor.hourlyRate || 0) > maxPrice) return false;
      if (minExp && (tutor.experienceYears || 0) < minExp) return false;
      if (minRating && (tutor.ratingAvg || 0) < minRating) return false;
      if (applied.verifiedOnly && tutor.verificationStatus !== 'VERIFIED') return false;
      return true;
    });

    // Tìm kiếm chỉ có khớp / không khớp: đã lọt qua bộ lọc là khớp, không chấm điểm phần trăm.
    return matched;
  }, [tutors, applied]);

  const totalPages = Math.max(1, Math.ceil(rankedTutors.length / PAGE_SIZE));

  // Quay về trang 1 khi bộ lọc (số kết quả) thay đổi; kẹp page trong khoảng hợp lệ.
  useEffect(() => {
    setPage(1);
  }, [applied]);

  const currentPage = Math.min(page, totalPages);
  const pagedTutors = rankedTutors.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const isFiltered =
    applied.keyword !== '' ||
    applied.queryGender !== '' ||
    applied.maxPrice !== '' ||
    applied.minExperience !== '' ||
    applied.minRating !== '' ||
    applied.verifiedOnly;

  const patchDraft = (patch: Partial<Filters>) => setDraft((prev) => ({ ...prev, ...patch }));

  /**
   * Bóc tiêu chí ra khỏi câu tìm kiếm. Câu tìm kiếm giờ là nguồn DUY NHẤT của mọi tiêu chí
   * (không còn bảng lọc), nên mỗi lần tìm phải thay thế trọn bộ — giữ lại tiêu chí của lần
   * trước sẽ khiến chúng dính vĩnh viễn mà không có chỗ nào gỡ ra.
   */
  const runSearch = (source: Filters) => {
    const raw = source.keyword.trim();
    const parsed = parseQuery(raw);
    // Không bóc được tiêu chí nào -> dùng lại đúng chữ người dùng gõ (còn dấu) để khớp tên,
    // đừng dùng bản đã bỏ dấu của bộ phân tích.
    const untouched = parsed.text === normalize(raw);
    const next: Filters = {
      keyword: untouched ? raw : parsed.text,
      queryGender: parsed.gender ?? '',
      maxPrice: parsed.maxPrice ?? '',
      minExperience: parsed.minExperience ?? '',
      minRating: parsed.minRating ?? '',
      verifiedOnly: parsed.verifiedOnly ?? false,
    };
    // Ô nhập luôn giữ NGUYÊN câu người dùng gõ. Chỉ bộ tiêu chí bên dưới mới dùng phần chữ
    // còn lại sau khi bóc — gõ "nữ" mà ô bị xóa trắng thì trông như tìm hụt.
    setDraft({ ...next, keyword: source.keyword });
    setApplied(next);
  };

  return (
    <div className="tcs-page">
      <SiteHeader active="find-tutor" />
      <main>
        <section id="tutor-list" className="tcs-section tcs-section--tutors">
          <div className="tcs-container">
            <div className="tcs-find-topbar">
              <Link className="tcs-find-myreq tcs-find-myreq--lg" to={APP_ROUTES.postTutorRequest}>
                Đăng yêu cầu tìm gia sư
              </Link>
            </div>

            <form
              className="tcs-find-search"
              onSubmit={(event) => {
                event.preventDefault();
                runSearch(draft);
              }}
            >
              <div className="tcs-find-search__bar">
                <div className="tcs-find-search__field">
                  <input
                    type="search"
                    className="tcs-find-search__input"
                    placeholder="VD: nữ dưới 200k trên 3 năm từ 4 sao — hoặc gõ tên gia sư"
                    value={draft.keyword}
                    onChange={(event) => {
                      const value = event.target.value;
                      patchDraft({ keyword: value });
                      // Xóa trắng ô -> gỡ toàn bộ tiêu chí, vì câu tìm kiếm là nguồn duy nhất.
                      if (value === '') setApplied(EMPTY_FILTERS);
                    }}
                    aria-label="Tìm kiếm gia sư"
                    ref={searchInputRef}
                  />
                  {/* Nút xóa của riêng trang — nút mặc định của trình duyệt (type="search") mỗi
                      trình một kiểu và không canh được theo ô, nên ẩn đi ở CSS. */}
                  {draft.keyword ? (
                    <button
                      type="button"
                      className="tcs-find-search__clear"
                      aria-label="Xóa nội dung tìm kiếm"
                      onClick={() => {
                        patchDraft({ keyword: '' });
                        setApplied(EMPTY_FILTERS);
                        searchInputRef.current?.focus();
                      }}
                    >
                      <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
                        <path
                          d="M5 5 L19 19 M19 5 L5 19"
                          stroke="currentColor"
                          strokeWidth="2.6"
                          strokeLinecap="round"
                        />
                      </svg>
                    </button>
                  ) : null}
                </div>
                <button type="submit" className="tcs-find-search__btn">
                  Tìm
                </button>
              </div>

              <div className="tcs-find-examples">
                <span className="tcs-find-examples__label">Gõ thẳng tiêu chí, ví dụ:</span>
                {SEARCH_EXAMPLES.map((example) => (
                  <span key={example} className="tcs-find-examples__item">
                    {example}
                  </span>
                ))}
              </div>

            </form>

            <div className="tcs-section-bar tcs-find-listbar">
              <div>
                <h2 className="tcs-section-bar__title tcs-find-listbar__title">Danh sách gia sư</h2>
                <p className="tcs-section-bar__subtitle">
                  Tham khảo các gia sư tiêu biểu trên nền tảng và xem chi tiết hồ sơ.
                </p>
              </div>
              {status === 'success' && tutors.length > 0 ? (
                <span className="tcs-section-bar__count">
                  {isFiltered ? `${rankedTutors.length}/${tutors.length}` : tutors.length} gia sư
                </span>
              ) : null}
            </div>

            {status === 'loading' && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách gia sư...
              </div>
            )}

            {status === 'error' && (
              <div className="tcs-search-results__state tcs-search-results__state--error">
                Không thể tải danh sách gia sư.
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                  onClick={reload}
                >
                  Thử lại
                </button>
              </div>
            )}

            {status === 'success' && tutors.length === 0 && (
              <p className="tcs-empty">Chưa có gia sư nào để hiển thị.</p>
            )}

            {status === 'success' && tutors.length > 0 && rankedTutors.length === 0 && (
              <p className="tcs-empty">Không tìm thấy gia sư phù hợp với bộ lọc hiện tại.</p>
            )}

            {status === 'success' && rankedTutors.length > 0 && (
              <>
                <div className="tcs-listing-grid tcs-listing-grid--3col">
                  {pagedTutors.map((tutor) => (
                    <TutorListingCard
                      key={tutor.id}
                      tutor={tutor}
                      isAuthenticated={isAuthenticated}
                    />
                  ))}
                </div>

                {totalPages > 1 && (
                  <nav className="tcs-pagination" aria-label="Phân trang gia sư">
                    <button
                      type="button"
                      className="tcs-pagination__nav"
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage === 1}
                    >
                      ← Trước
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                      <button
                        key={p}
                        type="button"
                        className={`tcs-pagination__page${p === currentPage ? ' tcs-pagination__page--active' : ''}`}
                        onClick={() => setPage(p)}
                        aria-current={p === currentPage ? 'page' : undefined}
                      >
                        {p}
                      </button>
                    ))}
                    <button
                      type="button"
                      className="tcs-pagination__nav"
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage === totalPages}
                    >
                      Sau →
                    </button>
                  </nav>
                )}
              </>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
