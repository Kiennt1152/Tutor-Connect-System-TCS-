import { useEffect } from 'react';

const CARD_SELECTOR = '.tcs-listing-card, .tcs-class-card';

/**
 * Đèn rọi cam đi theo con trỏ bên trong thẻ.
 *
 * Chỉ ghi hai biến CSS `--tcs-mx` / `--tcs-my`; phần vẽ do CSS lo, JS không đụng layout.
 * Gom cập nhật vào một khung hình bằng requestAnimationFrame để không ghi style
 * nhiều lần trên mỗi lần chuột di chuyển.
 *
 * Bỏ qua hoàn toàn trên thiết bị cảm ứng (không có hover) và khi người dùng bật
 * chế độ giảm chuyển động.
 */
export function useCardSpotlight(deps: unknown[] = []) {
  useEffect(() => {
    const canHover = window.matchMedia?.('(hover: hover) and (pointer: fine)').matches;
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (!canHover || reduceMotion) return;

    let frame = 0;
    let pending: { el: HTMLElement; x: number; y: number } | null = null;

    const flush = () => {
      frame = 0;
      if (!pending) return;
      const { el, x, y } = pending;
      el.style.setProperty('--tcs-mx', `${x}px`);
      el.style.setProperty('--tcs-my', `${y}px`);
      pending = null;
    };

    const onMove = (event: PointerEvent) => {
      const target = event.target as HTMLElement | null;
      const card = target?.closest?.(CARD_SELECTOR) as HTMLElement | null;
      if (!card) return;
      const rect = card.getBoundingClientRect();
      pending = { el: card, x: event.clientX - rect.left, y: event.clientY - rect.top };
      if (frame === 0) frame = requestAnimationFrame(flush);
    };

    // Một listener duy nhất ở document, không gắn theo từng thẻ -> không phải gắn lại khi
    // danh sách đổi, và không rò rỉ listener.
    document.addEventListener('pointermove', onMove, { passive: true });

    return () => {
      document.removeEventListener('pointermove', onMove);
      if (frame) cancelAnimationFrame(frame);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
