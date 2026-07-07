import { ADMIN_QUICK_ACTIONS } from '../../platform/config/adminNavConfig';
import type { AdminIconKey } from '../../platform/components/AdminIcons';

export type HomeQuickAction = {
  title: string;
  description: string;
  icon: string | AdminIconKey;
  to?: string;
  disabled?: boolean;
};

const ROLE_LABELS: Record<string, string> = {
  PLATFORM_ADMIN: 'Quản trị viên',
  CLIENT: 'Học viên / Phụ huynh',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm gia sư',
  UNKNOWN: 'Thành viên',
};

export function getRoleLabel(role: string) {
  return ROLE_LABELS[role] ?? ROLE_LABELS.UNKNOWN;
}

export function getHomeQuickActions(role: string): HomeQuickAction[] {
  switch (role) {
    case 'PLATFORM_ADMIN':
      return ADMIN_QUICK_ACTIONS.map((action) => ({
        title: action.title,
        description: action.description,
        icon: action.icon,
        to: action.to,
      }));
    case 'CLIENT':
      return [
        {
          title: 'Tìm gia sư',
          description: 'Khám phá gia sư nổi bật và môn học phổ biến.',
          icon: '🔍',
          to: '#find-tutor',
        },
        {
          title: 'Lớp học của tôi',
          description: 'Theo dõi lớp đã đăng ký và lịch học.',
          icon: '📚',
          disabled: true,
        },
        {
          title: 'Hồ sơ cá nhân',
          description: 'Cập nhật thông tin và cài đặt tài khoản.',
          icon: '👤',
          disabled: true,
        },
      ];
    case 'TUTOR':
      return [
        {
          title: 'Lớp đang dạy',
          description: 'Quản lý lớp học và học viên hiện tại.',
          icon: '📖',
          disabled: true,
        },
        {
          title: 'Lịch dạy',
          description: 'Xem và sắp xếp lịch dạy trong tuần.',
          icon: '📅',
          disabled: true,
        },
        {
          title: 'Hồ sơ gia sư',
          description: 'Chỉnh sửa kinh nghiệm, môn dạy và học phí.',
          icon: '👤',
          disabled: true,
        },
      ];
    case 'TUTOR_CENTER':
      return [
        {
          title: 'Quản lý gia sư',
          description: 'Danh sách gia sư thuộc trung tâm.',
          icon: '🏢',
          disabled: true,
        },
        {
          title: 'Tuyển dụng',
          description: 'Đăng tin và duyệt hồ sơ ứng tuyển.',
          icon: '📋',
          disabled: true,
        },
        {
          title: 'Hồ sơ trung tâm',
          description: 'Thông tin trung tâm và liên hệ.',
          icon: '👤',
          disabled: true,
        },
      ];
    default:
      return [
        {
          title: 'Hồ sơ cá nhân',
          description: 'Quản lý thông tin tài khoản của bạn.',
          icon: '👤',
          disabled: true,
        },
      ];
  }
}

export function getAuthenticatedHeroCopy(role: string) {
  switch (role) {
    case 'PLATFORM_ADMIN':
      return {
        eyebrow: 'Khu vực quản trị',
        title: 'Quản lý nền tảng Tutor Connect',
        subtitle:
          'Truy cập nhanh dashboard, người dùng và các công cụ vận hành hệ thống.',
      };
    case 'TUTOR':
      return {
        eyebrow: 'Không gian gia sư',
        title: 'Sẵn sàng cho buổi dạy tiếp theo',
        subtitle: 'Tìm lớp học phù hợp theo môn dạy và khu vực, ứng tuyển ngay trên nền tảng.',
      };
    case 'TUTOR_CENTER':
      return {
        eyebrow: 'Không gian trung tâm',
        title: 'Điều phối gia sư hiệu quả',
        subtitle: 'Quản lý đội ngũ gia sư và mở rộng hoạt động tuyển dụng.',
      };
    case 'CLIENT':
    default:
      return {
        eyebrow: 'Chào mừng trở lại',
        title: 'Tiếp tục hành trình học tập',
        subtitle: 'Tìm gia sư phù hợp, theo dõi lớp học và quản lý hồ sơ của bạn.',
      };
  }
}
