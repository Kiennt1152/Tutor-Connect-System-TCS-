import { useRef } from 'react';
import type { ProfileResponse } from '../types/profileTypes';

interface AvatarSectionProps {
  profile: ProfileResponse | null;
  uploading: boolean;
  onUpload: (file: File) => void | Promise<unknown>;
}

export default function AvatarSection({ profile, uploading, onUpload }: AvatarSectionProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  async function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    await onUpload(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  return (
    <section className="profile-section">
      <h2>Ảnh đại diện</h2>
      <div className="profile-avatar">
        {profile?.avatarUrl ? (
          <img src={profile.avatarUrl} alt="Ảnh đại diện" />
        ) : (
          <div className="profile-avatar-placeholder">Chưa có ảnh</div>
        )}
        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            hidden
            onChange={handleChange}
          />
          <button
            type="button"
            className="btn-secondary"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
          >
            {uploading ? 'Đang tải lên...' : 'Đổi ảnh đại diện'}
          </button>
          <p className="profile-hint">Hỗ trợ JPEG/PNG/WebP, tối đa 5 MB.</p>
        </div>
      </div>
    </section>
  );
}
