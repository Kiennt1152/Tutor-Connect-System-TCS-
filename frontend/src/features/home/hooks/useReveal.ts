import { useEffect } from 'react';

const IN_VIEW_MARGIN = 0.95; // coi như đã trong khung nhìn nếu mép trên nằm dưới 95% chiều cao
const IO_FAILSAFE_MS = 2500;
const LAST_RESORT_MS = 4000;

function isInViewport(el: HTMLElement) {
  const rect = el.getBoundingClientRect();
  return rect.top < window.innerHeight * IN_VIEW_MARGIN && rect.bottom > 0;
}

/**
 * Hiệu ứng "hiện dần khi cuộn tới" cho mọi phần tử có [data-reveal].
 *
 * Nguyên tắc quan trọng: NỘI DUNG KHÔNG BAO GIỜ ĐƯỢC ẨN VĨNH VIỄN.
 * Vì phần tử bắt đầu ở opacity 0, nếu IntersectionObserver không chạy (trình duyệt cũ,
 * tab bị ẩn không dựng khung hình, môi trường chụp ảnh tự động...) thì cả trang sẽ trắng.
 * Nên có 3 lớp bảo vệ:
 *   1. Quét thủ công lúc gắn: phần tử nào đã nằm trong khung nhìn thì hiện ngay.
 *   2. IntersectionObserver lo phần còn lại khi người dùng cuộn.
 *   3. Hẹn giờ dự phòng: nếu sau 2.5s observer chưa hề kích hoạt lần nào thì coi như
 *      không dùng được và hiện toàn bộ.
 */
export function useReveal(deps: unknown[] = []) {
  useEffect(() => {
    const nodes = Array.from(
      document.querySelectorAll<HTMLElement>('[data-reveal]:not(.is-visible)'),
    );

    // Lớp 4 — chốt chặn cuối, đặt TRƯỚC mọi lối thoát sớm.
    // Kể cả khi đã gán `is-visible`, nếu transition không chạy tới nơi (tab không dựng
    // khung hình) thì opacity vẫn là 0. Quét lại toàn bộ [data-reveal] theo giá trị thật
    // — không dùng biến `nodes`, vì selector ':not(.is-visible)' đã loại những phần tử
    // được gán class ở lượt effect trước, chính chúng mới là thứ có nguy cơ kẹt.
    const lastResort = window.setTimeout(() => {
      document.querySelectorAll<HTMLElement>('[data-reveal]').forEach((node) => {
        if (Number(getComputedStyle(node).opacity) < 1) {
          node.classList.add('is-visible', 'tcs-reveal-instant');
        }
      });
    }, LAST_RESORT_MS);

    const clearLast = () => window.clearTimeout(lastResort);
    if (nodes.length === 0) return clearLast;

    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion || typeof IntersectionObserver === 'undefined') {
      nodes.forEach((node) => node.classList.add('is-visible', 'tcs-reveal-instant'));
      return clearLast;
    }

    // Lớp 1 — hiện ngay những gì đã thấy được
    const pending = nodes.filter((node) => {
      if (isInViewport(node)) {
        node.classList.add('is-visible');
        return false;
      }
      return true;
    });

    if (pending.length === 0) return clearLast;

    // Lớp 2 — theo dõi phần còn lại
    let observerFired = false;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          observerFired = true;
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    );
    pending.forEach((node) => observer.observe(node));

    // Lớp 3 — dự phòng khi observer không hoạt động.
    // Thêm cả `tcs-reveal-instant` để tắt transition: ở môi trường không dựng khung hình
    // (tab ẩn, công cụ chụp ảnh tự động) transition không chạy tới nơi nên chỉ gán
    // `is-visible` thôi thì opacity vẫn kẹt ở 0.
    const failsafe = window.setTimeout(() => {
      if (observerFired) return;
      pending.forEach((node) => node.classList.add('is-visible', 'tcs-reveal-instant'));
    }, IO_FAILSAFE_MS);

    return () => {
      observer.disconnect();
      window.clearTimeout(failsafe);
      window.clearTimeout(lastResort);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
