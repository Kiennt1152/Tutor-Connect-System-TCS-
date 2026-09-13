import { useEffect, useState } from 'react';
import { messagingApi } from '../api/messagingApi';
import type { UserSummaryResponse } from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

type UserSearchModalProps = {
  open: boolean;
  onClose: () => void;
  onSelectUser: (user: UserSummaryResponse) => void;
  onCreateGroup?: (name: string, users: UserSummaryResponse[]) => Promise<void>;
};

const ROLE_LABELS: Record<string, string> = {
  CLIENT: 'Học viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm',
  PLATFORM_ADMIN: 'Quản trị viên',
};

export function UserSearchModal({
  open,
  onClose,
  onSelectUser,
}: UserSearchModalProps) {
  const [keyword, setKeyword] = useState('');
  const [users, setUsers] = useState<UserSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    const timeout = setTimeout(() => {
      setLoading(true);
      setError(null);
      messagingApi
        .listUsers(keyword)
        .then((data) => {
          // Chỉ hiển thị người dùng thông thường, ẩn Platform Admin (liên hệ Admin qua Ticket hỗ trợ)
          if (!cancelled) setUsers(data.filter((u) => u.role !== 'PLATFORM_ADMIN'));
        })
        .catch(() => {
          if (!cancelled) setError('Không thể tìm người dùng');
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, 300);

    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [open, keyword]);

  const handleSelectUser = (user: UserSummaryResponse) => {
    onSelectUser(user);
  };

  if (!open) return null;

  return (
    <div className="msg-modal-overlay" onClick={onClose}>
      <div className="msg-user-search-modal" onClick={(event) => event.stopPropagation()}>
        <div className="msg-modal__header">
          <h2>Cuộc trò chuyện mới</h2>
          <button type="button" className="msg-modal__close" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <div className="msg-user-search-modal__body">
          <div className="msg-group-create__search-row">
            <input
              type="text"
              className="msg-search-input"
              placeholder="Tìm kiếm người dùng theo tên..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              autoFocus
            />
          </div>

          <div className="msg-user-search-modal__list">
            {loading ? (
              <div className="msg-state msg-state--loading">Đang tìm...</div>
            ) : error ? (
              <div className="msg-state msg-state--error">{error}</div>
            ) : users.length === 0 ? (
              <div className="msg-state msg-state--empty">Không tìm thấy người dùng</div>
            ) : (
              users.map((user) => (
                <button
                  key={user.userId}
                  type="button"
                  className="msg-user-search-modal__item"
                  onClick={() => handleSelectUser(user)}
                >
                  <div className="msg-avatar" style={{ backgroundColor: getAvatarColor(user.userId) }}>
                    {user.avatarUrl ? (
                      <img src={user.avatarUrl} alt={user.displayName} className="msg-avatar__img" />
                    ) : (
                      <span>{getInitials(user.displayName)}</span>
                    )}
                  </div>
                  <div className="msg-user-search-modal__item-body">
                    <span className="msg-user-search-modal__item-name">{user.displayName}</span>
                    <span className="msg-user-search-modal__item-role">
                      {ROLE_LABELS[user.role] ?? user.role}
                    </span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
