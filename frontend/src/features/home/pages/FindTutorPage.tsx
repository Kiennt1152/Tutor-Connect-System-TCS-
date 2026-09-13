import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import type { FeaturedTutor } from '../types/homeTypes';
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

type SortKey = 'match' | 'price-asc' | 'price-desc' | 'rating' | 'experience';

type Filters = {
  keyword: string;
  gender: '' | 'MALE' | 'FEMALE' | 'OTHER';
  maxPrice: string;
  minExperience: string;
  minRating: string;
  verifiedOnly: boolean;
  sort: SortKey;
};

const EMPTY_FILTERS: Filters = {
  keyword: '',
  gender: '',
  maxPrice: '',
  minExperience: '',
  minRating: '',
  verifiedOnly: false,
  sort: 'match',
};

const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: 'match', label: 'Phù hợp nhất (gợi ý)' },
  { value: 'price-asc', label: 'Học phí thấp → cao' },
  { value: 'price-desc', label: 'Học phí cao → thấp' },
  { value: 'rating', label: 'Đánh giá cao nhất' },
  { value: 'experience', label: 'Kinh nghiệm nhiều nhất' },
];

const toNumber = (value: string) => {
  const parsed = Number(value.replace(/[^\d.]/g, ''));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
};

/**
 * Matchmaking: chấm điểm 0-100 cho mỗi gia sư theo tiêu chí người dùng chọn.
 * Trọng số: từ khóa 30% (chỉ tính khi có nhập), kinh nghiệm 25%, đánh giá 20%,
 * học phí 15%, xác minh 10%. Tiêu chí nào không dùng thì trọng số được chuẩn hóa lại.
 */
const matchScoreOf = (tutor: FeaturedTutor, filters: Filters, maxRate: number) => {
  const parts: { weight: number; score: number }[] = [];

  const q = normalize(filters.keyword);
  if (q) {
    const name = normalize(tutor.fullName);
    const bio = normalize(tutor.bio ?? '');
    let keywordScore = 0;
    if (name === q) keywordScore = 1;
    else if (name.startsWith(q)) keywordScore = 0.92;
    else if (name.includes(q)) keywordScore = 0.82;
    else if (bio.includes(q)) keywordScore = 0.6;
    parts.push({ weight: 0.3, score: keywordScore });
  }

  // Kinh nghiệm: 8 năm trở lên coi như tối đa.
  parts.push({ weight: 0.25, score: Math.min(1, (tutor.experienceYears || 0) / 8) });

  // Đánh giá: thang 5 sao.
  parts.push({ weight: 0.2, score: Math.min(1, Math.max(0, tutor.ratingAvg || 0) / 5) });

  // Học phí: rẻ hơn so với mức trần mong muốn (hoặc so với mức cao nhất đang có) thì hợp hơn.
  const budget = toNumber(filters.maxPrice) || maxRate;
  if (budget > 0) {
    const rate = tutor.hourlyRate || 0;
    parts.push({ weight: 0.15, score: Math.min(1, Math.max(0, 1 - rate / budget)) });
  }

  parts.push({ weight: 0.1, score: tutor.verificationStatus === 'VERIFIED' ? 1 : 0.35 });

  const totalWeight = parts.reduce((sum, part) => sum + part.weight, 0);
  if (totalWeight <= 0) return 0;
  const raw = parts.reduce((sum, part) => sum + part.weight * part.score, 0) / totalWeight;
  return Math.round(raw * 100);
};

export default function FindTutorPage() {
  const { status, data, reload } = useHome();
  const { isAuthenticated } = useAuth();
  const [draft, setDraft] = useState<Filters>(EMPTY_FILTERS); // đang chỉnh trên form
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS); // đã bấm "Tìm"
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(1);

  const tutors = data?.featuredTutors ?? [];

  const maxRate = useMemo(
    () => tutors.reduce((max, tutor) => Math.max(max, tutor.hourlyRate || 0), 0),
    [tutors],
  );

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
      if (applied.gender && (tutor.gender ?? '').toUpperCase() !== applied.gender) return false;
      if (maxPrice && (tutor.hourlyRate || 0) > maxPrice) return false;
      if (minExp && (tutor.experienceYears || 0) < minExp) return false;
      if (minRating && (tutor.ratingAvg || 0) < minRating) return false;
      if (applied.verifiedOnly && tutor.verificationStatus !== 'VERIFIED') return false;
      return true;
    });

    const scored = matched.map((tutor) => ({
      tutor,
      score: matchScoreOf(tutor, applied, maxRate),
    }));

    const sorted = [...scored];
    switch (applied.sort) {
      case 'price-asc':
        sorted.sort((a, b) => (a.tutor.hourlyRate || 0) - (b.tutor.hourlyRate || 0));
        break;
      case 'price-desc':
        sorted.sort((a, b) => (b.tutor.hourlyRate || 0) - (a.tutor.hourlyRate || 0));
        break;
      case 'rating':
        sorted.sort((a, b) => (b.tutor.ratingAvg || 0) - (a.tutor.ratingAvg || 0));
        break;
      case 'experience':
        sorted.sort((a, b) => (b.tutor.experienceYears || 0) - (a.tutor.experienceYears || 0));
        break;
      default:
        sorted.sort((a, b) => b.score - a.score);
    }
    return sorted;
  }, [tutors, applied, maxRate]);

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
    applied.gender !== '' ||
    applied.maxPrice !== '' ||
    applied.minExperience !== '' ||
    applied.minRating !== '' ||
    applied.verifiedOnly;

  const activeFilterCount = [
    applied.gender,
    applied.maxPrice,
    applied.minExperience,
    applied.minRating,
    applied.verifiedOnly ? 'y' : '',
  ].filter(Boolean).length;

  const patchDraft = (patch: Partial<Filters>) => setDraft((prev) => ({ ...prev, ...patch }));

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
                setApplied({ ...draft, keyword: draft.keyword.trim() });
              }}
            >
              <div className="tcs-find-search__bar">
                <div className="tcs-find-search__field">
                  <input
                    type="search"
                    className="tcs-find-search__input"
                    placeholder="Tìm gia sư theo tên, môn học hoặc mô tả..."
                    value={draft.keyword}
                    onChange={(event) => {
                      const value = event.target.value;
                      patchDraft({ keyword: value });
                      // Bấm dấu ✕ mặc định của trình duyệt (làm rỗng ô) -> reset luôn kết quả lọc.
                      if (value === '') setApplied((prev) => ({ ...prev, keyword: '' }));
                    }}
                    aria-label="Tìm kiếm gia sư"
                  />
                </div>
                <button
                  type="button"
                  className={`tcs-find-filter__toggle${showFilters ? ' tcs-find-filter__toggle--open' : ''}`}
                  onClick={() => setShowFilters((open) => !open)}
                  aria-expanded={showFilters}
                >
                  ⚙ Bộ lọc
                  {activeFilterCount > 0 ? (
                    <span className="tcs-find-filter__count">{activeFilterCount}</span>
                  ) : null}
                </button>
                <button type="submit" className="tcs-find-search__btn">
                  Tìm
                </button>
              </div>

              {showFilters && (
                <div className="tcs-find-filter">
                  <div className="tcs-find-filter__grid">
                    <label className="tcs-find-filter__item">
                      <span className="tcs-find-filter__label">Giới tính</span>
                      <select
                        className="tcs-find-filter__control"
                        value={draft.gender}
                        onChange={(event) =>
                          patchDraft({ gender: event.target.value as Filters['gender'] })
                        }
                      >
                        <option value="">Tất cả</option>
                        <option value="MALE">Nam</option>
                        <option value="FEMALE">Nữ</option>
                        <option value="OTHER">Khác</option>
                      </select>
                    </label>

                    <label className="tcs-find-filter__item">
                      <span className="tcs-find-filter__label">Học phí tối đa (đ/giờ)</span>
                      <input
                        type="number"
                        min={0}
                        step={10000}
                        className="tcs-find-filter__control"
                        placeholder="VD: 200000"
                        value={draft.maxPrice}
                        onChange={(event) => patchDraft({ maxPrice: event.target.value })}
                      />
                    </label>

                    <label className="tcs-find-filter__item">
                      <span className="tcs-find-filter__label">Kinh nghiệm tối thiểu (năm)</span>
                      <input
                        type="number"
                        min={0}
                        step={1}
                        className="tcs-find-filter__control"
                        placeholder="VD: 3"
                        value={draft.minExperience}
                        onChange={(event) => patchDraft({ minExperience: event.target.value })}
                      />
                    </label>

                    <label className="tcs-find-filter__item">
                      <span className="tcs-find-filter__label">Đánh giá tối thiểu</span>
                      <select
                        className="tcs-find-filter__control"
                        value={draft.minRating}
                        onChange={(event) => patchDraft({ minRating: event.target.value })}
                      >
                        <option value="">Tất cả</option>
                        <option value="3">Từ 3 sao</option>
                        <option value="4">Từ 4 sao</option>
                        <option value="4.5">Từ 4.5 sao</option>
                      </select>
                    </label>

                    <label className="tcs-find-filter__item">
                      <span className="tcs-find-filter__label">Sắp xếp</span>
                      <select
                        className="tcs-find-filter__control"
                        value={draft.sort}
                        onChange={(event) =>
                          patchDraft({ sort: event.target.value as SortKey })
                        }
                      >
                        {SORT_OPTIONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="tcs-find-filter__item tcs-find-filter__item--check">
                      <input
                        type="checkbox"
                        checked={draft.verifiedOnly}
                        onChange={(event) => patchDraft({ verifiedOnly: event.target.checked })}
                      />
                      <span>Chỉ gia sư đã xác minh</span>
                    </label>
                  </div>

                  <div className="tcs-find-filter__foot">
                    <p className="tcs-find-filter__hint">
                      Chế độ <strong>Phù hợp nhất</strong> chấm điểm gia sư theo từ khóa (30%),
                      kinh nghiệm (25%), đánh giá (20%), học phí (15%) và trạng thái xác minh (10%).
                    </p>
                    <button
                      type="button"
                      className="tcs-find-filter__reset"
                      onClick={() => {
                        setDraft(EMPTY_FILTERS);
                        setApplied(EMPTY_FILTERS);
                      }}
                    >
                      Xóa bộ lọc
                    </button>
                  </div>
                </div>
              )}
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
                  {pagedTutors.map(({ tutor, score }) => (
                    <TutorListingCard
                      key={tutor.id}
                      tutor={tutor}
                      isAuthenticated={isAuthenticated}
                      matchScore={applied.sort === 'match' ? score : undefined}
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
