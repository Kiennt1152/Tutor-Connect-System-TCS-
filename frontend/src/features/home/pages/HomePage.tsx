import { useEffect, useMemo, useState } from 'react';
import axiosClient from '../../../shared/api/axiosClient';
import type { FeaturedTutor, HomeData, SubjectItem } from '../types';
import './HomePage.css';

type Status = 'loading' | 'success' | 'error';

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');

function Header() {
  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <a className="tcs-logo" href="/">
          <span className="tcs-logo__mark">TC</span>
          <span className="tcs-logo__text">Tutor Connect</span>
        </a>
        <nav className="tcs-header__nav">
          <a href="#subjects">Môn học</a>
          <a href="#tutors">Gia sư</a>
          <a href="#how">Cách hoạt động</a>
        </nav>
        <div className="tcs-header__actions">
          <a className="tcs-btn tcs-btn--ghost" href="/login">
            Đăng nhập
          </a>
          <a className="tcs-btn tcs-btn--primary" href="/register">
            Đăng ký
          </a>
        </div>
      </div>
    </header>
  );
}

function Hero({ data }: { data: HomeData | null }) {
  return (
    <section className="tcs-hero">
      <div className="tcs-container tcs-hero__inner">
        <div className="tcs-hero__copy">
          <span className="tcs-badge tcs-badge--info">Nền tảng kết nối gia sư uy tín</span>
          <h1 className="tcs-hero__title">
            Tìm gia sư phù hợp,
            <br />
            học tập hiệu quả hơn
          </h1>
          <p className="tcs-hero__subtitle">
            Kết nối Học viên, Gia sư và Trung tâm gia sư với quy trình minh bạch, thanh toán an toàn
            qua ký quỹ và xác minh rõ ràng.
          </p>

          <form
            className="tcs-search"
            onSubmit={(event) => {
              event.preventDefault();
            }}
          >
            <input className="tcs-search__field" placeholder="Bạn muốn học môn gì?" aria-label="Môn học" />
            <input className="tcs-search__field" placeholder="Khu vực" aria-label="Khu vực" />
            <button className="tcs-btn tcs-btn--primary tcs-search__btn" type="submit">
              Tìm kiếm
            </button>
          </form>

          <div className="tcs-hero__stats">
            <div className="tcs-stat">
              <span className="tcs-stat__value">{data ? currency(data.totalTutors) : '—'}</span>
              <span className="tcs-stat__label">Gia sư</span>
            </div>
            <div className="tcs-stat">
              <span className="tcs-stat__value">{data ? currency(data.totalSubjects) : '—'}</span>
              <span className="tcs-stat__label">Môn học</span>
            </div>
            <div className="tcs-stat">
              <span className="tcs-stat__value">{data ? currency(data.totalClasses) : '—'}</span>
              <span className="tcs-stat__label">Lớp học</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function SubjectsSection({ subjects }: { subjects: SubjectItem[] }) {
  return (
    <section id="subjects" className="tcs-section">
      <div className="tcs-container">
        <div className="tcs-section__head">
          <h2 className="tcs-section__title">Môn học phổ biến</h2>
          <p className="tcs-section__subtitle">Chọn môn học để bắt đầu tìm gia sư phù hợp.</p>
        </div>
        {subjects.length === 0 ? (
          <p className="tcs-empty">Chưa có môn học nào.</p>
        ) : (
          <div className="tcs-chips">
            {subjects.map((subject) => (
              <span key={subject.id} className="tcs-chip">
                {subject.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function TutorCard({ tutor }: { tutor: FeaturedTutor }) {
  return (
    <article className="tcs-tutor">
      <div className="tcs-tutor__head">
        <div className="tcs-avatar">{initials(tutor.fullName) || 'GS'}</div>
        <div>
          <h3 className="tcs-tutor__name">{tutor.fullName}</h3>
          <div className="tcs-tutor__rating">
            <span className="tcs-star">★</span>
            {Number(tutor.ratingAvg).toFixed(1)}
            <span className="tcs-tutor__exp">· {tutor.experienceYears} năm KN</span>
          </div>
        </div>
      </div>
      <p className="tcs-tutor__bio">{tutor.bio?.trim() || 'Gia sư tận tâm, sẵn sàng đồng hành cùng học viên.'}</p>
      <div className="tcs-tutor__foot">
        <span className="tcs-tutor__price">{currency(tutor.hourlyRate)} đ/giờ</span>
        <a className="tcs-btn tcs-btn--soft" href="/login">
          Xem hồ sơ
        </a>
      </div>
    </article>
  );
}

function TutorsSection({ tutors }: { tutors: FeaturedTutor[] }) {
  return (
    <section id="tutors" className="tcs-section tcs-section--alt">
      <div className="tcs-container">
        <div className="tcs-section__head">
          <h2 className="tcs-section__title">Gia sư nổi bật</h2>
          <p className="tcs-section__subtitle">Những gia sư được đánh giá cao trên nền tảng.</p>
        </div>
        {tutors.length === 0 ? (
          <p className="tcs-empty">Chưa có gia sư nổi bật.</p>
        ) : (
          <div className="tcs-grid tcs-grid--tutors">
            {tutors.map((tutor) => (
              <TutorCard key={tutor.id} tutor={tutor} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

const STEPS = [
  { title: 'Tìm kiếm', desc: 'Lọc gia sư theo môn học, khu vực, ngân sách và đánh giá.' },
  { title: 'Kết nối', desc: 'Đăng yêu cầu hoặc liên hệ trực tiếp với gia sư phù hợp.' },
  { title: 'Học & Thanh toán', desc: 'Thanh toán an toàn qua ký quỹ, giải ngân khi hoàn thành.' },
];

function HowItWorks() {
  return (
    <section id="how" className="tcs-section">
      <div className="tcs-container">
        <div className="tcs-section__head">
          <h2 className="tcs-section__title">Cách hoạt động</h2>
        </div>
        <div className="tcs-grid tcs-grid--steps">
          {STEPS.map((step, index) => (
            <div key={step.title} className="tcs-step">
              <span className="tcs-step__num">{index + 1}</span>
              <h3 className="tcs-step__title">{step.title}</h3>
              <p className="tcs-step__desc">{step.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="tcs-footer">
      <div className="tcs-container tcs-footer__inner">
        <span>© {new Date().getFullYear()} Tutor Connect System</span>
        <span className="tcs-footer__muted">SEP490 · TCS</span>
      </div>
    </footer>
  );
}

function LoadingState() {
  return (
    <div className="tcs-state">
      <div className="tcs-spinner" aria-hidden />
      <p>Đang tải dữ liệu trang chủ…</p>
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="tcs-state">
      <div className="tcs-state__icon tcs-state__icon--error">!</div>
      <p>Không kết nối được máy chủ. Hãy kiểm tra backend đang chạy ở cổng 8080.</p>
      <button className="tcs-btn tcs-btn--primary" onClick={onRetry}>
        Thử lại
      </button>
    </div>
  );
}

function HomePage() {
  const [status, setStatus] = useState<Status>('loading');
  const [data, setData] = useState<HomeData | null>(null);

  const load = () => {
    setStatus('loading');
    axiosClient
      .get<HomeData>('/home')
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        console.error('Lỗi tải trang chủ:', err);
        setStatus('error');
      });
  };

  useEffect(() => {
    load();
  }, []);

  const isEmpty = useMemo(
    () => status === 'success' && data !== null && data.featuredTutors.length === 0 && data.subjects.length === 0,
    [status, data],
  );

  return (
    <div className="tcs-page">
      <Header />
      <main>
        <Hero data={data} />

        {status === 'loading' && <LoadingState />}
        {status === 'error' && <ErrorState onRetry={load} />}

        {status === 'success' && data && (
          <>
            {isEmpty && (
              <div className="tcs-container">
                <p className="tcs-empty tcs-empty--page">
                  Chưa có dữ liệu để hiển thị. Hãy chạy seed data ở backend.
                </p>
              </div>
            )}
            <SubjectsSection subjects={data.subjects} />
            <TutorsSection tutors={data.featuredTutors} />
            <HowItWorks />
          </>
        )}
      </main>
      <Footer />
    </div>
  );
}

export default HomePage;
