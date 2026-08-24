import { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { useTutorRequestForm } from '../hooks/useTutorRequestForm';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { ClassRequestForm } from '../../marketplace/components/ClassRequestForm';
import { emptyForm } from '../../marketplace/mappers/marketplaceMapper';
import type {
  ClassRequestPayload,
  ClassResponse,
} from '../../marketplace/types/marketplaceTypes';
import './HomePage.css';
import './FindTutorPage.css';

/*
 * ============================================================================
 * MÀN "ĐĂNG YÊU CẦU TÌM GIA SƯ"  —  route /dang-yeu-cau-tim-gia-su
 * ============================================================================
 *
 * TỆP NÀY LÀM GÌ
 * Chỉ là cái khung: tiêu đề, banner cảnh báo quyền, và màn hình báo thành công.
 * Toàn bộ ô nhập bạn thấy trên màn nằm ở <ClassRequestForm> — dùng chung với
 * màn "Chỉnh sửa lớp" trong MarketplacePage, nên sửa form là hai nơi cùng đổi.
 *
 * LUỒNG DỮ LIỆU (từ lúc mở trang đến lúc lưu xong)
 *
 *   1. MỞ TRANG
 *      useTutorRequestForm() gọi song song 3 API danh mục để đổ dữ liệu cho các
 *      ô chọn:  GET /catalog/subjects  (checkbox Môn học)
 *               GET /catalog/grades    (dropdown Lớp)
 *               GET /catalog/provinces (không dùng ở màn này — xem mục 2)
 *      API lỗi thì đặt về mảng rỗng, form vẫn mở được (riêng Lớp còn có
 *      FALLBACK_GRADES trong constants/catalogFallback.tsx đỡ lưng).
 *
 *   2. Ô TỈNH / PHƯỜNG
 *      Do <LocationPicker> lo, và nó KHÔNG gọi backend của mình mà gọi API công
 *      cộng provinces.open-api.vn. Vì vậy mất mạng ngoài là hai ô này trống dù
 *      backend vẫn chạy. Chỉ hiện khi chọn Offline.
 *
 *   3. NGƯỜI DÙNG ĐIỀN → BẤM "ĐĂNG YÊU CẦU"
 *      ClassRequestForm giữ toàn bộ state trong một object ClassFormValues, tự
 *      kiểm tra thiếu trường / lịch trùng giờ. Hợp lệ mới gọi formToPayload()
 *      để gói lại thành ClassRequestPayload rồi bắn ngược lên đây qua onSubmit.
 *
 *   4. handleSubmit() Ở DƯỚI
 *      Chặn trước nếu không phải tài khoản CLIENT, rồi gọi
 *      marketplaceApi.createClass()  →  POST /api/marketplace/classes
 *
 *   5. BACKEND
 *      MarketplaceController.createClass() (dòng 67) → MarketplaceServiceImpl
 *      .createClass(): kiểm tra role CLIENT + quyền CLASS_POSTING (tài khoản bị
 *      phạt sẽ bị chặn) → lưu 1 dòng vào bảng tutoring_classes → ghi audit_logs.
 *
 *   6. QUAN TRỌNG — TIN LƯU RA Ở TRẠNG THÁI **DRAFT (NHÁP)**
 *      Tức là gia sư CHƯA nhìn thấy. Muốn công khai phải sang trang "Yêu cầu của
 *      tôi" bấm "Đăng lớp" → POST /marketplace/classes/{id}/publish, lúc đó mới
 *      status = OPEN và đặt expires_at = now + 30 ngày. Đó là lý do màn thành
 *      công bên dưới ghi rõ "đang ở trạng thái nháp".
 *
 * DỮ LIỆU LƯU Ở ĐÂU
 *      Bảng tutoring_classes. Vài trường quen thuộc nằm ở cột riêng (title,
 *      grade_id, tuition_fee, start_date...), còn TẤT CẢ chi tiết của form —
 *      danh sách môn, học phí từng môn, từng khung giờ, mục tiêu, địa chỉ —
 *      được nén thành JSON và nhét vào MỘT cột: details_json.
 *      Muốn xem một tin trông ra sao thì đọc cột đó là đủ.
 */

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}

export default function PostTutorRequestPage() {
  const { user, isAuthenticated } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const { subjects, grades, createRequest } = useTutorRequestForm();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<ClassResponse | null>(null);
  const [formKey, setFormKey] = useState(0);

  async function handleSubmit(payload: ClassRequestPayload) {
    if (!isClient) {
      setError('Vui lòng đăng nhập bằng tài khoản Client (Phụ huynh/Học viên) để đăng yêu cầu.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const result = await createRequest(payload);
      setCreated(result);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSubmitting(false);
    }
  }

  function resetForm() {
    setCreated(null);
    setError(null);
    setFormKey((k) => k + 1);
  }

  return (
    <div className="tcs-page">
      <SiteHeader active="find-tutor" />
      <main>
        <section className="tcs-home-hero tcs-find-hero">
          <div className="tcs-container">
            <Link className="tcs-find-back tcs-find-back--btn" to={APP_ROUTES.findTutor}>
              ← Quay lại danh sách gia sư
            </Link>
            <div className="tcs-find-hero__intro">
              <h1 className="tcs-find-title tcs-find-title--compact">
                <span className="tcs-find-title__icon">🎓</span>
                <span className="tcs-find-title__text tcs-find-title__text--plain">Đăng yêu cầu tìm gia sư</span>
              </h1>
              <p className="tcs-find-subtitle tcs-find-subtitle--compact">
                Điền thông tin nhu cầu học tập của bạn — môn học, mục tiêu, hình thức, học phí. Gia sư
                phù hợp sẽ nhận được yêu cầu và liên hệ với bạn.
              </p>
            </div>

            <div className="tcs-find-form-card">
              {!isAuthenticated && (
                <div className="tcs-find-banner">
                  <span>
                    Bạn cần đăng nhập bằng tài khoản Client (Phụ huynh/Học viên) để đăng yêu cầu.
                  </span>
                  <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.login}>
                    Đăng nhập
                  </Link>
                </div>
              )}
              {isAuthenticated && !isClient && (
                <div className="tcs-find-banner tcs-find-banner--warn">
                  Chỉ tài khoản Client (Phụ huynh/Học viên) mới đăng được yêu cầu tìm gia sư.
                </div>
              )}

              {created ? (
                <div className="tcs-find-success">
                  <div className="tcs-find-success__icon">✓</div>
                  <h2>Đã gửi yêu cầu tìm gia sư!</h2>
                  <p>
                    Yêu cầu <strong>“{created.title}”</strong> đang ở trạng thái nháp. Vào trang
                    quản lý để đăng công khai cho gia sư ứng tuyển.
                  </p>
                  <div className="tcs-find-success__actions">
                    <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.marketplace}>
                      Quản lý yêu cầu
                    </Link>
                    <button type="button" className="tcs-btn tcs-btn--ghost" onClick={resetForm}>
                      Đăng yêu cầu khác
                    </button>
                  </div>
                </div>
              ) : (
                <ClassRequestForm
                  key={formKey}
                  initial={emptyForm()}
                  subjects={subjects}
                  grades={grades}
                  isEdit={false}
                  submitting={submitting}
                  error={error}
                  onSubmit={handleSubmit}
                  onCancel={resetForm}
                />
              )}
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
