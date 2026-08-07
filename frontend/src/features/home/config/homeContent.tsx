export const HOME_TESTIMONIALS = [
  {
    quote:
      'Gia sư được giới thiệu rất nhiệt tình, con học có tiến bộ rõ rệt sau vài buổi. Quy trình trên nền tảng minh bạch.',
    author: 'PH Minh Anh',
    role: 'Phụ huynh học sinh lớp 9',
  },
  {
    quote:
      'Tôi nhận lớp nhanh và được hỗ trợ tư vấn tận tình. Thanh toán qua ký quỹ giúp cả hai bên yên tâm hơn.',
    author: 'Gia sư Thu Hà',
    role: 'Gia sư Toán · Hà Nội',
  },
  {
    quote:
      'Trung tâm dễ quản lý đội ngũ gia sư, theo dõi lớp học và doanh thu trên một màn hình.',
    author: 'TT Gia sư FPT',
    role: 'Trung tâm đối tác',
  },
];

export const HOME_NEWS = [
  {
    id: '1',
    title: 'Quy trình ký quỹ minh bạch trên Tutor Connect System',
    excerpt: 'Thanh toán an toàn cho học viên, gia sư và trung tâm qua hệ thống ký quỹ tích hợp.',
    date: '20/06/2026',
  },
  {
    id: '2',
    title: 'Mở rộng danh mục môn học phổ biến',
    excerpt: 'Cập nhật thêm Toán, Tiếng Anh, Vật lý và các môn THCS — THPT trên nền tảng.',
    date: '15/06/2026',
  },
  {
    id: '3',
    title: 'Hướng dẫn đăng ký gia sư và trung tâm đối tác',
    excerpt: 'Các bước xác minh hồ sơ, thiết lập tài khoản và bắt đầu kết nối lớp học.',
    date: '10/06/2026',
  },
];

import { APP_ROUTES } from '../../../shared/constants/routes';

export const FOOTER_LINKS = [
  {
    title: 'Khám phá',
    links: [
      { label: 'Tìm gia sư', href: APP_ROUTES.findTutor },
      { label: 'Tìm lớp', href: APP_ROUTES.classFinder },
      { label: 'Trung tâm', href: APP_ROUTES.centers },
    ],
  },
  {
    title: 'Cộng đồng',
    links: [
      // Trỏ về mục trên trang chủ kèm "/" để hoạt động từ mọi trang.
      { label: 'Tin tức', href: '/#news' },
      { label: 'Đánh giá', href: '/#reviews' },
      { label: 'Trợ giúp', href: APP_ROUTES.help },
    ],
  },
  {
    title: 'Tài khoản',
    links: [
      { label: 'Đăng nhập', href: APP_ROUTES.login },
      { label: 'Đăng ký', href: APP_ROUTES.register },
    ],
  },
];
