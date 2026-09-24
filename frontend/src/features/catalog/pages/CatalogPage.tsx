/**
 * ====================================================================================================
 * [UC-09] MÀN HÌNH GIAO DIỆN CATALOGPAGE
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Hiển thị và điều phối các chức năng nghiệp vụ của phân hệ CatalogPage.
 * 2. Đảm bảo trải nghiệm người dùng tối ưu và đồng bộ dữ liệu với hệ thống Backend.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 * @author Nguyễn Trung Kiên (Kiennt1152)
 */
import { AdminLayout } from '../../platform/components/AdminLayout';
import { CatalogPanel } from '../components/CatalogPanel';
import './CatalogPage.css';

export default function CatalogPage() {
  return (
    <AdminLayout
      title="Quản lý danh mục"
      subtitle="Thiết lập danh mục dùng chung cho môn học, khu vực, cấp học và các lựa chọn hệ thống."
    >
      <CatalogPanel />
    </AdminLayout>
  );
}
