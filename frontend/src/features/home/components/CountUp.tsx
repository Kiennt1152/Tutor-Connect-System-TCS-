import { useEffect, useRef, useState } from 'react';

const format = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

/**
 * Số đếm tăng dần từ 0 tới giá trị thật khi phần tử lọt vào khung nhìn.
 * Tôn trọng "giảm chuyển động": khi bật thì hiện thẳng số cuối.
 */
export function CountUp({ value, duration = 1200 }: { value: number | null; duration?: number }) {
  const ref = useRef<HTMLSpanElement | null>(null);
  const [shown, setShown] = useState(0);
  const started = useRef(false);

  useEffect(() => {
    const node = ref.current;
    if (node == null || value == null) return;

    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion || typeof IntersectionObserver === 'undefined') {
      setShown(value);
      return;
    }

    let frame = 0;
    const run = () => {
      if (started.current) return;
      started.current = true;
      const start = performance.now();
      const step = (now: number) => {
        const progress = Math.min((now - start) / duration, 1);
        // easeOutCubic: nhanh lúc đầu, chậm dần về cuối
        const eased = 1 - Math.pow(1 - progress, 3);
        setShown(Math.round(value * eased));
        if (progress < 1) frame = requestAnimationFrame(step);
      };
      frame = requestAnimationFrame(step);
    };

    // Đã nằm trong khung nhìn ngay từ đầu (hero luôn rơi vào trường hợp này) -> chạy luôn.
    const rect = node.getBoundingClientRect();
    if (rect.top < window.innerHeight && rect.bottom > 0) {
      run();
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          observer.unobserve(entry.target);
          run();
        });
      },
      { threshold: 0.4 },
    );
    observer.observe(node);

    // Dự phòng: nếu requestAnimationFrame không chạy (tab không dựng khung hình) thì
    // số sẽ kẹt ở 0 dù đã 'bắt đầu' — nên kiểm tra giá trị đang hiển thị, không kiểm tra cờ started.
    const failsafe = window.setTimeout(() => {
      setShown((current) => (current === value ? current : value));
    }, 2500);

    return () => {
      observer.disconnect();
      window.clearTimeout(failsafe);
      cancelAnimationFrame(frame);
    };
  }, [value, duration]);

  return <span ref={ref}>{value == null ? '—' : format(shown)}</span>;
}
