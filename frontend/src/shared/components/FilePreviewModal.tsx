import { useEffect, useCallback, useState } from 'react';
import './FilePreviewModal.css';

export interface FilePreviewModalProps {
  readonly src: string;
  readonly fileName: string;
  readonly isOpen: boolean;
  readonly onClose: () => void;
}

export function FilePreviewModal({
  src,
  fileName,
  isOpen,
  onClose,
}: FilePreviewModalProps) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (!isOpen) return;
    document.body.style.overflow = 'hidden';
    globalThis.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = '';
      globalThis.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, handleKeyDown]);

  if (!isOpen) return null;

  const isPdf = /\.pdf$/i.test(src);

  if (isPdf) {
    return (
      <div className="fpm-overlay" role="presentation">
        <div
          className="fpm-dialog fpm-dialog--pdf"
          role="dialog"
          aria-modal="true"
          aria-label={`Previewing ${fileName}`}
        >
          <header className="fpm-header">
            <span className="fpm-filename" title={fileName}>
              {fileName}
            </span>
            <button className="fpm-close" onClick={onClose} aria-label="Close">
              ×
            </button>
          </header>
          <div className="fpm-body fpm-body--pdf">
            <iframe src={src} title={fileName} />
            <a
              className="fpm-open-link"
              href={src}
              target="_blank"
              rel="noopener noreferrer"
            >
              Open in new tab ↗
            </a>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fpm-overlay" role="presentation">
      <div
        className="fpm-dialog"
        role="dialog"
        aria-modal="true"
        aria-label={`Previewing ${fileName}`}
      >
        <header className="fpm-header">
          <span className="fpm-filename" title={fileName}>
            {fileName}
          </span>
          <button className="fpm-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>
        <div className="fpm-body">
          <ImageZoom src={src} fileName={fileName} />
        </div>
      </div>
    </div>
  );
}

function ImageZoom({
  src,
  fileName,
}: {
  readonly src: string;
  readonly fileName: string;
}) {
  const [scale, setScale] = useState(1);
  const [pos, setPos] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);
  const [origin, setOrigin] = useState({ x: 0, y: 0 });

  function handleWheel(e: React.WheelEvent<HTMLDivElement>) {
    e.preventDefault();
    setScale((s) => Math.min(5, Math.max(0.3, s - e.deltaY * 0.002)));
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId);
    setDragging(true);
    setOrigin({ x: e.clientX - pos.x, y: e.clientY - pos.y });
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (!dragging) return;
    setPos({ x: e.clientX - origin.x, y: e.clientY - origin.y });
  }

  function handlePointerUp(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.releasePointerCapture(e.pointerId);
    setDragging(false);
  }

  function reset() {
    setScale(1);
    setPos({ x: 0, y: 0 });
  }

  return (
    <div
      className="fpm-zoom-viewport"
      onWheel={handleWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
    >
      <img
        className="fpm-zoom-image"
        src={src}
        alt={fileName}
        draggable={false}
        style={{
          transform: `translate(${pos.x}px, ${pos.y}px) scale(${scale})`,
          cursor: dragging ? 'grabbing' : 'grab',
        }}
      />
      <div className="fpm-zoom-controls">
        <button onClick={reset} title="Reset zoom">
          Reset
        </button>
        <span className="fpm-zoom-badge">{(scale * 100).toFixed(0)}%</span>
      </div>
    </div>
  );
}
