import { useEffect, useLayoutEffect, useRef } from 'react';
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
  const prevScrollHeightRef = useRef<number>(0);
  const isPrependingRef = useRef<boolean>(false);
  const isInitialLoadRef = useRef<boolean>(true);

  useEffect(() => {
    if (messages.length === 0) {
      isInitialLoadRef.current = true;
      isPrependingRef.current = false;
    }
  }, [messages.length]);

  useLayoutEffect(() => {
    const el = containerRef.current;
    if (!el || messages.length === 0) return;

    if (isPrependingRef.current) {
      // Khi nạp tin nhắn cũ ở đầu: giữ nguyên vị trí màn hình đọc
      const diff = el.scrollHeight - prevScrollHeightRef.current;
      el.scrollTop = diff;
      isPrependingRef.current = false;
    } else if (isInitialLoadRef.current) {
      // Lần đầu mở hội thoại: cuộn tức thì xuống tin nhắn mới nhất
      el.scrollTop = el.scrollHeight;
      requestAnimationFrame(() => {
        isInitialLoadRef.current = false;
      });
    } else {
      // Tin nhắn mới được gửi đi hoặc nhận qua realtime
      bottomRef.current?.scrollIntoView({ block: 'end' });
    }
  }, [messages]);

  function handleScroll() {
    const el = containerRef.current;
    if (!el || !hasMore || loading || isInitialLoadRef.current) return;
    if (el.scrollTop < 80) {
      prevScrollHeightRef.current = el.scrollHeight;
      isPrependingRef.current = true;
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
