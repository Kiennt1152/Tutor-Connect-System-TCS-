import { useEffect, useMemo, useState } from 'react';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import { OpenClassBoardCard } from '../../marketplace/components/OpenClassBoardCard';
import type { CatalogOption, ClassResponse } from '../../marketplace/types/marketplaceTypes';
import './HomePage.css';
import './FindTutorPage.css';
import '../../marketplace/pages/MarketplacePage.css';

type Status = 'loading' | 'success' | 'error';

const PAGE_SIZE = 6;

/**
 * Danh sách lớp (/danh-sach-tin-da-dang): tổng hợp tin do client đăng.
 * Tin chỉ được gỡ khỏi danh sách khi hai bên ĐÃ KÝ XONG hợp đồng VÀ đã chuyển khoản tiền cọc
 * (học phí tháng đầu) vào escrow; trước đó lớp vẫn hiển thị dù đã chọn được gia sư.
 * Dùng lại thẻ lớp giống màn "Yêu cầu của tôi".
 */
export default function ClassBoardPage() {
  const [status, setStatus] = useState<Status>('loading');
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [page, setPage] = useState(1);

  const reload = () => {
    setStatus('loading');
    marketplaceApi
      .listBoardClasses()
      .then((data) => {
        setClasses(data);
        setPage(1);
        setStatus('success');
      })
      .catch(() => setStatus('error'));
  };

  useEffect(() => {
    reload();
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
  }, []);

  const totalPages = Math.max(1, Math.ceil(classes.length / PAGE_SIZE));
  const pageClasses = useMemo(
    () => classes.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [classes, page],
  );

  const goToPage = (p: number) => {
    setPage(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="tcs-page tcs-classboard">
      <SiteHeader />
      <main>
        <section className="tcs-home-hero">
          <div className="tcs-container">
            <div className="tcs-hero__panel">
              <h1 className="tcs-find-title" style={{ marginBottom: 0 }}>
                <span className="tcs-find-title__text">Danh sách tin đã đăng</span>
                {status === 'success' && classes.length > 0 && (
                  <span className="cboard-count">{classes.length} tin</span>
                )}
              </h1>
            </div>
          </div>
        </section>

        <section className="tcs-section tcs-section--listing">
          <div className="tcs-container">
            {status === 'loading' && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách lớp...
              </div>
            )}
            {status === 'error' && (
              <div className="tcs-search-results__state tcs-search-results__state--error">
                Không thể tải danh sách lớp.
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                  onClick={reload}
                >
                  Thử lại
                </button>
              </div>
            )}
            {status === 'success' && classes.length === 0 && (
              <p className="tcs-empty">Hiện chưa có lớp học nào đang mở.</p>
            )}
            {status === 'success' && classes.length > 0 && (
              <>
                <div className="cboard-grid">
                  {pageClasses.map((c) => (
                    <OpenClassBoardCard key={c.classId} c={c} subjects={subjects} />
                  ))}
                </div>
                {totalPages > 1 && (
                  <nav className="mkt-pagination" aria-label="Phân trang danh sách lớp">
                    <button
                      type="button"
                      className="mkt-pagination__nav"
                      onClick={() => goToPage(Math.max(1, page - 1))}
                      disabled={page === 1}
                    >
                      ← Trước
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                      <button
                        key={p}
                        type="button"
                        className={`mkt-pagination__page${p === page ? ' mkt-pagination__page--active' : ''}`}
                        onClick={() => goToPage(p)}
                        aria-current={p === page ? 'page' : undefined}
                      >
                        {p}
                      </button>
                    ))}
                    <button
                      type="button"
                      className="mkt-pagination__nav"
                      onClick={() => goToPage(Math.min(totalPages, page + 1))}
                      disabled={page === totalPages}
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
