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
      { label: 'Đánh giá', href: APP_ROUTES.tutorReviews },
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
