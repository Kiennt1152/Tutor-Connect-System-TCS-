import { useEffect, useMemo, useState } from 'react';
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
  onCreateGroup,
}: UserSearchModalProps) {
  const [mode, setMode] = useState<'direct' | 'group'>('direct');
  const [keyword, setKeyword] = useState('');
  const [groupName, setGroupName] = useState('');
  const [users, setUsers] = useState<UserSummaryResponse[]>([]);
  const [selected, setSelected] = useState<UserSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
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

  const selectedIds = useMemo(() => new Set(selected.map((user) => user.userId)), [selected]);

  const toggleUser = (user: UserSummaryResponse) => {
    if (mode === 'direct') {
      onSelectUser(user);
      return;
    }
    setSelected((current) => {
      if (current.some((item) => item.userId === user.userId)) {
        return current.filter((item) => item.userId !== user.userId);
      }
      return current.length >= 19 ? current : [...current, user];
    });
  };

  const handleCreateGroup = async () => {
    const normalizedName = groupName.trim();
    if (normalizedName.length < 3 || normalizedName.length > 80 || selected.length < 2) return;
    if (!onCreateGroup) return;
    setSubmitting(true);
    setError(null);
    try {
      await onCreateGroup(normalizedName, selected);
    } catch {
      setError('Không thể tạo nhóm. Hãy kiểm tra tên và danh sách thành viên.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className="msg-modal-overlay" onClick={onClose}>
      <div className="msg-user-search-modal" onClick={(event) => event.stopPropagation()}>
        <div className="msg-modal__header">
          <h2>Tạo cuộc trò chuyện</h2>
          <button type="button" className="msg-modal__close" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <div className="msg-new-mode" role="tablist" aria-label="Hình thức trò chuyện">
          <button
            type="button"
            className={`msg-new-mode__item ${mode === 'direct' ? 'msg-new-mode__item--active' : ''}`}
            onClick={() => setMode('direct')}
          >
            Trực tiếp
          </button>
          <button
            type="button"
            className={`msg-new-mode__item ${mode === 'group' ? 'msg-new-mode__item--active' : ''}`}
            onClick={() => setMode('group')}
          >
            Tạo nhóm
          </button>
        </div>

        <div className="msg-user-search-modal__body">
          {mode === 'group' && (
            <div className="msg-group-header-input">
              <div className="msg-group-camera-badge" title="Nhóm mới">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
              </div>
              <input
                type="text"
                className="msg-group-name-field"
                placeholder="Đặt tên nhóm (ví dụ: Nhóm học Toán 12)..."
                value={groupName}
                maxLength={80}
                onChange={(event) => setGroupName(event.target.value)}
              />
              <span className="msg-group-name-len">{groupName.trim().length}/80</span>
            </div>
          )}

          {mode === 'group' && selected.length > 0 && (
            <div className="msg-group-selected-strip">
              {selected.map((user) => (
                <div
                  key={user.userId}
                  className="msg-selected-bubble"
                  onClick={() => toggleUser(user)}
                  title={`Bỏ chọn ${user.displayName}`}
                >
                  <div className="msg-selected-bubble__avatar" style={{ backgroundColor: getAvatarColor(user.userId) }}>
                    {user.avatarUrl ? (
                      <img src={user.avatarUrl} alt="" />
                    ) : (
                      <span>{getInitials(user.displayName)}</span>
                    )}
                    <span className="msg-selected-bubble__close">×</span>
                  </div>
                  <span className="msg-selected-bubble__name">
                    {user.displayName.trim().split(' ').slice(-1)[0]}
                  </span>
                </div>
              ))}
            </div>
          )}

          <div className="msg-group-create__search-row">
            <input
              type="text"
              className="msg-search-input"
              placeholder={mode === 'group' ? 'Tìm thành viên thêm vào nhóm...' : 'Tìm kiếm người dùng theo tên...'}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              autoFocus={mode === 'direct'}
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
              users.map((user) => {
                const isSelected = selectedIds.has(user.userId);
                return (
                  <button
                    key={user.userId}
                    type="button"
                    className={`msg-user-search-modal__item ${isSelected ? 'msg-user-search-modal__item--selected' : ''}`}
                    onClick={() => toggleUser(user)}
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
                    {mode === 'group' && (
                      <div className={`msg-round-checkbox ${isSelected ? 'msg-round-checkbox--checked' : ''}`}>
                        {isSelected && (
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                          </svg>
                        )}
                      </div>
                    )}
                  </button>
                );
              })
            )}
          </div>

          {mode === 'group' && (
            <div className="msg-group-create__footer">
              <div className="msg-group-create__count">
                {selected.length > 0 ? (
                  <span>Đã chọn: <strong>{selected.length}</strong>/19</span>
                ) : (
                  <span>Chọn tối thiểu 2 người</span>
                )}
              </div>
              <div className="msg-group-create__btns">
                <button type="button" className="btn-ghost" onClick={onClose}>
                  Hủy
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  disabled={submitting || groupName.trim().length < 3 || selected.length < 2}
                  onClick={handleCreateGroup}
                >
                  {submitting ? 'Đang tạo...' : 'Tạo nhóm'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
