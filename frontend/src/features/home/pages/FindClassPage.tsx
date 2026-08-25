import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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

/*
 * ============================================================================
 * MÀN "TÌM YÊU CẦU GIẢNG DẠY"  —  route /tim-yeu-cau-giang-day
 * ============================================================================
 *
 * MỘT ROUTE, HAI MÀN KHÁC HẲN NHAU
 * Đây là chỗ dễ nhầm nhất. Cùng một đường dẫn nhưng ai đăng nhập sẽ quyết định
 * render cái gì:
 *   • TUTOR / TUTOR_CENTER  → <TutorFindClassPage> — màn tìm lớp có chấm điểm
 *                              phù hợp, thanh trượt ưu tiên, nút "Ứng tuyển".
 *   • Vai khác / chưa login → <OpenClassListPage>  — chỉ là danh sách lớp trơn.
 * Nên khi sửa gì ở màn này, luôn tự hỏi: mình đang sửa nhánh nào.
 *
 * ĐIỀU QUAN TRỌNG NHẤT CẦN BIẾT
 * Backend KHÔNG chấm điểm và KHÔNG xếp hạng gì cả. Nó chỉ trả về danh sách lớp
 * đang mở (GET /marketplace/classes?status=OPEN). Toàn bộ việc chấm "80% phù
 * hợp" và sắp thứ tự chạy ngay trong trình duyệt, ở matching/tutorMatching.tsx.
 *
 * Hệ quả thực tế:
 *   - Sửa công thức chấm điểm thì KHÔNG cần restart backend, chỉ cần F5.
 *   - Nhưng trình duyệt phải tải về TẤT CẢ lớp đang mở rồi mới lọc. Hiện ~95 lớp
 *     nên chạy thoải mái; nếu sau này lên hàng chục nghìn lớp thì phải chuyển
 *     việc lọc/chấm điểm xuống backend.
 *
 * BA TỆP LÀM NÊN NHÁNH GIA SƯ (đọc theo thứ tự này là hiểu)
 *   1. TutorFindClass.tsx      — người điều phối: tải lớp, phân trang, mở modal.
 *   2. useClassSearch.tsx      — dựng cả khối tìm kiếm: ô gõ nhanh + bộ phân tích
 *                                câu tiếng Việt + 5 ô lọc + 5 thanh trượt ưu tiên.
 *                                Nó trả về `bar` (JSX) và `criteria` (tiêu chí đã
 *                                chốt) — chỉ đổi khi bấm Tìm, không đổi lúc gõ.
 *   3. tutorMatching.tsx       — thuật toán chấm điểm. Đọc chú thích trong đó.
 *
 * LUỒNG MỘT LẦN TÌM
 *   gõ câu → bấm Tìm → useClassSearch chốt `criteria`
 *          → searchClasses(classes, criteria) chấm điểm từng lớp, sắp giảm dần
 *          → cắt 6 lớp/trang → ClassResultCard vẽ ra
 *
 * NÚT "ỨNG TUYỂN"
 *   ApplyClassModal hiện hồ sơ gia sư (GET /profile/me) cho người dùng soát lại,
 *   nhập báo giá TỪNG MÔN, rồi POST /marketplace/classes/{id}/apply.
 *   Danh sách lớp đã nộp đơn lấy từ GET /marketplace/applications/mine để đổi nút
 *   thành "✓ Đã ứng tuyển".
 */

export default function FindClassPage() {
  const { user, isAuthenticated } = useAuth();
  const isTutor = hasAnyRole(user?.role, ['TUTOR', 'TUTOR_CENTER']);

  if (isTutor) {
    return <TutorFindClassPage />;
  }
  return <OpenClassListPage isAuthenticated={isAuthenticated} />;
}

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
            <div className="tcs-find-hero__intro tcs-find-hero__intro--left">
              <Link className="tcs-find-back" to="/">← Trang chủ</Link>
              <h1 className="tcs-find-title tcs-find-title--left">
                <span className="tcs-find-title__text tcs-find-title__text--plain">Tìm yêu cầu giảng dạy</span>
              </h1>
              <p className="tcs-find-subtitle tcs-find-subtitle--sm">
                Cho biết bạn muốn dạy môn gì, ở đâu, mức học phí bao nhiêu — hệ thống tự chấm điểm
                và xếp hạng các lớp đang mở để bạn dễ chọn lớp nhận dạy.
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
