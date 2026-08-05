import { useCallback, useEffect, useRef, useState } from 'react';
import { messagingApi } from '../api/messagingApi';
import { useStompClient } from './useStompClient';
import type { MessageResponse } from '../types/messagingTypes';

/**
 * Tai lich su tin nhan cua mot conversation va lang nghe tin nhan moi qua STOMP.
 */
export function useMessages(conversationId: number | null) {
  const { subscribe, publish, connected } = useStompClient();
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const pageRef = useRef(0);

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      return;
    }

    let cancelled = false;
    pageRef.current = 0;
    setLoading(true);
    setError(null);

    messagingApi
      .getMessages(conversationId, 0, 30)
      .then((res) => {
        if (cancelled) return;
        setMessages([...res.content].reverse());
        setHasMore(!res.last);
      })
      .catch(() => {
        if (cancelled) return;
        setError('Không thể tải tin nhắn');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [conversationId]);

  useEffect(() => {
    if (!conversationId) return;
    const unsubscribe = subscribe(`/topic/conversation/${conversationId}`, (payload) => {
      const message = payload as MessageResponse;
      setMessages((prev) => {
        if (prev.some((m) => m.messageId === message.messageId)) return prev;
        return [...prev, message];
      });
    });
    return unsubscribe;
  }, [conversationId, subscribe]);

  const loadMore = useCallback(async () => {
    if (!conversationId || loading || !hasMore) return;
    const nextPage = pageRef.current + 1;
    setLoading(true);
    try {
      const res = await messagingApi.getMessages(conversationId, nextPage, 30);
      setMessages((prev) => [...[...res.content].reverse(), ...prev]);
      setHasMore(!res.last);
      pageRef.current = nextPage;
    } catch {
      setError('Không thể tải thêm tin nhắn');
    } finally {
      setLoading(false);
    }
  }, [conversationId, hasMore, loading]);

  const sendMessage = useCallback(
    async (content: string) => {
      if (!conversationId || !content.trim()) return;
      if (connected) {
        publish('/app/chat.send', { conversationId, content: content.trim() });
      } else {
        const message = await messagingApi.sendMessage(conversationId, content.trim());
        setMessages((prev) =>
          prev.some((m) => m.messageId === message.messageId) ? prev : [...prev, message],
        );
      }
    },
    [conversationId, connected, publish],
  );

  return { messages, loading, error, hasMore, loadMore, sendMessage };
}
