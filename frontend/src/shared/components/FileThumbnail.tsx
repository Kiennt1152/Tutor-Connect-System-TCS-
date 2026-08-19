import { useEffect, useState, type ReactNode } from 'react';
import axiosClient from '../api/axiosClient';
import { FilePreviewModal } from './FilePreviewModal';
import './FileThumbnail.css';

export interface FileThumbnailProps {
  readonly src: string;
  readonly fileName: string;
  readonly mimeType: string | null;
  readonly fileSize: number | null;
  readonly actions?: ReactNode;
  /**
   * MediaFile id. Bắt buộc với file riêng tư (CCCD, giấy tờ, hồ sơ xác minh):
   * các file này nằm sau /api/files/private/{fileId} và cần JWT, mà thẻ <img>
   * không gửi được header Authorization — nên phải tải qua axios rồi tạo blob URL.
   */
  readonly fileId?: number;
}

export function FileThumbnail({
  src,
  fileName,
  mimeType,
  fileSize,
  actions,
  fileId,
}: FileThumbnailProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [hoverPreview, setHoverPreview] = useState(false);
  const [privateBlobUrl, setPrivateBlobUrl] = useState<string | null>(null);

  const isPrivateFile = isPrivatePath(src);

  // File riêng tư: tải kèm JWT qua axios, chuyển thành blob URL để <img>/preview dùng được.
  useEffect(() => {
    if (!isPrivateFile || fileId == null) {
      setPrivateBlobUrl(null);
      return;
    }
    let cancelled = false;
    let objectUrl: string | null = null;
    axiosClient
      .get(`/files/private/${fileId}`, { responseType: 'blob' })
      .then((res) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(res.data as Blob);
        setPrivateBlobUrl(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setPrivateBlobUrl(null);
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [isPrivateFile, fileId]);

  // Với file riêng tư dùng blob đã xác thực; file công khai dùng URL trực tiếp.
  const resolvedSrc =
    isPrivateFile && fileId != null ? privateBlobUrl : resolvePreviewSrc(src);

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
        aria-label={`Xem trước ${fileName}`}
        title={`Bấm để xem trước ${fileName}`}
      >
        {isImage ? (
          resolvedSrc ? (
            <img className="ft-thumb" src={resolvedSrc} alt={fileName} loading="lazy" />
          ) : (
            <div className="ft-icon" aria-label="Đang tải ảnh">⏳</div>
          )
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
            {isImage && resolvedSrc ? (
              <img className="ft-popover__img" src={resolvedSrc} alt={fileName} />
            ) : (
              <div className="ft-popover__pdf">{fileName}</div>
            )}
          </div>
          <div className="ft-popover__hint">
            <span>Bấm để xem đầy đủ</span>
            <kbd>Esc</kbd> <span>để đóng</span>
          </div>
        </div>
      )}

      <FilePreviewModal
        src={resolvedSrc ?? ''}
        fileName={fileName}
        mimeType={mimeType}
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
      />
    </div>
  );
}

/** File riêng tư (CCCD, giấy tờ, hồ sơ xác minh) nằm ở /uploads/private/ và cần JWT để xem. */
function isPrivatePath(src: string): boolean {
  return src.includes('/uploads/private/');
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

  if (
    configuredApiUrl &&
    (configuredApiUrl.startsWith('http://') || configuredApiUrl.startsWith('https://'))
  ) {
    return configuredApiUrl.replace(/\/api\/?$/, '');
  }

  return window.location.origin;
}
