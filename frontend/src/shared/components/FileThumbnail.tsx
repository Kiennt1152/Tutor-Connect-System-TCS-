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
  readonly showHoverPreview?: boolean;
  /**
   * MediaFile id. Dùng được cho file riêng tư (CCCD, giấy tờ, hồ sơ xác minh,
   * bằng chứng lớp học):
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
  showHoverPreview = true,
}: FileThumbnailProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [hoverPreview, setHoverPreview] = useState(false);
  const [privateBlobUrl, setPrivateBlobUrl] = useState<string | null>(null);
  const [privateLoadFailed, setPrivateLoadFailed] = useState(false);

  const isPrivateFile = isPrivatePath(src);

  // File riêng tư: tải kèm JWT qua axios, chuyển thành blob URL để <img>/preview dùng được.
  useEffect(() => {
    if (!isPrivateFile) {
      setPrivateBlobUrl(null);
      setPrivateLoadFailed(false);
      return;
    }
    let cancelled = false;
    let objectUrl: string | null = null;
    setPrivateBlobUrl(null);
    setPrivateLoadFailed(false);

    const request = fileId != null
      ? axiosClient.get(`/files/private/${fileId}`, { responseType: 'blob' })
      : axiosClient.get('/files/private/by-url', {
        params: { url: src },
        responseType: 'blob',
      });

    request
      .then((res) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(res.data as Blob);
        setPrivateBlobUrl(objectUrl);
      })
      .catch(() => {
        if (!cancelled) {
          setPrivateBlobUrl(null);
          setPrivateLoadFailed(true);
        }
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [isPrivateFile, fileId, src]);

  // Với file riêng tư dùng blob đã xác thực; file công khai dùng URL trực tiếp.
  const resolvedSrc =
    isPrivateFile ? privateBlobUrl : resolvePreviewSrc(src);

  const isImage = mimeType?.startsWith('image/') ?? false;
  const isPdf = mimeType === 'application/pdf';
  const canPreview = Boolean(resolvedSrc);

  function handleMouseEnter() {
    if (showHoverPreview && canPreview && (isImage || isPdf)) setHoverPreview(true);
  }

  function handleMouseLeave() {
    setHoverPreview(false);
  }

  function openModal() {
    if (!canPreview) return;
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
        disabled={!canPreview}
        aria-label={`Xem trước ${fileName}`}
        title={canPreview ? `Bấm để xem trước ${fileName}` : `Đang tải ${fileName}`}
      >
        {isImage ? (
          resolvedSrc ? (
            <img className="ft-thumb" src={resolvedSrc} alt={fileName} loading="lazy" />
          ) : privateLoadFailed ? (
            <div className="ft-icon" aria-label="Không tải được ảnh">!</div>
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

      {showHoverPreview && hoverPreview && canPreview && (isImage || isPdf) && (
        <div className="ft-popover" role="tooltip">
          <div className="ft-popover__body">
            {isImage ? (
              <img className="ft-popover__img" src={resolvedSrc ?? ''} alt={fileName} />
            ) : (
              <div className="ft-popover__pdf">Bấm để mở tài liệu</div>
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

/** File riêng tư (CCCD, giấy tờ, bằng chứng) nằm ở /uploads/private/ và cần JWT để xem. */
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
