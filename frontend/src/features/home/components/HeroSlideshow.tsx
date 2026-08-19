import { useEffect, useState } from 'react';

const SLIDES = [
  { src: '/images/hero-study-1.jpg', alt: 'Nhóm học viên học cùng nhau bên máy tính' },
  { src: '/images/hero-study-2.jpg', alt: 'Gia sư trao đổi bài với học viên' },
  { src: '/images/hero-study-3.jpg', alt: 'Học viên ghi chép trong lớp học' },
];

const INTERVAL_MS = 5000;

/**
 * Ảnh hero chuyển cảnh mềm: ảnh mờ dần vào nhau kèm phóng chậm (hiệu ứng Ken Burns).
 *
 * Ảnh đầu tải ngay (eager) vì nằm ngay đầu trang; các ảnh sau tải lười.
 * Nếu người dùng bật giảm chuyển động thì đứng yên ở ảnh đầu.
 */
export function HeroSlideshow() {
  const [active, setActive] = useState(0);

  useEffect(() => {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) return;

    const timer = window.setInterval(() => {
      setActive((current) => (current + 1) % SLIDES.length);
    }, INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, []);

  return (
    <div className="tcs-hero-media">
      <div className="tcs-hero-media__frame">
        {SLIDES.map((slide, index) => (
          <img
            key={slide.src}
            src={slide.src}
            alt={index === 0 ? slide.alt : ''}
            aria-hidden={index !== 0}
            /* Cả ba ảnh đều nằm trong hero ngay từ đầu. Để 'lazy' thì ảnh 2, 3 chưa kịp
               tải lúc chuyển cảnh -> khung trắng. Nên tải sớm cả ba, nhưng hạ độ ưu tiên
               của hai ảnh sau để không tranh băng thông với lần vẽ đầu tiên. */
            loading="eager"
            fetchPriority={index === 0 ? 'high' : 'low'}
            decoding="async"
            width={900}
            height={600}
            className={`tcs-hero-media__img${index === active ? ' is-active' : ''}`}
          />
        ))}

        {/* Phủ một lớp cam rất nhạt để ảnh hoà vào tông thương hiệu */}
        <span className="tcs-hero-media__tint" aria-hidden="true" />

        <div className="tcs-hero-media__dots" role="tablist" aria-label="Chọn ảnh">
          {SLIDES.map((slide, index) => (
            <button
              key={slide.src}
              type="button"
              role="tab"
              aria-selected={index === active}
              aria-label={`Ảnh ${index + 1}`}
              className={`tcs-hero-media__dot${index === active ? ' is-active' : ''}`}
              onClick={() => setActive(index)}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
