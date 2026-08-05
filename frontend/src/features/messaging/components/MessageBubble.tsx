import type { MessageResponse } from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

type MessageBubbleProps = {
  message: MessageResponse;
  isMine: boolean;
  showAvatar: boolean;
};

function formatTime(value: string): string {
  return new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

export function MessageBubble({ message, isMine, showAvatar }: MessageBubbleProps) {
  return (
    <div className={`msg-bubble-row${isMine ? ' msg-bubble-row--mine' : ''}`}>
      {!isMine && (
        <div
          className="msg-avatar msg-avatar--sm"
          style={{ backgroundColor: getAvatarColor(message.senderId), visibility: showAvatar ? 'visible' : 'hidden' }}
        >
          <span>{getInitials(message.senderName)}</span>
        </div>
      )}

      <div className={`msg-bubble${isMine ? ' msg-bubble--mine' : ''}`}>
        <p className="msg-bubble__content">{message.content}</p>
        <span className="msg-bubble__time">
          {formatTime(message.sentAt)}
          {message.isEdited ? ' · đã sửa' : ''}
        </span>
      </div>
    </div>
  );
}
