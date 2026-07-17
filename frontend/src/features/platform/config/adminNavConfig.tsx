import { APP_ROUTES } from '../../../shared/constants/routes';
import type { AdminIconKey } from '../components/AdminIcons';
import type { PlatformDashboard } from '../types/platformTypes';

export type AdminNavItem = {
  to: string;
  label: string;
  icon: AdminIconKey;
  end?: boolean;
};

export type AdminNavGroup = {
  label: string;
  items: AdminNavItem[];
};

export type AdminModule = {
  icon: AdminIconKey;
  title: string;
  description: string;
  to: string;
  label?: string;
};

export type AdminStatConfig = {
  key: keyof PlatformDashboard;
  label: string;
  tone: 'default' | 'primary' | 'warn';
  icon: AdminIconKey;
  to?: string;
};

export const ADMIN_NAV_GROUPS: AdminNavGroup[] = [
  {
    label: 'Tổng quan',
    items: [
      {
        to: APP_ROUTES.platform,
        label: 'Bảng điều khiển',
        icon: 'dashboard',
        end: true,
      },
    ],
  },
  {
    label: 'Vận hành',
    items: [
      { to: APP_ROUTES.platformUsers, label: 'Người dùng', icon: 'users' },
      { to: APP_ROUTES.catalog, label: 'Danh mục', icon: 'folder' },
      { to: APP_ROUTES.platformVerifications, label: 'Xác minh', icon: 'shield' },
      { to: APP_ROUTES.platformReports, label: 'Báo cáo & tranh chấp', icon: 'flag' },
      { to: APP_ROUTES.platformEscrows, label: 'Escrow', icon: 'wallet' },
    ],
  },
];

export const ADMIN_HOME_NAV: Pick<AdminNavItem, 'label' | 'to'>[] = [
  { label: 'Bảng điều khiển', to: APP_ROUTES.platform },
  { label: 'Người dùng', to: APP_ROUTES.platformUsers },
  { label: 'Danh mục', to: APP_ROUTES.catalog },
  { label: 'Xác minh', to: APP_ROUTES.platformVerifications },
  { label: 'Báo cáo & tranh chấp', to: APP_ROUTES.platformReports },
  { label: 'Escrow', to: APP_ROUTES.platformEscrows },
];

export const ADMIN_QUICK_ACTIONS: AdminModule[] = [
  {
    icon: 'dashboard',
    title: 'Bảng điều khiển',
    description: 'Theo dõi số liệu và công việc cần xử lý.',
    to: APP_ROUTES.platform,
  },
  {
    icon: 'users',
    title: 'Quản lý người dùng',
    description: 'Xem danh sách và cập nhật trạng thái tài khoản.',
    to: APP_ROUTES.platformUsers,
  },
  {
    icon: 'folder',
    title: 'Quản lý danh mục',
    description: 'Cấu hình danh mục môn học, khu vực, cấp học và tùy chọn hệ thống.',
    to: APP_ROUTES.catalog,
  },
  {
    icon: 'shield',
    title: 'Duyệt xác minh',
    description: 'Phê duyệt hồ sơ gia sư và trung tâm.',
    to: APP_ROUTES.platformVerifications,
  },
  {
    icon: 'flag',
    title: 'Báo cáo & tranh chấp',
    description: 'Theo dõi báo cáo vi phạm, tranh chấp lớp học và bằng chứng.',
    to: APP_ROUTES.platformReports,
  },
  {
    icon: 'wallet',
    title: 'Giải ngân escrow',
    description: 'Thực hiện giải ngân escrow cho lớp đã đủ điều kiện tất toán.',
    to: APP_ROUTES.platformEscrows,
  },
];

export const ADMIN_OPERATION_MODULES: AdminModule[] = ADMIN_QUICK_ACTIONS.filter(
  (item) => item.icon !== 'dashboard',
);

export const ADMIN_STATS: AdminStatConfig[] = [
  {
    key: 'totalUsers',
    label: 'Tổng người dùng',
    tone: 'default',
    icon: 'users',
    to: APP_ROUTES.platformUsers,
  },
  {
    key: 'totalTutors',
    label: 'Gia sư',
    tone: 'primary',
    icon: 'graduation',
    to: APP_ROUTES.platformUsers,
  },
  {
    key: 'totalClasses',
    label: 'Lớp học',
    tone: 'default',
    icon: 'book',
  },
  {
    key: 'pendingVerifications',
    label: 'Xác minh chờ duyệt',
    tone: 'warn',
    icon: 'shield',
    to: APP_ROUTES.platformVerifications,
  },
  {
    key: 'openReports',
    label: 'Báo cáo đang mở',
    tone: 'warn',
    icon: 'flag',
    to: APP_ROUTES.platformReports,
  },
];

export const ADMIN_PRIORITY_ITEMS = [
  {
    key: 'pendingVerifications' as const,
    label: 'Xác minh chờ duyệt',
    icon: 'shield' as const,
    to: APP_ROUTES.platformVerifications,
  },
  {
    key: 'openReports' as const,
    label: 'Báo cáo đang mở',
    icon: 'flag' as const,
    to: APP_ROUTES.platformReports,
  },
];
