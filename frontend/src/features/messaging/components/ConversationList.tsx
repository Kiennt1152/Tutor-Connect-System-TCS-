import { useState } from 'react';
import type { ConversationResponse } from '../types/messagingTypes';
import { ConversationItem } from './ConversationItem';

type ConversationListProps = {
  conversations: ConversationResponse[];
  activeConversationId: number | null;
  loading: boolean;
  error: string | null;
  onSelect: (conversation: ConversationResponse) => void;
  onNewConversation: () => void;
};

export function ConversationList({
  conversations,
  activeConversationId,
  loading,
  error,
  onSelect,
  onNewConversation,
}: ConversationListProps) {
  const [search, setSearch] = useState('');

  const filtered = conversations.filter((c) => {
    if (!search.trim()) return true;
    const name = c.otherParticipant?.displayName ?? '';
    return name.toLowerCase().includes(search.trim().toLowerCase());
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
        ) : filtered.length === 0 ? (
          <div className="msg-state msg-state--empty">
            {search.trim() ? 'Không tìm thấy hội thoại phù hợp' : 'Chưa có cuộc trò chuyện nào'}
          </div>
        ) : (
          filtered.map((conversation) => (
            <ConversationItem
              key={conversation.conversationId}
              conversation={conversation}
              active={conversation.conversationId === activeConversationId}
              onClick={() => onSelect(conversation)}
            />
          ))
        )}
      </div>
    </div>
  );
}
