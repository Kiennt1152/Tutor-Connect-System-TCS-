import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * React Router khong tu cuon toi anchor `#` khi dieu huong trong app
 * (VD: bam "Tim lop" -> /#classes chi doi URL roi dung yen).
 * Component nay lang nghe hash va cuon toi dung muc.
 */
export function ScrollToHash() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (!hash) {
      return;
    }
    const id = decodeURIComponent(hash.slice(1));

    // Muc can toi co the chua render xong (trang con dang tai du lieu) -> thu lai vai lan.
    let attempts = 0;
    let frame = 0;
    const tryScroll = () => {
      const el = document.getElementById(id);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
      }
      if (attempts < 20) {
        attempts += 1;
        frame = window.setTimeout(tryScroll, 100);
      }
    };
    tryScroll();

    return () => window.clearTimeout(frame);
  }, [pathname, hash]);

  return null;
}
