import { useEffect, useMemo, useState } from 'react';
import { messagingApi } from '../api/messagingApi';
import type { UserSummaryResponse } from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

type UserSearchModalProps = {
  open: boolean;
  onClose: () => void;
  onSelectUser: (user: UserSummaryResponse) => void;
  onCreateGroup: (name: string, users: UserSummaryResponse[]) => Promise<void>;
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
          if (!cancelled) setUsers(data);
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
          <h2>Cuộc trò chuyện mới</h2>
          <button type="button" className="msg-modal__close" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <div className="msg-new-mode" role="tablist" aria-label="Loại cuộc trò chuyện">
          <button
            type="button"
            className={mode === 'direct' ? 'msg-new-mode__item msg-new-mode__item--active' : 'msg-new-mode__item'}
            onClick={() => setMode('direct')}
          >
            Trực tiếp
          </button>
          <button
            type="button"
            className={mode === 'group' ? 'msg-new-mode__item msg-new-mode__item--active' : 'msg-new-mode__item'}
            onClick={() => setMode('group')}
          >
            Nhóm
          </button>
        </div>

        <div className="msg-user-search-modal__body">
          {mode === 'group' && (
            <div className="msg-group-create__name">
              <label htmlFor="group-name">Tên nhóm</label>
              <input
                id="group-name"
                type="text"
                className="msg-search-input"
                placeholder="Ví dụ: Nhóm học Toán 12"
                value={groupName}
                maxLength={80}
                onChange={(event) => setGroupName(event.target.value)}
              />
              <span>{groupName.trim().length}/80 ký tự</span>
            </div>
          )}

          {mode === 'group' && selected.length > 0 && (
            <div className="msg-group-create__selected">
              {selected.map((user) => (
                <button key={user.userId} type="button" onClick={() => toggleUser(user)}>
                  {user.displayName} ×
                </button>
              ))}
            </div>
          )}

          <div className="msg-group-create__search-row">
            <input
              type="text"
              className="msg-search-input"
              placeholder="Tìm theo tên, email hoặc số điện thoại..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              autoFocus={mode === 'direct'}
            />
            {mode === 'group' && <span>{selected.length}/19 đã chọn</span>}
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
                    className={`msg-user-search-modal__item${isSelected ? ' msg-user-search-modal__item--selected' : ''}`}
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
                    {mode === 'group' && <span className="msg-member-check">{isSelected ? '✓' : '+'}</span>}
                  </button>
                );
              })
            )}
          </div>

          {mode === 'group' && (
            <div className="msg-group-create__actions">
              <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose}>
                Hủy
              </button>
              <button
                type="button"
                className="tcs-btn tcs-btn--primary"
                disabled={submitting || groupName.trim().length < 3 || selected.length < 2}
                onClick={handleCreateGroup}
              >
                {submitting ? 'Đang tạo...' : 'Tạo nhóm'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
