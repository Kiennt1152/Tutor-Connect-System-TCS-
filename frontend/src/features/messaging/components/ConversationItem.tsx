import React from 'react';
import type { ConversationResponse } from '../types/messagingTypes';
import { getAvatarColor, getInitials } from '../utils/avatarUtils';

export type ConversationAction =
  | 'pin'
  | 'unpin'
  | 'mark_unread'
  | 'mark_read'
  | 'mute'
  | 'unmute'
  | 'delete';

type ConversationItemProps = {
  conversation: ConversationResponse;
  active: boolean;
  isPinned?: boolean;
  isMuted?: boolean;
  isManualUnread?: boolean;
  isMenuOpen?: boolean;
  menuPlacement?: 'top' | 'bottom';
  onToggleMenu?: (e: React.MouseEvent) => void;
  onAction?: (action: ConversationAction, e: React.MouseEvent) => void;
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
    APPLICATION: 'Đơn ứng tuyển',
    RECRUITMENT: 'Tuyển dụng',
    RECRUITMENT_APPLICATION: 'Tuyển dụng',
    CLASS_REQUEST: 'Yêu cầu lớp',
    CLASS_ACTIVE: 'Lớp đang học',
  };
  return labels[type] || null;
}

export function ConversationItem({
  conversation,
  active,
  isPinned = false,
  isMuted = false,
  isManualUnread = false,
  isMenuOpen = false,
  menuPlacement = 'bottom',
  onToggleMenu,
  onAction,
  onClick,
}: ConversationItemProps) {
  const other = conversation.otherParticipant;
  const isGroup = conversation.type === 'GROUP';
  const name = isGroup ? conversation.name ?? 'Nhóm chat' : other?.displayName ?? 'Người dùng';
  const hasEffectiveUnread = conversation.unreadCount > 0 || isManualUnread;
  const contextLabel = getContextLabel(conversation.type);

  return (
    <div
      role="button"
      tabIndex={0}
      className={`msg-conversation-item${active ? ' msg-conversation-item--active' : ''}${isPinned ? ' msg-conversation-item--pinned' : ''}${isMenuOpen ? ' msg-conversation-item--menu-open' : ''}`}
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
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
          <div className="msg-conversation-item__name-wrap">
            <span className="msg-conversation-item__name">{name}</span>
            {isMuted && (
              <span className="msg-status-icon msg-status-icon--muted" title="Đã tắt thông báo">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  <path d="M18.63 13A17.89 17.89 0 0 1 18 8" />
                  <path d="M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </span>
            )}
            {isPinned && (
              <span className="msg-status-icon msg-status-icon--pinned" title="Đã ghim">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z" />
                </svg>
              </span>
            )}
          </div>

          <div className="msg-conversation-item__meta-wrap">
            <span className="msg-conversation-item__time">
              {formatTime(conversation.lastMessageAt)}
            </span>
            <button
              type="button"
              className={`msg-item-more-btn${isMenuOpen ? ' msg-item-more-btn--active' : ''}`}
              title="Tùy chọn"
              aria-label="Tùy chọn"
              onClick={(e) => {
                e.stopPropagation();
                onToggleMenu?.(e);
              }}
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="2" />
                <circle cx="19" cy="12" r="2" />
                <circle cx="5" cy="12" r="2" />
              </svg>
            </button>
          </div>
        </div>

        <div className="msg-conversation-item__row">
          <span
            className={`msg-conversation-item__preview${hasEffectiveUnread ? ' msg-conversation-item__preview--unread' : ''}`}
          >
            {conversation.lastMessagePreview ?? 'Bắt đầu trò chuyện'}
          </span>
          {hasEffectiveUnread && (
            <span
              className={`msg-unread-badge${isManualUnread && conversation.unreadCount === 0 ? ' msg-unread-badge--dot' : ''}`}
            >
              {conversation.unreadCount > 0
                ? conversation.unreadCount > 9
                  ? '9+'
                  : conversation.unreadCount
                : ''}
            </span>
          )}
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

      {isMenuOpen && (
        <div
          className={`msg-item-menu${menuPlacement === 'top' ? ' msg-item-menu--top' : ''}`}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            type="button"
            className="msg-item-menu__item"
            onClick={(e) => {
              e.stopPropagation();
              onAction?.(isPinned ? 'unpin' : 'pin', e);
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z" />
            </svg>
            <span>{isPinned ? 'Bỏ ghim hội thoại' : 'Ghim hội thoại'}</span>
          </button>

          <button
            type="button"
            className="msg-item-menu__item"
            onClick={(e) => {
              e.stopPropagation();
              onAction?.(hasEffectiveUnread ? 'mark_read' : 'mark_unread', e);
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="4" />
            </svg>
            <span>{hasEffectiveUnread ? 'Đánh dấu đã đọc' : 'Đánh dấu chưa đọc'}</span>
          </button>

          <button
            type="button"
            className="msg-item-menu__item"
            onClick={(e) => {
              e.stopPropagation();
              onAction?.(isMuted ? 'unmute' : 'mute', e);
            }}
          >
            {isMuted ? (
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            ) : (
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                <path d="M18.63 13A17.89 17.89 0 0 1 18 8" />
                <path d="M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            )}
            <span>{isMuted ? 'Bật thông báo' : 'Tắt thông báo'}</span>
          </button>

          <div className="msg-item-menu__divider" />

          <button
            type="button"
            className="msg-item-menu__item msg-item-menu__item--danger"
            onClick={(e) => {
              e.stopPropagation();
              onAction?.('delete', e);
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
            <span>Xóa cuộc trò chuyện</span>
          </button>
        </div>
      )}
    </div>
  );
}
