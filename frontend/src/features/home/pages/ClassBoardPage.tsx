import { useEffect, useState } from 'react';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import { OpenClassBoardCard } from '../../marketplace/components/OpenClassBoardCard';
import type { CatalogOption, ClassResponse } from '../../marketplace/types/marketplaceTypes';
import './HomePage.css';
import './FindTutorPage.css';
import '../../marketplace/pages/MarketplacePage.css';

type Status = 'loading' | 'success' | 'error';

/**
 * Danh sách lớp (/danh-sach-lop): tổng hợp mọi lớp do client đăng và đang mở.
 * Khi hai bên ký thỏa thuận hợp đồng xong -> lớp được kích hoạt (rời trạng thái OPEN)
 * nên tự động biến mất khỏi danh sách này. Dùng lại thẻ lớp giống màn "Yêu cầu của tôi".
 */
export default function ClassBoardPage() {
  const [status, setStatus] = useState<Status>('loading');
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);

  const reload = () => {
    setStatus('loading');
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch(() => setStatus('error'));
  };

  useEffect(() => {
    reload();
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
  }, []);

  return (
    <div className="tcs-page tcs-classboard">
      <SiteHeader />
      <main>
        <section className="tcs-home-hero">
          <div className="tcs-container">
            <div className="tcs-hero__panel">
              <h1 className="tcs-find-title" style={{ marginBottom: 0 }}>
                <span className="tcs-find-title__icon">📋</span>
                <span className="tcs-find-title__text">Danh sách lớp đã đăng</span>
              </h1>
            </div>
          </div>
        </section>

        <section className="tcs-section tcs-section--listing">
          <div className="tcs-container">
            {status === 'success' && classes.length > 0 && (
              <div className="tcs-section-bar">
                <div />
                <span className="tcs-section-bar__count">{classes.length} lớp</span>
              </div>
            )}

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
              <div className="cboard-grid">
                {classes.map((c) => (
                  <OpenClassBoardCard key={c.classId} c={c} subjects={subjects} />
                ))}
              </div>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
