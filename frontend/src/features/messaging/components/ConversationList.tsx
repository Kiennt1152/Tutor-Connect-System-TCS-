import { useEffect, useState } from 'react';
import type { ConversationResponse } from '../types/messagingTypes';
import { ConversationItem, type ConversationAction } from './ConversationItem';
import { messagingApi } from '../api/messagingApi';

const STORAGE_KEYS = {
  PINNED: 'tcs_pinned_convs',
  MUTED: 'tcs_muted_convs',
  UNREAD: 'tcs_unread_convs',
  HIDDEN: 'tcs_hidden_convs',
};

function loadStorageSet(key: string): Set<number> {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return new Set();
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? new Set(arr.map(Number)) : new Set();
  } catch {
    return new Set();
  }
}

function saveStorageSet(key: string, set: Set<number>) {
  try {
    localStorage.setItem(key, JSON.stringify(Array.from(set)));
  } catch {
    // ignore
  }
}

type ConversationListProps = {
  conversations: ConversationResponse[];
  activeConversationId: number | null;
  loading: boolean;
  error: string | null;
  onSelect: (conversation: ConversationResponse) => void;
  onNewConversation: () => void;
  onDeleteConversation?: (conversationId: number) => void;
};

export function ConversationList({
  conversations,
  activeConversationId,
  loading,
  error,
  onSelect,
  onNewConversation,
  onDeleteConversation,
}: ConversationListProps) {
  const [search, setSearch] = useState('');
  const [openMenuId, setOpenMenuId] = useState<number | null>(null);

  const [pinnedIds, setPinnedIds] = useState<Set<number>>(() => loadStorageSet(STORAGE_KEYS.PINNED));
  const [mutedIds, setMutedIds] = useState<Set<number>>(() => loadStorageSet(STORAGE_KEYS.MUTED));
  const [manualUnreadIds, setManualUnreadIds] = useState<Set<number>>(() => loadStorageSet(STORAGE_KEYS.UNREAD));
  const [hiddenIds, setHiddenIds] = useState<Set<number>>(() => loadStorageSet(STORAGE_KEYS.HIDDEN));

  // Close menu when clicking outside
  useEffect(() => {
    if (openMenuId === null) return;
    const handleWindowClick = () => setOpenMenuId(null);
    window.addEventListener('click', handleWindowClick);
    return () => window.removeEventListener('click', handleWindowClick);
  }, [openMenuId]);

  const handleAction = (conversation: ConversationResponse, action: ConversationAction) => {
    const id = conversation.conversationId;
    setOpenMenuId(null);

    if (action === 'pin') {
      setPinnedIds((prev) => {
        const next = new Set(prev).add(id);
        saveStorageSet(STORAGE_KEYS.PINNED, next);
        return next;
      });
    } else if (action === 'unpin') {
      setPinnedIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        saveStorageSet(STORAGE_KEYS.PINNED, next);
        return next;
      });
    } else if (action === 'mute') {
      setMutedIds((prev) => {
        const next = new Set(prev).add(id);
        saveStorageSet(STORAGE_KEYS.MUTED, next);
        return next;
      });
    } else if (action === 'unmute') {
      setMutedIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        saveStorageSet(STORAGE_KEYS.MUTED, next);
        return next;
      });
    } else if (action === 'mark_unread') {
      setManualUnreadIds((prev) => {
        const next = new Set(prev).add(id);
        saveStorageSet(STORAGE_KEYS.UNREAD, next);
        return next;
      });
    } else if (action === 'mark_read') {
      setManualUnreadIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        saveStorageSet(STORAGE_KEYS.UNREAD, next);
        return next;
      });
      if (conversation.unreadCount > 0) {
        messagingApi.markAsRead(id).catch(() => {});
      }
    } else if (action === 'delete') {
      const confirmed = window.confirm('Bạn có chắc chắn muốn xóa cuộc trò chuyện này khỏi danh sách?');
      if (confirmed) {
        setHiddenIds((prev) => {
          const next = new Set(prev).add(id);
          saveStorageSet(STORAGE_KEYS.HIDDEN, next);
          return next;
        });
        onDeleteConversation?.(id);
      }
    }
  };

  const handleSelect = (conversation: ConversationResponse) => {
    if (manualUnreadIds.has(conversation.conversationId)) {
      setManualUnreadIds((prev) => {
        const next = new Set(prev);
        next.delete(conversation.conversationId);
        saveStorageSet(STORAGE_KEYS.UNREAD, next);
        return next;
      });
    }
    onSelect(conversation);
  };

  // Filter out hidden conversations and apply search keyword
  const visible = conversations.filter((c) => !hiddenIds.has(c.conversationId));
  const filtered = visible.filter((c) => {
    if (!search.trim()) return true;
    const name = c.type === 'GROUP' ? c.name ?? '' : c.otherParticipant?.displayName ?? '';
    return name.toLowerCase().includes(search.trim().toLowerCase());
  });

  // Sort: pinned conversations first, then by lastMessageAt descending
  const sorted = [...filtered].sort((a, b) => {
    const aPin = pinnedIds.has(a.conversationId);
    const bPin = pinnedIds.has(b.conversationId);
    if (aPin && !bPin) return -1;
    if (!aPin && bPin) return 1;
    const timeA = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
    const timeB = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
    return timeB - timeA;
  });

  return (
    <div className="msg-sidebar">
      <div className="msg-sidebar__header">
        <h1 className="msg-sidebar__title">Tin nhắn</h1>
        <button type="button" className="msg-new-btn" onClick={onNewConversation}>
          + Mới
        </button>
      </div>

      <div className="msg-sidebar__search">
        <input
          type="text"
          className="msg-search-input"
          placeholder="Tìm cuộc trò chuyện..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      <div className="msg-conversation-list">
        {loading ? (
          <div className="msg-state msg-state--loading">Đang tải hội thoại...</div>
        ) : error ? (
          <div className="msg-state msg-state--error">{error}</div>
        ) : sorted.length === 0 ? (
          <div className="msg-state msg-state--empty">
            {search.trim() ? (
              'Không tìm thấy hội thoại phù hợp'
            ) : (
              <div>
                <p style={{ margin: '0 0 0.5rem', fontWeight: 600 }}>Bạn chưa có cuộc trò chuyện nào.</p>
                <p style={{ margin: 0, fontSize: '0.82rem', color: '#64748b' }}>
                  Nhấn nút "Nhắn tin" từ Marketplace, Tuyển dụng, Lịch dạy v.v. để bắt đầu chat.
                </p>
              </div>
            )}
          </div>
        ) : (
          sorted.map((conversation, index) => (
            <ConversationItem
              key={conversation.conversationId}
              conversation={conversation}
              active={conversation.conversationId === activeConversationId}
              isPinned={pinnedIds.has(conversation.conversationId)}
              isMuted={mutedIds.has(conversation.conversationId)}
              isManualUnread={manualUnreadIds.has(conversation.conversationId)}
              isMenuOpen={openMenuId === conversation.conversationId}
              menuPlacement={index > 1 && index >= sorted.length - 2 ? 'top' : 'bottom'}
              onToggleMenu={(e) => {
                e.stopPropagation();
                setOpenMenuId((prev) => (prev === conversation.conversationId ? null : conversation.conversationId));
              }}
              onAction={(action) => handleAction(conversation, action)}
              onClick={() => handleSelect(conversation)}
            />
          ))
        )}
      </div>
    </div>
  );
}
