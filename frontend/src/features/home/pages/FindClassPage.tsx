import { useEffect, useState } from 'react';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasAnyRole } from '../../../shared/auth/rbac';
import { useOpenClasses } from '../hooks/useOpenClasses';
import { ClassesSection } from './HomePage';
import { TutorFindClass } from '../../marketplace/components/TutorFindClass';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type { CatalogOption } from '../../marketplace/types/marketplaceTypes';
import './HomePage.css';
import './FindTutorPage.css';
import '../../marketplace/pages/MarketplacePage.css';

/** Màn "Tìm lớp" độc lập (route /tim-lop). Gia sư → bản chấm điểm phù hợp;
 *  học viên/khách → danh sách lớp đang mở. */
export default function FindClassPage() {
  const { user, isAuthenticated } = useAuth();
  const isTutor = hasAnyRole(user?.role, ['TUTOR', 'TUTOR_CENTER']);

  if (isTutor) {
    return <TutorFindClassPage />;
  }
  return <OpenClassListPage isAuthenticated={isAuthenticated} />;
}

/** Bản dành cho gia sư: chấm điểm độ phù hợp theo trọng số. */
function TutorFindClassPage() {
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [grades, setGrades] = useState<CatalogOption[]>([]);
  const [provinces, setProvinces] = useState<CatalogOption[]>([]);

  useEffect(() => {
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
    marketplaceApi.listGrades().then(setGrades).catch(() => setGrades([]));
    marketplaceApi.listProvinces().then(setProvinces).catch(() => setProvinces([]));
  }, []);

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main>
        <section className="tcs-home-hero tcs-find-hero">
          <div className="tcs-container">
            <div className="tcs-find-hero__intro">
              <h1 className="tcs-find-title">
                <span className="tcs-find-title__icon">🎓</span>
                <span className="tcs-find-title__text">Tìm lớp phù hợp</span>
              </h1>
              <p className="tcs-find-subtitle">
                Khai báo môn dạy, khu vực, học phí và lịch rảnh — hệ thống chấm điểm và xếp hạng các
                lớp đang mở để bạn dễ chọn lớp nhận dạy.
              </p>
            </div>
            <div className="tfc-container">
              <TutorFindClass subjects={subjects} grades={grades} provinces={provinces} />
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}

/** Bản danh sách công khai: học viên đăng ký / khách xem lớp đang mở. */
function OpenClassListPage({ isAuthenticated }: { isAuthenticated: boolean }) {
  const { status, classes, reload } = useOpenClasses();
  return (
    <div className="tcs-page">
      <SiteHeader />
      <main>
        <ClassesSection
          classes={classes}
          status={status}
          isAuthenticated={isAuthenticated}
          onRetry={reload}
        />
      </main>
      <SiteFooter />
    </div>
  );
}
