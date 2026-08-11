import { useEffect, useMemo, useState } from 'react';
import { messagingApi } from '../api/messagingApi';
import type {
  ConversationResponse,
  GroupMemberResponse,
  UserSummaryResponse,
} from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

type GroupInfoPanelProps = {
  conversation: ConversationResponse;
  currentUserId: number | undefined;
  onClose: () => void;
  onUpdated: (conversation: ConversationResponse) => void;
  onLeft: (conversationId: number) => void;
};

export function GroupInfoPanel({
  conversation,
  currentUserId,
  onClose,
  onUpdated,
  onLeft,
}: GroupInfoPanelProps) {
  const [members, setMembers] = useState<GroupMemberResponse[]>([]);
  const [name, setName] = useState(conversation.name ?? '');
  const [showAdd, setShowAdd] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [candidates, setCandidates] = useState<UserSummaryResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isOwner = conversation.ownerUserId === currentUserId;
  const memberIds = useMemo(() => new Set(members.map((member) => member.userId)), [members]);

  const loadMembers = async () => {
    setLoading(true);
    try {
      setMembers(await messagingApi.getGroupMembers(conversation.conversationId));
      setError(null);
    } catch {
      setError('Không thể tải danh sách thành viên.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    messagingApi.getGroupMembers(conversation.conversationId).then((data) => {
      if (!cancelled) {
        setMembers(data);
        setError(null);
      }
    }).catch(() => {
      if (!cancelled) setError('Không thể tải danh sách thành viên.');
    }).finally(() => {
      if (!cancelled) setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [conversation.conversationId]);

  useEffect(() => {
    if (!showAdd) return;
    let cancelled = false;
    const timeout = setTimeout(() => {
      messagingApi.listUsers(keyword).then((users) => {
        if (!cancelled) setCandidates(users.filter((user) => !memberIds.has(user.userId)));
      }).catch(() => {
        if (!cancelled) setCandidates([]);
      });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [keyword, memberIds, showAdd]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch {
      setError('Không thể cập nhật nhóm. Vui lòng thử lại.');
    } finally {
      setBusy(false);
    }
  };

  const saveName = () => run(async () => {
    const updated = await messagingApi.renameGroup(conversation.conversationId, name.trim());
    onUpdated(updated);
  });

  const addMembers = () => run(async () => {
    const updated = await messagingApi.addGroupMembers(conversation.conversationId, selectedIds);
    onUpdated(updated);
    setSelectedIds([]);
    setKeyword('');
    setShowAdd(false);
    await loadMembers();
  });

  const removeMember = (member: GroupMemberResponse) => run(async () => {
    if (!window.confirm(`Xóa ${member.displayName} khỏi nhóm?`)) return;
    await messagingApi.removeGroupMember(conversation.conversationId, member.userId);
    const nextMembers = members.filter((item) => item.userId !== member.userId);
    setMembers(nextMembers);
    onUpdated({ ...conversation, participantCount: nextMembers.length });
  });

  const transferOwner = (member: GroupMemberResponse) => run(async () => {
    if (!window.confirm(`Chuyển quyền owner cho ${member.displayName}?`)) return;
    const updated = await messagingApi.transferGroupOwner(conversation.conversationId, member.userId);
    onUpdated(updated);
    await loadMembers();
  });

  const leaveGroup = () => run(async () => {
    if (!window.confirm('Rời nhóm này? Bạn sẽ không thể xem hoặc gửi tin nhắn nữa.')) return;
    await messagingApi.leaveGroup(conversation.conversationId);
    onLeft(conversation.conversationId);
  });

  return (
    <div className="msg-group-panel-overlay" onClick={onClose}>
      <aside className="msg-group-panel" onClick={(event) => event.stopPropagation()}>
        <header className="msg-group-panel__header">
          <div>
            <span className="msg-group-panel__eyebrow">Thông tin nhóm</span>
            <h2>{conversation.name}</h2>
          </div>
          <button type="button" className="msg-modal__close" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </header>

        <div className="msg-group-panel__body">
          {isOwner && (
            <section className="msg-group-panel__section">
              <label htmlFor="edit-group-name">Tên nhóm</label>
              <div className="msg-group-panel__inline">
                <input
                  id="edit-group-name"
                  className="msg-search-input"
                  value={name}
                  maxLength={80}
                  onChange={(event) => setName(event.target.value)}
                />
                <button
                  type="button"
                  className="tcs-btn tcs-btn--primary"
                  disabled={busy || name.trim().length < 3 || name.trim() === conversation.name}
                  onClick={saveName}
                >
                  Lưu
                </button>
              </div>
            </section>
          )}

          <section className="msg-group-panel__section">
            <div className="msg-group-panel__section-head">
              <h3>Thành viên ({members.length || conversation.participantCount})</h3>
              {isOwner && members.length < 20 && (
                <button type="button" className="tcs-btn tcs-btn--ghost" onClick={() => setShowAdd(!showAdd)}>
                  {showAdd ? 'Đóng' : 'Thêm người'}
                </button>
              )}
            </div>

            {showAdd && (
              <div className="msg-group-add">
                <input
                  className="msg-search-input"
                  placeholder="Tìm người dùng..."
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                />
                <div className="msg-group-add__results">
                  {candidates.map((candidate) => {
                    const selected = selectedIds.includes(candidate.userId);
                    const maxSelected = selectedIds.length >= 20 - members.length;
                    return (
                      <button
                        key={candidate.userId}
                        type="button"
                        disabled={!selected && maxSelected}
                        className={selected ? 'msg-group-add__item msg-group-add__item--selected' : 'msg-group-add__item'}
                        onClick={() => setSelectedIds((ids) => selected
                          ? ids.filter((id) => id !== candidate.userId)
                          : [...ids, candidate.userId])}
                      >
                        <span>{candidate.displayName}</span>
                        <span>{selected ? '✓' : '+'}</span>
                      </button>
                    );
                  })}
                </div>
                <button
                  type="button"
                  className="tcs-btn tcs-btn--primary"
                  disabled={busy || selectedIds.length === 0}
                  onClick={addMembers}
                >
                  Thêm {selectedIds.length || ''} thành viên
                </button>
              </div>
            )}

            {loading ? (
              <div className="msg-state msg-state--loading">Đang tải thành viên...</div>
            ) : (
              <div className="msg-group-members">
                {members.map((member) => (
                  <div key={member.userId} className="msg-group-member">
                    <div className="msg-avatar" style={{ backgroundColor: getAvatarColor(member.userId) }}>
                      {member.avatarUrl ? (
                        <img src={member.avatarUrl} alt={member.displayName} className="msg-avatar__img" />
                      ) : (
                        <span>{getInitials(member.displayName)}</span>
                      )}
                    </div>
                    <div className="msg-group-member__body">
                      <strong>{member.displayName}</strong>
                      <span>{member.owner ? 'Owner' : member.role}</span>
                    </div>
                    {isOwner && !member.owner && (
                      <div className="msg-group-member__actions">
                        <button type="button" title="Chuyển quyền owner" onClick={() => transferOwner(member)} disabled={busy}>
                          Owner
                        </button>
                        <button type="button" title="Xóa khỏi nhóm" onClick={() => removeMember(member)} disabled={busy}>
                          ×
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>

          {error && <div className="msg-state msg-state--error">{error}</div>}

          {!isOwner && (
            <button type="button" className="msg-group-panel__leave" onClick={leaveGroup} disabled={busy}>
              Rời nhóm
            </button>
          )}
          {isOwner && (
            <p className="msg-group-panel__owner-note">
              Chuyển quyền owner cho một thành viên khác trước khi rời nhóm.
            </p>
          )}
        </div>
      </aside>
    </div>
  );
}
