import { useEffect, useRef } from 'react';

/**
 * Chạy callback theo chu kỳ khi `enabled` bật.
 * Callback mới nhất luôn được dùng, và không cho chạy chồng nhiều request cùng lúc.
 */
export function useAutoPolling(
  callback: () => void | Promise<void>,
  enabled: boolean,
  intervalMs: number,
) {
  const callbackRef = useRef(callback);
  const runningRef = useRef(false);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    if (!enabled) {
      runningRef.current = false;
      return;
    }

    const tick = () => {
      if (runningRef.current) return;
      runningRef.current = true;
      Promise.resolve(callbackRef.current())
        .catch(() => undefined)
        .finally(() => {
          runningRef.current = false;
        });
    };

    const timer = window.setInterval(tick, intervalMs);
    return () => {
      window.clearInterval(timer);
      runningRef.current = false;
    };
  }, [enabled, intervalMs]);
}
