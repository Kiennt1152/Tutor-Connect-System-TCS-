import type { ConversationResponse } from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

type ConversationItemProps = {
  conversation: ConversationResponse;
  active: boolean;
  onClick: () => void;
};

function formatTime(value: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  const now = new Date();
  const isSameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();
  if (isSameDay) {
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
  const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays >= 1 && diffDays < 7) {
    return `${diffDays} ngày trước`;
  }
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
}

function getContextLabel(type: string): string | null {
  const labels: Record<string, string> = {
    APPLICATION: '📝 Đơn ứng tuyển',
    RECRUITMENT: '💼 Tuyển dụng',
    RECRUITMENT_APPLICATION: '💼 Tuyển dụng',
    CLASS_REQUEST: '📋 Yêu cầu lớp',
    CLASS_ACTIVE: '📚 Lớp đang học',
  };
  return labels[type] || null;
}

export function ConversationItem({ conversation, active, onClick }: ConversationItemProps) {
  const other = conversation.otherParticipant;
  const isGroup = conversation.type === 'GROUP';
  const name = isGroup ? conversation.name ?? 'Nhóm chat' : other?.displayName ?? 'Người dùng';
  const hasUnread = conversation.unreadCount > 0;
  const contextLabel = getContextLabel(conversation.type);

  return (
    <button
      type="button"
      className={`msg-conversation-item${active ? ' msg-conversation-item--active' : ''}`}
      onClick={onClick}
    >
      <div
        className={`msg-avatar${isGroup ? ' msg-avatar--group' : ''}`}
        style={{ backgroundColor: getAvatarColor(other?.userId ?? conversation.conversationId) }}
      >
        {!isGroup && other?.avatarUrl ? (
          <img src={other.avatarUrl} alt={name} className="msg-avatar__img" />
        ) : (
          <span>{getInitials(name)}</span>
        )}
      </div>

      <div className="msg-conversation-item__body">
        <div className="msg-conversation-item__row">
          <span className="msg-conversation-item__name">{name}</span>
          <span className="msg-conversation-item__time">
            {formatTime(conversation.lastMessageAt)}
          </span>
        </div>
        <div className="msg-conversation-item__row">
          <span
            className={`msg-conversation-item__preview${hasUnread ? ' msg-conversation-item__preview--unread' : ''}`}
          >
            {conversation.lastMessagePreview ?? 'Bắt đầu trò chuyện'}
          </span>
          {hasUnread ? (
            <span className="msg-unread-badge">
              {conversation.unreadCount > 9 ? '9+' : conversation.unreadCount}
            </span>
          ) : null}
        </div>
        {contextLabel && (
          <div className="msg-conversation-item__context">
            <span>{contextLabel}</span>
          </div>
        )}
        {isGroup && (
          <div className="msg-conversation-item__context">
            <span>{conversation.participantCount} thành viên</span>
          </div>
        )}
      </div>
    </button>
  );
}
