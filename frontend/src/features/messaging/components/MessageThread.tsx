import { useEffect, useRef } from 'react';
import type { MessageResponse } from '../types/messagingTypes';
import { MessageBubble } from './MessageBubble';

type MessageThreadProps = {
  messages: MessageResponse[];
  currentUserId: number | undefined;
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
};

export function MessageThread({
  messages,
  currentUserId,
  loading,
  hasMore,
  onLoadMore,
}: MessageThreadProps) {
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [messages.length]);

  function handleScroll() {
    const el = containerRef.current;
    if (!el || !hasMore || loading) return;
    if (el.scrollTop < 80) {
      onLoadMore();
    }
  }

  return (
    <div className="msg-thread" ref={containerRef} onScroll={handleScroll}>
      {loading && messages.length === 0 ? (
        <div className="msg-state msg-state--loading">Đang tải tin nhắn...</div>
      ) : messages.length === 0 ? (
        <div className="msg-state msg-state--empty">
          Chưa có tin nhắn nào. Hãy bắt đầu trò chuyện!
        </div>
      ) : (
        <>
          {loading && hasMore ? (
            <div className="msg-state msg-state--loading">Đang tải thêm...</div>
          ) : null}
          {messages.map((message, index) => {
            const previous = messages[index - 1];
            const showAvatar = !previous || previous.senderId !== message.senderId;
            return (
              <MessageBubble
                key={message.messageId}
                message={message}
                isMine={message.senderId === currentUserId}
                showAvatar={showAvatar}
              />
            );
          })}
          <div ref={bottomRef} />
        </>
      )}
    </div>
  );
}
