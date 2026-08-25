import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { SiteHeader } from '../../home/components/SiteHeader';
import { SiteFooter } from '../../home/components/SiteFooter';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassResultCard } from '../components/ClassResultCard';
import { useClassSearch } from '../hooks/useClassSearch';
import { searchClasses, type TutorCriteria } from '../matching/tutorMatching';
import type { CatalogOption, ClassResponse } from '../types/marketplaceTypes';
import '../../home/pages/HomePage.css';
import '../../home/pages/FindTutorPage.css';
import './MarketplacePage.css';
import './ClassFinderPage.css';

/** Cùng cỡ trang với màn gia sư: 6 lớp / trang (2 cột × 3 hàng). */
const PAGE_SIZE = 6;

export default function ClassFinderPage() {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [grades, setGrades] = useState<CatalogOption[]>([]);
  const [provinces, setProvinces] = useState<CatalogOption[]>([]);
  /** { key: bộ tiêu chí đã sinh ra trang này, page: số trang }. */
  const [paged, setPaged] = useState<{ key: TutorCriteria | null; page: number }>({
    key: null,
    page: 1,
  });

  useEffect(() => {
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
    marketplaceApi.listGrades().then(setGrades).catch(() => setGrades([]));
    marketplaceApi.listProvinces().then(setProvinces).catch(() => setProvinces([]));
  }, []);

  useEffect(() => {
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch(() => setStatus('error'));
  }, []);

  // Đúng thanh tìm của màn gia sư: ô gõ nhanh + 5 ô lọc + 5 thanh trượt ưu tiên.
  const search = useClassSearch({ subjects, grades, provinces, classes });
  const results = useMemo(
    () => searchClasses(classes, search.criteria),
    [classes, search.criteria],
  );

  // Kết quả đổi (tìm mới / lọc khác) -> quay về trang 1. Gắn số trang vào chính bộ tiêu chí
  // đã sinh ra nó: tiêu chí đổi thì số trang cũ hết hiệu lực ngay trong lần render đó, không
  // phải đợi một nhịp effect mới nhảy về đầu.
  const page = paged.key === search.criteria ? paged.page : 1;
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const pageResults = results.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main>
        <section className="tcs-home-hero tcs-find-hero">
          <div className="tcs-container">
            <div className="tcs-find-hero__intro tcs-find-hero__intro--left">
              <Link className="tcs-find-back" to="/">← Trang chủ</Link>
              <h1 className="tcs-find-title tcs-find-title--left">
                <span className="tcs-find-title__text tcs-find-title__text--plain">
                  Tìm lớp phù hợp với bạn
                </span>
              </h1>
              <p className="tcs-find-subtitle tcs-find-subtitle--sm">
                Cho biết bạn cần học môn gì, ở đâu, mức học phí bao nhiêu — hệ thống tự chấm
                điểm và xếp hạng các lớp đang mở để bạn dễ chọn lớp đăng ký.
              </p>
            </div>

            <div className="tfc-container">
              <div className="tfc">
                {search.bar}

                <section className="tfc-results" id="tfc-results">
                  <header className="tfc-results__head">
                    <h2>{search.hasFilter ? 'Lớp phù hợp với bạn' : 'Tất cả lớp đang mở'}</h2>
                    <span className="tfc-results__count">
                      {status === 'success'
                        ? search.hasFilter && search.selectedCount > 0
                          ? `${results.length} lớp môn: ${search.subjectNames}`
                          : `${results.length} lớp đang mở`
                        : ''}
                    </span>
                  </header>

                  {status === 'loading' && (
                    <div className="tfc-state">Đang tải danh sách lớp…</div>
                  )}
                  {status === 'error' && (
                    <div className="tfc-state tfc-state--error">
                      Không tải được danh sách lớp.
                    </div>
                  )}
                  {status === 'success' && results.length === 0 && (
                    <div className="tfc-state">
                      {search.hasFilter && search.selectedCount > 0
                        ? `Chưa có lớp nào môn: ${search.subjectNames}. Thử môn khác nhé.`
                        : 'Hiện chưa có lớp nào đang mở đăng ký.'}
                    </div>
                  )}

                  <div className="tfc-list">
                    {pageResults.map((r) => (
                      <ClassResultCard
                        key={r.parsed.raw.classId}
                        result={r}
                        subjectName={search.subjectName}
                        gradeName={search.gradeName}
                        showScore={search.hasFilter}
                        actions={
                          <button
                            type="button"
                            className="tfc-btn tfc-btn--primary"
                            onClick={() => navigate(`/marketplace/classes/${r.parsed.raw.classId}`)}
                          >
                            Xem chi tiết
                          </button>
                        }
                      />
                    ))}
                  </div>

                  {pageCount > 1 && (
                    <nav className="tfc-pager" aria-label="Phân trang danh sách lớp">
                      <button
                        type="button"
                        className="tfc-pager__btn"
                        disabled={safePage <= 1}
                        onClick={() => setPaged({ key: search.criteria, page: Math.max(1, safePage - 1) })}
                      >
                        ← Trước
                      </button>
                      {Array.from({ length: pageCount }, (_, i) => i + 1).map((n) => (
                        <button
                          key={n}
                          type="button"
                          className={`tfc-pager__num${n === safePage ? ' is-active' : ''}`}
                          aria-current={n === safePage ? 'page' : undefined}
                          onClick={() => setPaged({ key: search.criteria, page: n })}
                        >
                          {n}
                        </button>
                      ))}
                      <button
                        type="button"
                        className="tfc-pager__btn"
                        disabled={safePage >= pageCount}
                        onClick={() => setPaged({ key: search.criteria, page: Math.min(pageCount, safePage + 1) })}
                      >
                        Sau →
                      </button>
                    </nav>
                  )}
                </section>
              </div>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
