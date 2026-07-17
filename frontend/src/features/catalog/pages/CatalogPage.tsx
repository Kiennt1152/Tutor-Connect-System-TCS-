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
