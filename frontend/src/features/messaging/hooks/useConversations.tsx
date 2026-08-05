import { useCallback, useEffect, useRef, useState } from 'react';
import { messagingApi } from '../api/messagingApi';
import { useStompClient } from './useStompClient';
import { useAuth } from '../../../shared/auth/AuthProvider';
import type { ConversationResponse, MessageResponse } from '../types/messagingTypes';

/**
 * Quan ly danh sach hoi thoai cua user hien tai. Lang nghe tin nhan moi tren tat ca
 * conversation dang co de cap nhat preview/unreadCount realtime.
 */
export function useConversations() {
  const { user } = useAuth();
  const { subscribe } = useStompClient();
  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const conversationsRef = useRef<ConversationResponse[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await messagingApi.getConversations();
      setConversations(data);
      conversationsRef.current = data;
    } catch {
      setError('Không thể tải danh sách hội thoại');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const unsubscribes = conversationsRef.current.map((conv) =>
      subscribe(`/topic/conversation/${conv.conversationId}`, (payload) => {
        const message = payload as MessageResponse;
        setConversations((prev) => {
          const next = prev.map((c) =>
            c.conversationId === message.conversationId
              ? {
                  ...c,
                  lastMessagePreview: message.content,
                  lastMessageAt: message.sentAt,
                  unreadCount:
                    message.senderId === user?.userId ? c.unreadCount : c.unreadCount + 1,
                }
              : c,
          );
          conversationsRef.current = next;
          return next;
        });
      }),
    );

    return () => unsubscribes.forEach((unsub) => unsub());
  }, [conversations.length, subscribe, user?.userId]);

  const upsertConversation = useCallback((conversation: ConversationResponse) => {
    setConversations((prev) => {
      const exists = prev.some((c) => c.conversationId === conversation.conversationId);
      const next = exists
        ? prev.map((c) => (c.conversationId === conversation.conversationId ? conversation : c))
        : [conversation, ...prev];
      conversationsRef.current = next;
      return next;
    });
  }, []);

  const markConversationRead = useCallback((conversationId: number) => {
    setConversations((prev) => {
      const next = prev.map((c) =>
        c.conversationId === conversationId ? { ...c, unreadCount: 0 } : c,
      );
      conversationsRef.current = next;
      return next;
    });
  }, []);

  return { conversations, loading, error, reload: load, upsertConversation, markConversationRead };
}
