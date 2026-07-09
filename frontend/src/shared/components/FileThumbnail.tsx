import { useState, type ReactNode } from 'react';
import { FilePreviewModal } from './FilePreviewModal';
import './FileThumbnail.css';

export interface FileThumbnailProps {
  readonly src: string;
  readonly fileName: string;
  readonly mimeType: string | null;
  readonly fileSize: number | null;
  readonly actions?: ReactNode;
}

export function FileThumbnail({
  src,
  fileName,
  mimeType,
  fileSize,
  actions,
}: FileThumbnailProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [hoverPreview, setHoverPreview] = useState(false);
  const resolvedSrc = resolvePreviewSrc(src);

  const isImage = mimeType?.startsWith('image/') ?? false;
  const isPdf = mimeType === 'application/pdf';

  function handleMouseEnter() {
    if (isImage || isPdf) setHoverPreview(true);
  }

  function handleMouseLeave() {
    setHoverPreview(false);
  }

  function openModal() {
    setModalOpen(true);
  }

  return (
    <div
      className="ft"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <button
        type="button"
        className="ft-trigger"
        onClick={openModal}
        aria-label={`Preview ${fileName}`}
        title={`Click to preview ${fileName}`}
      >
        {isImage ? (
          <img className="ft-thumb" src={resolvedSrc} alt={fileName} loading="lazy" />
        ) : (
          <div className="ft-icon">{isPdf ? '📕' : '📄'}</div>
        )}
      </button>

      <div className="ft-info">
        <div className="ft-name" title={fileName}>
          {fileName}
        </div>
        <div className="ft-meta">
          {mimeType}
          {fileSize != null && ` · ${(fileSize / 1024).toFixed(1)} KB`}
        </div>
      </div>

      {actions && <div className="ft-actions">{actions}</div>}

      {hoverPreview && (isImage || isPdf) && (
        <div className="ft-popover" role="tooltip">
          <div className="ft-popover__body">
            {isImage ? (
              <img className="ft-popover__img" src={resolvedSrc} alt={fileName} />
            ) : (
              <div className="ft-popover__pdf">{fileName}</div>
            )}
          </div>
          <div className="ft-popover__hint">
            <span>Click to open full preview</span>
            <kbd>Esc</kbd> <span>to close</span>
          </div>
        </div>
      )}

      <FilePreviewModal
        src={resolvedSrc}
        fileName={fileName}
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
      />
    </div>
  );
}

function resolvePreviewSrc(src: string) {
  if (!src) {
    return src;
  }

  if (
    src.startsWith('http://') ||
    src.startsWith('https://') ||
    src.startsWith('blob:') ||
    src.startsWith('data:')
  ) {
    return src;
  }

  if (src.startsWith('/uploads/')) {
    return `${getBackendOrigin()}${src}`;
  }

  return src;
}

function getBackendOrigin() {
  const configuredApiUrl = import.meta.env.VITE_API_URL as string | undefined;

  if (configuredApiUrl && configuredApiUrl.startsWith('http://')) {
    return configuredApiUrl.replace(/\/api\/?$/, '');
  }

  if (configuredApiUrl && configuredApiUrl.startsWith('https://')) {
    return configuredApiUrl.replace(/\/api\/?$/, '');
  }

  return 'http://localhost:8080';
}
