import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axiosClient from '../../../shared/api/axiosClient';
import {
  ArrowRight,
  BadgeCheck,
  Bot,
  CalendarDays,
  ClipboardList,
  GraduationCap,
  Search,
  ShieldCheck,
  Star,
  Users,
  Wallet,
} from '../../../shared/components/icons';
import './HomePage.css';

type ConnState = 'loading' | 'ok' | 'error';

const features = [
  {
    icon: ShieldCheck,
    title: 'Gia sư đã xác minh',
    desc: 'Mọi gia sư đều được kiểm duyệt giấy tờ và bằng cấp trước khi nhận lớp.',
  },
  {
    icon: Search,
    title: 'Tìm kiếm thông minh',
    desc: 'Lọc theo môn học, khu vực, ngân sách và đánh giá để tìm gia sư phù hợp nhất.',
  },
  {
    icon: Wallet,
    title: 'Thanh toán ký quỹ',
    desc: 'Tiền học được giữ an toàn qua escrow và chỉ giải ngân khi buổi học hoàn tất.',
  },
  {
    icon: CalendarDays,
    title: 'Quản lý lớp học',
    desc: 'Theo dõi lịch học, điểm danh và tiến độ lớp một cách minh bạch, rõ ràng.',
  },
  {
    icon: Bot,
    title: 'Gợi ý bằng AI',
    desc: 'Trợ lý AI đề xuất gia sư và lớp học phù hợp, hỗ trợ giải đáp tức thì.',
  },
  {
    icon: Star,
    title: 'Đánh giá & uy tín',
    desc: 'Hệ thống đánh giá hai chiều giúp xây dựng uy tín và chất lượng dịch vụ.',
  },
];

const steps = [
  {
    icon: Search,
    title: 'Tìm hoặc đăng yêu cầu',
    desc: 'Tìm gia sư theo nhu cầu, hoặc đăng yêu cầu để gia sư chủ động ứng tuyển.',
  },
  {
    icon: ClipboardList,
    title: 'Kết nối & xác nhận',
    desc: 'Trao đổi, chọn gia sư phù hợp và xác nhận lớp học chỉ với vài thao tác.',
  },
  {
    icon: ShieldCheck,
    title: 'Học & thanh toán an toàn',
    desc: 'Học theo lịch, thanh toán qua ký quỹ và chỉ giải ngân khi bạn hài lòng.',
  },
];

const roles = [
  {
    icon: Users,
    title: 'Học viên / Phụ huynh',
    desc: 'Tìm gia sư phù hợp, đăng yêu cầu và theo dõi tiến độ học tập.',
  },
  {
    icon: GraduationCap,
    title: 'Gia sư',
    desc: 'Nhận lớp, quản lý lịch dạy và nhận thanh toán minh bạch, đúng hạn.',
  },
  {
    icon: BadgeCheck,
    title: 'Trung tâm gia sư',
    desc: 'Quản lý đội ngũ gia sư, tuyển dụng và theo dõi báo cáo tài chính.',
  },
];

function HomePage() {
  const [conn, setConn] = useState<ConnState>('loading');

  useEffect(() => {
    axiosClient
      .get('/hello')
      .then(() => setConn('ok'))
      .catch(() => setConn('error'));
  }, []);

  const connText =
    conn === 'loading'
      ? 'Đang kiểm tra kết nối hệ thống...'
      : conn === 'ok'
        ? 'Hệ thống đang hoạt động'
        : 'Không kết nối được máy chủ';

  return (
    <div className="home">
      {/* ---------- Hero ---------- */}
      <section className="hero">
        <div className="hero__inner">
          <span className={`status-pill status-pill--${conn}`}>
            <span className="status-dot" />
            {connText}
          </span>
          <h1 className="hero__title">
            Kết nối <span className="accent">gia sư phù hợp</span> cho hành trình học tập
            của bạn
          </h1>
          <p className="hero__subtitle">
            Tutor Connect System giúp học viên, phụ huynh và trung tâm tìm đúng gia sư đã
            được xác minh — minh bạch, an toàn và hiệu quả.
          </p>
          <div className="hero__actions">
            <Link to="/tutors" className="btn btn--light btn--lg">
              <Search size={20} />
              Tìm gia sư ngay
            </Link>
            <Link to="/register" className="btn btn--outline-light btn--lg">
              Trở thành gia sư
              <ArrowRight size={20} />
            </Link>
          </div>
          <div className="hero__stats">
            <div className="hero__stat">
              <strong>500+</strong>
              <span>Gia sư đã xác minh</span>
            </div>
            <div className="hero__stat">
              <strong>1.200+</strong>
              <span>Lớp học kết nối</span>
            </div>
            <div className="hero__stat">
              <strong>4.8/5</strong>
              <span>Đánh giá trung bình</span>
            </div>
          </div>
        </div>
      </section>

      {/* ---------- Features ---------- */}
      <section className="section">
        <div className="section__head">
          <h2>Vì sao chọn Tutor Connect?</h2>
          <p>Nền tảng được xây dựng quanh sự minh bạch, an toàn và trải nghiệm rõ ràng.</p>
        </div>
        <div className="grid grid--3">
          {features.map(({ icon: Icon, title, desc }) => (
            <article key={title} className="card">
              <span className="card__icon">
                <Icon size={24} />
              </span>
              <h3>{title}</h3>
              <p>{desc}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ---------- How it works ---------- */}
      <section className="section section--alt">
        <div className="section__head">
          <h2>Hoạt động như thế nào?</h2>
          <p>Chỉ ba bước đơn giản để bắt đầu việc dạy và học.</p>
        </div>
        <div className="grid grid--3 steps">
          {steps.map(({ icon: Icon, title, desc }, i) => (
            <article key={title} className="step">
              <span className="step__num">{i + 1}</span>
              <span className="card__icon">
                <Icon size={24} />
              </span>
              <h3>{title}</h3>
              <p>{desc}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ---------- Roles ---------- */}
      <section className="section">
        <div className="section__head">
          <h2>Dành cho mọi vai trò</h2>
          <p>Trải nghiệm nhất quán cho học viên, gia sư và trung tâm gia sư.</p>
        </div>
        <div className="grid grid--3">
          {roles.map(({ icon: Icon, title, desc }) => (
            <article key={title} className="card card--role">
              <span className="card__icon card__icon--soft">
                <Icon size={24} />
              </span>
              <h3>{title}</h3>
              <p>{desc}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ---------- CTA ---------- */}
      <section className="cta">
        <div className="cta__inner">
          <h2>Sẵn sàng bắt đầu?</h2>
          <p>Tạo tài khoản miễn phí và kết nối với gia sư phù hợp ngay hôm nay.</p>
          <div className="hero__actions">
            <Link to="/register" className="btn btn--light btn--lg">
              Đăng ký miễn phí
              <ArrowRight size={20} />
            </Link>
            <Link to="/login" className="btn btn--outline-light btn--lg">
              Đăng nhập
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

export default HomePage;
