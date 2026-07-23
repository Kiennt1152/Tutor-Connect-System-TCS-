import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/** Cuộn mượt tới phần tử có id trùng với hash (nếu có). */
function scrollToId(rawHash: string) {
  if (!rawHash) return false;
  const id = decodeURIComponent(rawHash.replace(/^#/, ''));
  if (!id) return false;
  const el = document.getElementById(id);
  if (el) {
    // Dùng 'auto' (tức thì) — 'smooth' không đáng tin ở một số trình duyệt/webview (không cuộn gì).
    el.scrollIntoView({ behavior: 'auto', block: 'start' });
    return true;
  }
  return false;
}

/**
 * React Router khong tu cuon toi anchor `#` khi dieu huong trong app.
 * Xu ly 2 truong hop:
 *  1. Tai trang / chuyen route co san hash (VD mo thang /#classes) -> cuon khi mount.
 *  2. Bam link `#...` ngay tren cung trang: react-router dung pushState, KHONG phat
 *     `hashchange` va effect useLocation khong chay lai -> phai bat click truc tiep.
 */
export function ScrollToHash() {
  const { pathname, hash } = useLocation();

  // (1) Có hash khi mount / đổi route — mục có thể chưa render xong nên thử lại vài lần.
  useEffect(() => {
    if (!hash) return;
    let attempts = 0;
    let timer = 0;
    const tick = () => {
      if (scrollToId(hash)) return;
      if (attempts < 20) {
        attempts += 1;
        timer = window.setTimeout(tick, 100);
      }
    };
    tick();
    return () => window.clearTimeout(timer);
  }, [pathname, hash]);

  // (2) Bấm link anchor cùng trang (VD "Tìm lớp" -> /#classes khi đang ở "/").
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      const anchor = (e.target as HTMLElement | null)?.closest?.('a[href*="#"]');
      if (!anchor) return;
      const href = anchor.getAttribute('href') ?? '';
      const url = new URL(href, window.location.origin);
      // Chỉ xử lý khi ở cùng trang; khác trang thì để router điều hướng rồi (1) lo cuộn.
      if (url.pathname !== window.location.pathname || !url.hash) return;
      // Đợi router cập nhật URL xong rồi cuộn.
      window.setTimeout(() => scrollToId(url.hash), 0);
    };
    document.addEventListener('click', onClick);
    return () => document.removeEventListener('click', onClick);
  }, []);

  return null;
}
