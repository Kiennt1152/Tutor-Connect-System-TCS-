import { FileThumbnail } from './FileThumbnail';

/** Đoán kiểu file từ đuôi đường dẫn — bằng chứng chỉ được lưu dưới dạng URL, không kèm mimeType. */
export function evidenceMimeType(url: string): string | null {
  const normalized = url.split(/[?#]/)[0].toLowerCase();
  if (normalized.endsWith('.jpg') || normalized.endsWith('.jpeg')) return 'image/jpeg';
  if (normalized.endsWith('.png')) return 'image/png';
  if (normalized.endsWith('.webp')) return 'image/webp';
  if (normalized.endsWith('.gif')) return 'image/gif';
  if (normalized.endsWith('.bmp')) return 'image/bmp';
  if (normalized.endsWith('.heic') || normalized.endsWith('.heif')) return 'image/heic';
  if (normalized.endsWith('.pdf')) return 'application/pdf';
  return null;
}

export function evidenceFileName(url: string, index: number): string {
  const path = url.split(/[?#]/)[0];
  const rawFileName = path.split('/').filter(Boolean).pop();
  if (!rawFileName) return `Bằng chứng ${index + 1}`;
  try {
    return decodeURIComponent(rawFileName);
  } catch {
    return rawFileName;
  }
}

export interface EvidencePreviewListProps {
  readonly urls: string[];
  readonly emptyText?: string;
  /** Class của khung bao ngoài, để mỗi trang giữ khoảng cách riêng. */
  readonly className?: string;
  readonly emptyClassName?: string;
  readonly linkClassName?: string;
}

/**
 * Danh sách bằng chứng (báo cáo vi phạm, tranh chấp, phiếu hỗ trợ).
 * File nhận ra được thì hiện ảnh thu nhỏ, bấm vào mở trình xem đầy đủ;
 * còn lại rơi về link thường. Dùng chung để mọi màn hình xem bằng chứng giống nhau.
 */
export function EvidencePreviewList({
  urls,
  emptyText = 'Chưa có bằng chứng đính kèm.',
  className,
  emptyClassName,
  linkClassName,
}: EvidencePreviewListProps) {
  if (urls.length === 0) {
    return <p className={emptyClassName}>{emptyText}</p>;
  }

  return (
    <div className={className}>
      {urls.map((url, index) => {
        const mimeType = evidenceMimeType(url);
        if (!mimeType) {
          return (
            <a key={url} className={linkClassName} href={url} target="_blank" rel="noreferrer">
              {url}
            </a>
          );
        }
        return (
          <FileThumbnail
            key={url}
            src={url}
            fileName={evidenceFileName(url, index)}
            mimeType={mimeType}
            fileSize={null}
          />
        );
      })}
    </div>
  );
}
