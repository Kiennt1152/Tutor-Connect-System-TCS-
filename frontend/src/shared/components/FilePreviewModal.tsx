import { useEffect, useCallback, useRef, useState } from 'react';
import './FilePreviewModal.css';

export interface FilePreviewModalProps {
  readonly src: string;
  readonly fileName: string;
  readonly isOpen: boolean;
  readonly onClose: () => void;
  /**
   * Kiểu file từ server. Cần cho file tải bằng blob URL: lúc đó `src` là
   * "blob:http://..." nên không thể đoán định dạng từ đuôi đường dẫn.
   */
  readonly mimeType?: string | null;
}

const MIN_SCALE = 0.2;
const MAX_SCALE = 8;
const clampScale = (value: number) => Math.min(MAX_SCALE, Math.max(MIN_SCALE, value));

export function FilePreviewModal({
  src,
  fileName,
  isOpen,
  onClose,
  mimeType,
}: FilePreviewModalProps) {
  // Ưu tiên mimeType rồi tới tên file; `src` là blob URL nên không có đuôi để đoán.
  const isPdf =
    mimeType === 'application/pdf' ||
    /\.pdf(\?|#|$)/i.test(fileName) ||
    (!mimeType && /\.pdf(\?|#|$)/i.test(src));

  if (!isOpen) return null;

  return (
    <div className="fpm-overlay" role="presentation">
      <div
        className={`fpm-dialog${isPdf ? ' fpm-dialog--pdf' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={`Đang xem trước ${fileName}`}
      >
        {isPdf ? (
          <PdfViewer src={src} fileName={fileName} onClose={onClose} />
        ) : (
          <ImageViewer src={src} fileName={fileName} onClose={onClose} />
        )}
      </div>
    </div>
  );
}

function ViewerHeader({
  fileName,
  onClose,
  children,
}: {
  readonly fileName: string;
  readonly onClose: () => void;
  readonly children?: React.ReactNode;
}) {
  return (
    <header className="fpm-header">
      <span className="fpm-filename" title={fileName}>
        {fileName}
      </span>
      <div className="fpm-tools">
        {children}
        <button
          type="button"
          className="fpm-tool fpm-tool--close"
          onClick={onClose}
          aria-label="Đóng"
          title="Đóng (Esc)"
        >
          ×
        </button>
      </div>
    </header>
  );
}

function PdfViewer({
  src,
  fileName,
  onClose,
}: {
  readonly src: string;
  readonly fileName: string;
  readonly onClose: () => void;
}) {
  useEscapeAndScrollLock(onClose);

  return (
    <>
      <ViewerHeader fileName={fileName} onClose={onClose}>
        <a className="fpm-tool" href={src} target="_blank" rel="noopener noreferrer">
          Mở tab mới
        </a>
      </ViewerHeader>
      <div className="fpm-body fpm-body--pdf">
        <iframe src={src} title={fileName} />
      </div>
    </>
  );
}

function ImageViewer({
  src,
  fileName,
  onClose,
}: {
  readonly src: string;
  readonly fileName: string;
  readonly onClose: () => void;
}) {
  const viewportRef = useRef<HTMLDivElement>(null);
  /** 'fit' = vừa khung (mặc định), 'actual' = kích thước gốc 1:1. */
  const [mode, setMode] = useState<'fit' | 'actual'>('fit');
  const [scale, setScale] = useState(1);
  const [rotation, setRotation] = useState(0);
  const [pos, setPos] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);
  const originRef = useRef({ x: 0, y: 0 });

  const reset = useCallback(() => {
    setScale(1);
    setPos({ x: 0, y: 0 });
    setRotation(0);
  }, []);

  // Ảnh mới thì bỏ hết thu phóng/xoay của ảnh trước.
  useEffect(() => {
    setMode('fit');
    reset();
  }, [src, reset]);

  /** Phóng quanh một điểm trên khung; không truyền điểm thì phóng quanh tâm. */
  const zoomBy = useCallback((factor: number, anchor?: { x: number; y: number }) => {
    setScale((current) => {
      const next = clampScale(current * factor);
      const ratio = next / current;
      setPos((p) => {
        if (!anchor) return p;
        // transform là translate(pos) scale(scale) quanh tâm khung, nên muốn giữ nguyên
        // điểm dưới con trỏ thì dịch pos theo đúng tỉ lệ phóng.
        return {
          x: anchor.x - (anchor.x - p.x) * ratio,
          y: anchor.y - (anchor.y - p.y) * ratio,
        };
      });
      return next;
    });
  }, []);

  const toggleMode = useCallback(() => {
    setMode((m) => (m === 'fit' ? 'actual' : 'fit'));
    reset();
  }, [reset]);

  const rotate = useCallback(() => setRotation((r) => (r + 90) % 360), []);

  useEscapeAndScrollLock(onClose, (e) => {
    if (e.key === '+' || e.key === '=') zoomBy(1.25);
    else if (e.key === '-' || e.key === '_') zoomBy(0.8);
    else if (e.key === '0') reset();
    else if (e.key === 'r' || e.key === 'R') rotate();
    else return false;
    return true;
  });

  // React gắn onWheel dạng passive nên preventDefault không có tác dụng;
  // phải tự đăng ký listener với passive: false để chặn cuộn nền khi phóng.
  useEffect(() => {
    const el = viewportRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const rect = el.getBoundingClientRect();
      zoomBy(e.deltaY < 0 ? 1.15 : 1 / 1.15, {
        x: e.clientX - (rect.left + rect.width / 2),
        y: e.clientY - (rect.top + rect.height / 2),
      });
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [zoomBy]);

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId);
    setDragging(true);
    originRef.current = { x: e.clientX - pos.x, y: e.clientY - pos.y };
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (!dragging) return;
    setPos({ x: e.clientX - originRef.current.x, y: e.clientY - originRef.current.y });
  }

  function handlePointerUp(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.releasePointerCapture(e.pointerId);
    setDragging(false);
  }

  return (
    <>
      <ViewerHeader fileName={fileName} onClose={onClose}>
        <button type="button" className="fpm-tool" onClick={() => zoomBy(0.8)} title="Thu nhỏ (−)">
          −
        </button>
        <span className="fpm-zoom-badge">{Math.round(scale * 100)}%</span>
        <button type="button" className="fpm-tool" onClick={() => zoomBy(1.25)} title="Phóng to (+)">
          +
        </button>
        <button
          type="button"
          className="fpm-tool"
          onClick={toggleMode}
          title="Đổi giữa vừa khung và kích thước gốc"
        >
          {mode === 'fit' ? 'Cỡ gốc' : 'Vừa khung'}
        </button>
        <button type="button" className="fpm-tool" onClick={rotate} title="Xoay 90° (R)">
          ↻
        </button>
        <button type="button" className="fpm-tool" onClick={reset} title="Đặt lại (0)">
          Đặt lại
        </button>
        <a className="fpm-tool" href={src} target="_blank" rel="noopener noreferrer">
          Mở tab mới
        </a>
      </ViewerHeader>

      <div
        className="fpm-body"
        ref={viewportRef}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onDoubleClick={() => (scale > 1 ? reset() : zoomBy(2))}
      >
        <img
          className={`fpm-image fpm-image--${mode}${dragging ? ' is-dragging' : ''}`}
          src={src}
          alt={fileName}
          draggable={false}
          style={{
            transform: `translate(${pos.x}px, ${pos.y}px) scale(${scale}) rotate(${rotation}deg)`,
            cursor: dragging ? 'grabbing' : 'grab',
          }}
        />
      </div>

      <p className="fpm-hint">Cuộn để phóng · Kéo để di chuyển · Nhấp đúp để phóng nhanh</p>
    </>
  );
}

/**
 * Khoá cuộn nền khi mở, đóng bằng Esc, và cho phép bắt thêm phím tắt riêng.
 * Hàm `extraKeys` trả về true nếu đã xử lý phím đó.
 */
function useEscapeAndScrollLock(
  onClose: () => void,
  extraKeys?: (e: KeyboardEvent) => boolean,
) {
  const extraRef = useRef(extraKeys);
  extraRef.current = extraKeys;

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
        return;
      }
      if (extraRef.current?.(e)) e.preventDefault();
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    globalThis.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      globalThis.removeEventListener('keydown', onKeyDown);
    };
  }, [onClose]);
}
